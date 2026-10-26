
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/vehicles")
public class VehiclesServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession s = request.getSession(false);
        if (s == null || s.getAttribute("role") == null) {
            response.sendRedirect("login.html");
            return;
        }

        boolean officer = "Officer".equals(s.getAttribute("role"));
        String email = (String) s.getAttribute("email");
        response.setContentType("text/html;charset=UTF-8");

        StringBuilder out = new StringBuilder(
            HtmlUtil.pageStart(officer ? "All Vehicles" : "My Vehicles",
            (String) s.getAttribute("userName"),
            (String) s.getAttribute("role"))
        );

        out.append(HtmlUtil.nav((String) s.getAttribute("role")));
        out.append("<section class='panel'><h1>")
           .append(officer ? "Registered Vehicles" : "My Vehicles")
           .append("</h1>");
        out.append(HtmlUtil.message(request.getParameter("message")));

        if (!officer) {
            out.append("<h2>Register a vehicle</h2>")
               .append("<form class='form-grid' method='post' action='vehicles'>")
               .append("<label>Vehicle number<input name='vehicleNumber' required maxlength='20' placeholder='TN01AB1234'></label>")
               .append("<label>Owner name<input name='ownerName' required maxlength='100' value='")
               .append(HtmlUtil.e((String) s.getAttribute("userName")))
               .append("'></label>")
               .append("<label>Vehicle type<select name='vehicleType' required>")
               .append("<option>Car</option><option>Motorcycle</option><option>Bus</option>")
               .append("<option>Truck</option><option>Auto</option><option>Other</option>")
               .append("</select></label>")
               .append("<button type='submit'>Register Vehicle</button></form><hr>");
        }

        out.append("<div class='table-wrap'><table><thead><tr>")
           .append("<th>Vehicle Number</th><th>Owner</th><th>Type</th>")
           .append("</tr></thead><tbody>");

        String sql;
        if (officer) {
            sql = "SELECT vehicle_number, owner_name, vehicle_type FROM vehicles ORDER BY vehicle_id DESC";
        } else {
            sql = "SELECT vehicle_number, owner_name, vehicle_type FROM vehicles WHERE owner_email = ? ORDER BY vehicle_id DESC";
        }

        try (Connection con = DBConnection.getConnection();
             PreparedStatement p = con.prepareStatement(sql)) {

            if (!officer) {
                p.setString(1, email);
            }

            try (ResultSet r = p.executeQuery()) {
                boolean any = false;

                while (r.next()) {
                    any = true;
                    out.append("<tr><td>")
                       .append(HtmlUtil.e(r.getString("vehicle_number")))
                       .append("</td><td>")
                       .append(HtmlUtil.e(r.getString("owner_name")))
                       .append("</td><td>")
                       .append(HtmlUtil.e(r.getString("vehicle_type")))
                       .append("</td></tr>");
                }

                if (!any) {
                    out.append("<tr><td colspan='3'>No vehicles found.</td></tr>");
                }
            }

        } catch (Exception e) {
            log("Vehicle list error", e);
            out.append("<tr><td colspan='3'>Could not load vehicles. Check database configuration.</td></tr>");
        }

        out.append("</tbody></table></div></section>")
           .append(HtmlUtil.pageEnd());

        response.getWriter().println(out);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession s = request.getSession(false);

        if (s == null || !"Citizen".equals(s.getAttribute("role"))) {
            response.sendRedirect("login.html");
            return;
        }

        String number = request.getParameter("vehicleNumber");
        String owner = request.getParameter("ownerName");
        String type = request.getParameter("vehicleType");
        String email = (String) s.getAttribute("email");

        if (number == null || owner == null || type == null ||
            email == null || number.isBlank() || owner.isBlank() ||
            type.isBlank() || email.isBlank()) {
            response.sendRedirect("vehicles?message=Please+complete+all+fields");
            return;
        }

        String sql = "INSERT INTO vehicles (vehicle_number, owner_name, vehicle_type, owner_email) VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement p = con.prepareStatement(sql)) {

            p.setString(1, number.trim().toUpperCase());
            p.setString(2, owner.trim());
            p.setString(3, type.trim());
            p.setString(4, email.trim());

            p.executeUpdate();

            response.sendRedirect("vehicles?message=Vehicle+registered+successfully");

        } catch (Exception e) {
            log("Vehicle registration error", e);
            response.sendRedirect("vehicles?message=Registration+failed.+Check+server+logs");
        }
    }
}
