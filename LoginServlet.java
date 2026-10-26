import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String chosenRole = request.getParameter("role");

        if (email == null || password == null || chosenRole == null) {
            response.sendRedirect("login.html?error=missing");
            return;
        }

        String sql = "SELECT user_id, name, password, role FROM users WHERE email = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && password.equals(rs.getString("password"))
                        && chosenRole.equalsIgnoreCase(rs.getString("role"))) {
                    HttpSession old = request.getSession(false);
                    if (old != null) old.invalidate();
                    HttpSession session = request.getSession(true);
                    session.setAttribute("userId", rs.getInt("user_id"));
                    session.setAttribute("userName", rs.getString("name"));
                    session.setAttribute("email", email.trim());
                    session.setAttribute("role", rs.getString("role"));
                    session.setMaxInactiveInterval(30 * 60);

                    if ("Officer".equalsIgnoreCase(rs.getString("role"))) {
                        response.sendRedirect("officer-dashboard");
                    } else {
                        response.sendRedirect("citizen-dashboard");
                    }
                } else {
                    response.sendRedirect("login.html?error=invalid");
                }
            }
        } catch (Exception e) {
            log("Login error", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Login failed because of a server/database configuration problem. Check the Tomcat log.");
        }
    }
}
