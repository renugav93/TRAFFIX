import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/citizen-dashboard")
public class CitizenDashboardServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession s = request.getSession(false);
        if (s == null || !"Citizen".equals(s.getAttribute("role"))) {
            response.sendRedirect("login.html"); return;
        }
        int id = (Integer)s.getAttribute("userId");
        int vehicles = 0, pending = 0, reports = 0;
        try (Connection con = DBConnection.getConnection()) {
            try (PreparedStatement p = con.prepareStatement("SELECT COUNT(*) FROM vehicles WHERE owner_email=?")) {
                p.setString(1, (String)s.getAttribute("email"));
                try (ResultSet r = p.executeQuery()) { if (r.next()) vehicles = r.getInt(1); }
            }
            try (PreparedStatement p = con.prepareStatement("SELECT COUNT(*) FROM challans c JOIN vehicles v ON c.vehicle_number=v.vehicle_number WHERE v.owner_email=? AND c.payment_status='Pending'")) {
                p.setString(1, (String)s.getAttribute("email"));
                try (ResultSet r = p.executeQuery()) { if (r.next()) pending = r.getInt(1); }
            }
            try (PreparedStatement p = con.prepareStatement("SELECT COUNT(*) FROM traffic_reports WHERE citizen_id=? AND status <> 'Resolved'")) {
                p.setInt(1, id);
                try (ResultSet r = p.executeQuery()) { if (r.next()) reports = r.getInt(1); }
            }
        } catch (Exception e) { log("Citizen dashboard error", e); }
        response.setContentType("text/html;charset=UTF-8");
        String name = (String)s.getAttribute("userName");
        response.getWriter().println(HtmlUtil.pageStart("Citizen Dashboard", name, "Citizen") +
            HtmlUtil.nav("Citizen") + "<section class='hero'><p class='eyebrow'>CITIZEN PORTAL</p><h1>Hello, " + HtmlUtil.e(name) +
            "</h1><p>Keep track of your vehicles, traffic fines and reports in one place.</p></section>" +
            "<section class='cards'><article class='stat'><span>My Vehicles</span><strong>" + vehicles +
            "</strong><a href='vehicles'>View vehicles →</a></article><article class='stat'><span>Pending Fines</span><strong>" + pending +
            "</strong><a href='challans'>View challans →</a></article><article class='stat'><span>Open Reports</span><strong>" + reports +
            "</strong><a href='reports'>Track reports →</a></article></section>" +
            "<section class='panel'><h2>Quick actions</h2><div class='quicklinks'><a href='vehicles'>Register a vehicle</a><a href='challans'>View and pay demo fines</a><a href='reports'>Submit a traffic report</a></div></section>" +
            HtmlUtil.pageEnd());
    }
}
