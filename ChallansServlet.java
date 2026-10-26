
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Date;
import java.time.LocalDate;
import java.math.BigDecimal;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/challans")
public class ChallansServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession s = request.getSession(false);
        if (s == null || s.getAttribute("role") == null) {
            response.sendRedirect("login.html");
            return;
        }

        boolean officer = "Officer".equals(s.getAttribute("role"));
        response.setContentType("text/html;charset=UTF-8");

        StringBuilder out = new StringBuilder(
            HtmlUtil.pageStart("Challans", (String) s.getAttribute("userName"),
            (String) s.getAttribute("role"))
        );

        out.append(HtmlUtil.nav((String) s.getAttribute("role")))
           .append("<section class='panel'><h1>")
           .append(officer ? "Manage Challans" : "My Challans")
           .append("</h1>")
           .append(HtmlUtil.message(request.getParameter("message")));

        if (officer) {
            out.append("<h2>Issue a challan</h2>")
               .append("<form class='form-grid' method='post' action='challans'>")
               .append("<label>Registered vehicle<select name='vehicleNumber' required>");

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement p = con.prepareStatement(
                     "SELECT vehicle_number FROM vehicles ORDER BY vehicle_number");
                 ResultSet r = p.executeQuery()) {

                while (r.next()) {
                    out.append("<option value='")
                       .append(HtmlUtil.e(r.getString("vehicle_number")))
                       .append("'>")
                       .append(HtmlUtil.e(r.getString("vehicle_number")))
                       .append("</option>");
                }
            } catch (Exception e) {
                log("Vehicle choices error", e);
            }

            out.append("</select></label>")
               .append("<label>Violation<select name='violation' required>")
               .append("<option>Speed limit violation</option>")
               .append("<option>Signal jumping</option>")
               .append("<option>No helmet</option>")
               .append("<option>No seat belt</option>")
               .append("<option>Illegal parking</option>")
               .append("<option>Using mobile while driving</option>")
               .append("<option>Other traffic violation</option>")
               .append("</select></label>")
               .append("<label>Fine amount (₹)<input type='number' name='fineAmount' min='1' step='0.01' required></label>")
               .append("<button type='submit'>Issue Challan</button></form><hr>");
        }

        out.append("<div class='table-wrap'><table><thead><tr>")
           .append("<th>ID</th><th>Vehicle</th><th>Violation</th><th>Fine (₹)</th><th>Date</th><th>Status</th>");

        if (!officer) out.append("<th>Action</th>");
        out.append("</tr></thead><tbody>");

        String sql =
            "SELECT c.challan_id,c.vehicle_number,c.violation,c.fine_amount,c.challan_date,c.payment_status " +
            "FROM challans c " +
            (officer ? "" :
            "JOIN vehicles v ON c.vehicle_number=v.vehicle_number WHERE v.owner_email=? ") +
            "ORDER BY c.challan_date DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement p = con.prepareStatement(sql)) {

            if (!officer) p.setString(1, (String) s.getAttribute("email"));

            try (ResultSet r = p.executeQuery()) {
                boolean any = false;

                while (r.next()) {
                    any = true;
                    int id = r.getInt("challan_id");
                    String status = r.getString("payment_status");

                    out.append("<tr><td>").append(id)
                       .append("</td><td>").append(HtmlUtil.e(r.getString("vehicle_number")))
                       .append("</td><td>").append(HtmlUtil.e(r.getString("violation")))
                       .append("</td><td>").append(r.getBigDecimal("fine_amount").toPlainString())
                       .append("</td><td>").append(r.getDate("challan_date"))
                       .append("</td><td><span class='status ")
                       .append("Paid".equals(status) ? "paid" : "pending")
                       .append("'>").append(HtmlUtil.e(status)).append("</span></td>");

                    if (!officer) {
                        out.append("<td>");
                        if ("Pending".equals(status)) {
                            out.append("<form method='post' action='payment'>")
                               .append("<input type='hidden' name='challanId' value='")
                               .append(id)
                               .append("'><button class='small-btn' type='submit'>Pay Demo</button></form>");
                        } else {
                            out.append("—");
                        }
                        out.append("</td>");
                    }

                    out.append("</tr>");
                }

                if (!any) {
                    out.append("<tr><td colspan='7'>No challans found.</td></tr>");
                }
            }
        } catch (Exception e) {
            log("Challan list error", e);
            out.append("<tr><td colspan='7'>Could not load challans. Check database configuration.</td></tr>");
        }

        out.append("</tbody></table></div></section>").append(HtmlUtil.pageEnd());
        response.getWriter().println(out);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession s = request.getSession(false);

        if (s == null || !"Officer".equals(s.getAttribute("role"))) {
            response.sendRedirect("login.html");
            return;
        }

        try {
            String vehicle = request.getParameter("vehicleNumber");
            String violation = request.getParameter("violation");
            String amount = request.getParameter("fineAmount");

            if (vehicle == null || vehicle.isBlank() ||
                violation == null || violation.isBlank() ||
                amount == null || amount.isBlank()) {
                throw new IllegalArgumentException("Missing required fields");
            }

            BigDecimal fine = new BigDecimal(amount);
            if (fine.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Fine must be positive");
            }

            String sql =
                "INSERT INTO challans (vehicle_number, violation, fine_amount, challan_date, payment_status) " +
                "VALUES (?, ?, ?, ?, 'Pending')";

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement p = con.prepareStatement(sql)) {

                p.setString(1, vehicle.trim().toUpperCase());
                p.setString(2, violation.trim());
                p.setBigDecimal(3, fine);
                p.setDate(4, Date.valueOf(LocalDate.now()));

                p.executeUpdate();
            }

            response.sendRedirect("challans?message=Challan+issued+successfully");

        } catch (Exception e) {
            log("Issue challan error", e);
            response.sendRedirect("challans?message=Could+not+issue+challan.+Check+server+logs");
        }
    }
}
