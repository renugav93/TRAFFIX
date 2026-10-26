import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/reports")
public class ReportsServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession s = request.getSession(false);
        if (s == null || s.getAttribute("role") == null) { response.sendRedirect("login.html"); return; }
        boolean officer = "Officer".equals(s.getAttribute("role"));
        response.setContentType("text/html;charset=UTF-8");
        StringBuilder out = new StringBuilder(HtmlUtil.pageStart("Traffic Reports", (String)s.getAttribute("userName"), (String)s.getAttribute("role")));
        out.append(HtmlUtil.nav((String)s.getAttribute("role"))).append("<section class='panel'><h1>")
           .append(officer ? "Citizen Traffic Reports" : "Report a Traffic Issue").append("</h1>")
           .append(HtmlUtil.message(request.getParameter("message")));
        if (!officer) {
            out.append("<form class='form-grid' method='post' action='reports'>")
               .append("<label>Location<input name='location' required maxlength='200' placeholder='Street / area'></label>")
               .append("<label>Issue type<select name='reportType' required><option>Signal issue</option><option>Traffic congestion</option><option>Road damage</option><option>Illegal parking</option><option>Accident / hazard</option><option>Other</option></select></label>")
               .append("<label class='wide'>Description<textarea name='description' required maxlength='1000' rows='4' placeholder='Describe the issue clearly'></textarea></label>")
               .append("<button type='submit'>Submit Report</button></form><hr>");
        }
        out.append("<div class='table-wrap'><table><thead><tr><th>ID</th><th>Location</th><th>Issue</th><th>Description</th><th>Date</th><th>Status</th>");
        if (officer) out.append("<th>Update</th>");
        out.append("</tr></thead><tbody>");
        String sql = officer
            ? "SELECT r.report_id,r.location,r.report_type,r.description,r.report_date,r.status,u.name FROM traffic_reports r JOIN users u ON r.citizen_id=u.user_id ORDER BY r.report_date DESC"
            : "SELECT r.report_id,r.location,r.report_type,r.description,r.report_date,r.status FROM traffic_reports r WHERE r.citizen_id=? ORDER BY r.report_date DESC";
        try (Connection con = DBConnection.getConnection(); PreparedStatement p = con.prepareStatement(sql)) {
            if (!officer) p.setInt(1, (Integer)s.getAttribute("userId"));
            try (ResultSet r = p.executeQuery()) {
                boolean any = false;
                while (r.next()) {
                    any = true;
                    out.append("<tr><td>").append(r.getInt("report_id")).append("</td><td>").append(HtmlUtil.e(r.getString("location")))
                       .append("</td><td>").append(HtmlUtil.e(r.getString("report_type"))).append("</td><td>")
                       .append(HtmlUtil.e(r.getString("description"))).append("</td><td>").append(r.getTimestamp("report_date"))
                       .append("</td><td>").append(HtmlUtil.e(r.getString("status"))).append("</td>");
                    if (officer) {
                        out.append("<td><form class='inline-form' method='post' action='reports'><input type='hidden' name='reportId' value='")
                           .append(r.getInt("report_id")).append("'><select name='status'><option")
                           .append("Submitted".equals(r.getString("status")) ? " selected" : "").append(">Submitted</option><option")
                           .append("Under Review".equals(r.getString("status")) ? " selected" : "").append(">Under Review</option><option")
                           .append("Resolved".equals(r.getString("status")) ? " selected" : "").append(">Resolved</option></select><button class='small-btn' type='submit'>Update</button></form></td>");
                    }
                    out.append("</tr>");
                }
                if (!any) out.append("<tr><td colspan='7'>No reports found.</td></tr>");
            }
        } catch (Exception e) { log("Reports list error", e); out.append("<tr><td colspan='7'>Could not load reports. Check database configuration.</td></tr>"); }
        out.append("</tbody></table></div></section>").append(HtmlUtil.pageEnd());
        response.getWriter().println(out);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession s = request.getSession(false);
        if (s == null || s.getAttribute("role") == null) { response.sendRedirect("login.html"); return; }
        if ("Officer".equals(s.getAttribute("role"))) {
            try {
                int reportId = Integer.parseInt(request.getParameter("reportId"));
                String status = request.getParameter("status");
                if (!"Submitted".equals(status) && !"Under Review".equals(status) && !"Resolved".equals(status)) throw new IllegalArgumentException();
                try (Connection con = DBConnection.getConnection(); PreparedStatement p = con.prepareStatement("UPDATE traffic_reports SET status=? WHERE report_id=?")) {
                    p.setString(1, status); p.setInt(2, reportId); p.executeUpdate();
                }
                response.sendRedirect("reports?message=Report+status+updated");
            } catch (Exception e) {
                log("Update report error", e);
                response.sendRedirect("reports?message=Could+not+update+report");
            }
        } else {
            String location = request.getParameter("location");
            String type = request.getParameter("reportType");
            String description = request.getParameter("description");
            if (location == null || type == null || description == null || location.isBlank() || description.isBlank()) {
                response.sendRedirect("reports?message=Please+complete+all+fields"); return;
            }
            try (Connection con = DBConnection.getConnection(); PreparedStatement p = con.prepareStatement(
                    "INSERT INTO traffic_reports(citizen_id,location,report_type,description) VALUES(?,?,?,?)")) {
                p.setInt(1, (Integer)s.getAttribute("userId"));
                p.setString(2, location.trim()); p.setString(3, type); p.setString(4, description.trim());
                p.executeUpdate();
                response.sendRedirect("reports?message=Traffic+report+submitted");
            } catch (Exception e) {
                log("Submit report error", e);
                response.sendRedirect("reports?message=Could+not+submit+report");
            }
        }
    }
}
