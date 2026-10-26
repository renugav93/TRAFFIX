
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/density")
public class DensityServlet extends HttpServlet {

    private boolean checkOfficer(HttpServletRequest request,
                                 HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);

        if (session == null ||
            !"Officer".equalsIgnoreCase(
                String.valueOf(session.getAttribute("role")))) {
            response.sendError(403, "Officer access required. Please log in as Officer.");
            return false;
        }
        return true;
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException {

        if (!checkOfficer(request, response)) return;

        response.setContentType("text/html;charset=UTF-8");

        try (java.io.PrintWriter out = response.getWriter();
             Connection con = DBConnection.getConnection()) {

            out.println("<!DOCTYPE html><html><head><title>Set Traffic Density</title>");
            out.println("<style>");
            out.println("body{font-family:Arial;background:#f1f5f9;padding:30px;color:#1e293b}");
            out.println("h2{color:#0f172a}table{border-collapse:collapse;background:white;width:100%;max-width:850px}");
            out.println("th,td{padding:14px;border:1px solid #cbd5e1;text-align:left}");
            out.println("th{background:#1e293b;color:white}");
            out.println("select,button{padding:8px;border-radius:5px}");
            out.println("button{background:#2563eb;color:white;border:0;cursor:pointer}");
            out.println("a{color:#2563eb}");
            out.println("</style></head><body>");

            out.println("<h2>Set Traffic Density</h2>");
            out.println("<p>Update the traffic density for each junction.</p>");
            out.println("<table><tr><th>Junction</th><th>Current Density</th><th>Update</th></tr>");

            String sql = "SELECT junction_name, traffic_density FROM traffic_signals ORDER BY junction_name";

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    String junction = rs.getString("junction_name");
                    String density = rs.getString("traffic_density");

                    out.println("<tr><td>" + junction + "</td><td>" + density + "</td><td>");
                    out.println("<form method='post' action='density'>");
                    out.println("<input type='hidden' name='junction' value='" + junction + "'>");
                    out.println("<select name='density'>");

                    String[] choices = {"LOW", "MEDIUM", "HIGH"};

                    for (String choice : choices) {
                        out.println("<option value='" + choice + "'" +
                            (choice.equalsIgnoreCase(density) ? " selected" : "") +
                            ">" + choice + "</option>");
                    }

                    out.println("</select> ");
                    out.println("<button type='submit'>Save</button>");
                    out.println("</form></td></tr>");
                }
            }

            out.println("</table><p><a href='officer-dashboard'>Back to Officer Dashboard</a></p>");
            out.println("</body></html>");

        } catch (Exception e) {
            log("Traffic density loading error", e);
            response.sendError(500, "Unable to load traffic density. Check the Tomcat log.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws IOException {

        if (!checkOfficer(request, response)) return;

        String junction = request.getParameter("junction");
        String density = request.getParameter("density");

        if (junction == null ||
            !("LOW".equals(density) ||
              "MEDIUM".equals(density) ||
              "HIGH".equals(density))) {
            response.sendError(400, "Invalid junction or density.");
            return;
        }

        String sql = "UPDATE traffic_signals SET traffic_density=? WHERE junction_name=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, density);
            ps.setString(2, junction);

            if (ps.executeUpdate() == 0) {
                response.sendError(404, "Junction not found.");
                return;
            }

            response.sendRedirect("density?updated=1");

        } catch (Exception e) {
            log("Traffic density update error", e);
            response.sendError(500, "Unable to update traffic density. Check the Tomcat log.");
        }
    }
}
