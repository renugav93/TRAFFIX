import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/payment")
public class PaymentServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession s = request.getSession(false);
        if (s == null || !"Citizen".equals(s.getAttribute("role"))) { response.sendRedirect("login.html"); return; }
        try {
            int challanId = Integer.parseInt(request.getParameter("challanId"));
            String sql = "UPDATE challans c JOIN vehicles v ON c.vehicle_number=v.vehicle_number SET c.payment_status='Paid' WHERE c.challan_id=? AND v.owner_email=? AND c.payment_status='Pending'";
            try (Connection con = DBConnection.getConnection(); PreparedStatement p = con.prepareStatement(sql)) {
                p.setInt(1, challanId);
                p.setString(2, (String)s.getAttribute("email"));
                int changed = p.executeUpdate();
                response.sendRedirect("challans?message=" + (changed > 0 ? "Demo+payment+recorded" : "Payment+could+not+be+updated"));
            }
        } catch (Exception e) {
            log("Demo payment error", e);
            response.sendRedirect("challans?message=Payment+could+not+be+updated");
        }
    }
}
