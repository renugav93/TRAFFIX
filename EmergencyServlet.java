
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/emergency")
public class EmergencyServlet extends HttpServlet {

    private boolean isOfficer(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null &&
            "Officer".equalsIgnoreCase(
                String.valueOf(session.getAttribute("role")));
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);
        String role = session == null ? "" :
            String.valueOf(session.getAttribute("role"));

        if (!"Officer".equalsIgnoreCase(role) &&
            !"Citizen".equalsIgnoreCase(role)) {
            response.sendError(403, "Please log in first.");
            return;
        }

        response.setContentType("text/html;charset=UTF-8");
        var out = response.getWriter();

        out.println("<!DOCTYPE html><html><head><title>Emergency Priority</title>");
        out.println("<style>");
        out.println("body{font-family:Arial;margin:35px;background:#f1f5f9;color:#0f172a}");
        out.println("h1{color:#b91c1c}form{background:white;padding:25px;max-width:500px;border-radius:10px}");
        out.println("label{display:block;margin-top:15px}input,select{padding:10px;width:100%;box-sizing:border-box;margin-top:5px}");
        out.println("button{padding:12px 18px;background:#b91c1c;color:white;border:0;margin-top:20px;cursor:pointer}");
        out.println("table{border-collapse:collapse;width:100%;background:white;margin-top:25px}");
        out.println("th,td{border:1px solid #cbd5e1;padding:10px;text-align:left}th{background:#b91c1c;color:white}");
        out.println("</style></head><body>");
        out.println("<h1>Emergency Priority</h1>");
        out.println("<p>Submit an emergency priority request. This is a software simulation and does not control real traffic lights.</p>");

        out.println("<form method='post' action='emergency'>");
        out.println("<label>Vehicle Type</label><select name='vehicleType' required>");
        out.println("<option>Ambulance</option><option>Fire Engine</option><option>Police Vehicle</option></select>");
        out.println("<label>Vehicle Number</label><input name='vehicleNumber' maxlength='20' required>");
        out.println("<label>Junction</label><select name='junction' required>");
        out.println("<option>Anna Nagar</option><option>Guindy</option><option>T Nagar</option><option>Adyar</option></select>");
        out.println("<button type='submit'>Request Emergency Priority</button></form>");

        if (isOfficer(request)) {
            out.println("<h2>Emergency Requests</h2>");
            out.println("<table><tr><th>ID</th><th>Type</th><th>Vehicle Number</th><th>Junction</th><th>Status</th><th>Time</th></tr>");

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(
                     "SELECT request_id, vehicle_type, vehicle_number, " +
                     "junction_name, request_status, request_time " +
                     "FROM emergency_requests ORDER BY request_time DESC");
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    out.println("<tr><td>" + rs.getInt("request_id") +
                        "</td><td>" + rs.getString("vehicle_type") +
                        "</td><td>" + rs.getString("vehicle_number") +
                        "</td><td>" + rs.getString("junction_name") +
                        "</td><td>" + rs.getString("request_status") +
                        "</td><td>" + rs.getTimestamp("request_time") +
                        "</td></tr>");
                }

            } catch (Exception e) {
                getServletContext().log("Unable to load emergency requests", e);
                out.println("</table><p>Unable to load emergency requests. Check the Tomcat log.</p>");
            }

            out.println("</table>");
        }

        out.println("<p><a href='officer-dashboard'>Back to Officer Dashboard</a></p>");
        out.println("</body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);
        String role = session == null ? "" :
            String.valueOf(session.getAttribute("role"));

        if (!"Officer".equalsIgnoreCase(role) &&
            !"Citizen".equalsIgnoreCase(role)) {
            response.sendError(403, "Please log in first.");
            return;
        }

        String type = request.getParameter("vehicleType");
        String number = request.getParameter("vehicleNumber");
        String junction = request.getParameter("junction");

        if (type == null || number == null || junction == null ||
            !java.util.Arrays.asList(
                "Ambulance", "Fire Engine", "Police Vehicle").contains(type) ||
            !java.util.Arrays.asList(
                "Anna Nagar", "Guindy", "T Nagar", "Adyar").contains(junction) ||
            !number.trim().matches("[A-Za-z0-9 -]{1,20}")) {
            response.sendError(400, "Please enter valid emergency details.");
            return;
        }

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "INSERT INTO emergency_requests " +
                 "(vehicle_type, vehicle_number, junction_name) " +
                 "VALUES (?, ?, ?)")) {

            ps.setString(1, type);
            ps.setString(2, number.trim().toUpperCase());
            ps.setString(3, junction);
            ps.executeUpdate();

            response.sendRedirect("emergency?submitted=1");

        } catch (Exception e) {
            getServletContext().log("Unable to save emergency request", e);
            response.sendError(500, "Unable to save request. Check the Tomcat log.");
        }
    }
}
