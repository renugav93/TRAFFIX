
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/officer-dashboard")
public class OfficerDashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {

        HttpSession s = request.getSession(false);

        if (s == null ||
            !"Officer".equalsIgnoreCase(
                String.valueOf(s.getAttribute("role")))) {
            response.sendRedirect("login.html");
            return;
        }

        int vehicles = 0, pending = 0, reports = 0;

        StringBuilder junctionCards = new StringBuilder();

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement()) {

            try (ResultSet r = st.executeQuery(
                    "SELECT COUNT(*) FROM vehicles")) {
                if (r.next()) vehicles = r.getInt(1);
            }

            try (ResultSet r = st.executeQuery(
                    "SELECT COUNT(*) FROM challans " +
                    "WHERE payment_status='Pending'")) {
                if (r.next()) pending = r.getInt(1);
            }

            try (ResultSet r = st.executeQuery(
                    "SELECT COUNT(*) FROM traffic_reports " +
                    "WHERE status <> 'Resolved'")) {
                if (r.next()) reports = r.getInt(1);
            }

            try (ResultSet r = st.executeQuery(
                    "SELECT junction_name, signal_status, traffic_density, " +
                    "TIMESTAMPDIFF(SECOND, cycle_started_at, CURRENT_TIMESTAMP) AS elapsed_seconds, " +
                    "UNIX_TIMESTAMP(cycle_started_at) AS cycle_started_epoch " +
                    "FROM traffic_signals ORDER BY signal_id")) {

                while (r.next()) {
                    String junction = r.getString("junction_name");
                    String density = r.getString("traffic_density");
                    int elapsed = r.getInt("elapsed_seconds");
                    long cycleStartedEpoch = r.getLong("cycle_started_epoch");
                    if (r.wasNull() || elapsed < 0) {
                        elapsed = 0;
                    }

                    int green, yellow = 5, red;

                    if ("HIGH".equalsIgnoreCase(density)) {
                        green = 60;
                        red = 30;
                    } else if ("MEDIUM".equalsIgnoreCase(density)) {
                        green = 40;
                        red = 45;
                    } else {
                        green = 30;
                        red = 60;
                    }

                    int cycle = green + yellow + red;
                    int position = elapsed % cycle;
                    String signal;
                    int remaining;

                    if (position < green) {
                        signal = "GREEN";
                        remaining = green - position;
                    } else if (position < green + yellow) {
                        signal = "YELLOW";
                        remaining = green + yellow - position;
                    } else {
                        signal = "RED";
                        remaining = cycle - position;
                    }
                    String signalColor = "#64748b";

                    if ("GREEN".equalsIgnoreCase(signal)) {
                        signalColor = "#15803d";
                    } else if ("YELLOW".equalsIgnoreCase(signal)) {
                        signalColor = "#ca8a04";
                    } else if ("RED".equalsIgnoreCase(signal)) {
                        signalColor = "#dc2626";
                    }

                    String densityColor = "#15803d";

                    if ("MEDIUM".equalsIgnoreCase(density)) {
                        densityColor = "#ca8a04";
                    } else if ("HIGH".equalsIgnoreCase(density)) {
                        densityColor = "#dc2626";
                    }

                   junctionCards.append(
                        "<article class='stat' style='padding:12px;min-width:0'>" +
                        "<span style='font-size:13px'>Junction</span>" +
                        "<h3 style='font-size:18px;margin:6px 0'>" +
                        HtmlUtil.e(junction) + "</h3>" +
                        "<p style='font-size:13px;margin:5px 0'>Signal: " +
                        "<strong class='signal-phase' style='color:" + signalColor + "'>" +
                        HtmlUtil.e(signal) + "</strong></p>" +
                        "<p style='font-size:13px;margin:5px 0'>Traffic Density: " +
                        "<strong style='color:" + densityColor + "'>" +
                        HtmlUtil.e(density) + "</strong></p>" +
                        "<p style='font-size:13px;margin:5px 0'>Time Remaining: " +
                       "<strong class='signal-countdown' data-phase='" + signal +
                        "' data-density='" + density +
                        "' data-remaining='" + remaining +
                        "' data-started='" + cycleStartedEpoch + "'>" +
                        remaining + " seconds</strong></p>" +
                        "</article>"
                       
                    );
                 }
            }

        } catch (Exception e) {
            log("Officer dashboard error", e);
        }

        if (junctionCards.length() == 0) {
            junctionCards.append(
                "<p>Junction monitoring data is unavailable. " +
                "Please check the database connection.</p>"
            );
        }

        response.setContentType("text/html;charset=UTF-8");

        String name = (String) s.getAttribute("userName");

        response.getWriter().println(
            HtmlUtil.pageStart("Officer Dashboard", name, "Officer") +

            HtmlUtil.nav("Officer") +

            "<section class='hero'>" +
            "<p class='eyebrow'>OFFICER CONTROL PANEL</p>" +
            "<h1>Welcome, " + HtmlUtil.e(name) + "</h1>" +
            "<p>Manage traffic signals, traffic density, emergency requests, vehicles, challans and citizen reports.</p>" +
            "</section>" +

            "<section class='cards'>" +

            "<article class='stat'>" +
            "<span>Registered Vehicles</span>" +
            "<strong>" + vehicles + "</strong>" +
            "<a href='vehicles'>View vehicles →</a>" +
            "</article>" +

            "<article class='stat'>" +
            "<span>Pending Challans</span>" +
            "<strong>" + pending + "</strong>" +
            "<a href='challans'>Manage challans →</a>" +
            "</article>" +

            "<article class='stat'>" +
            "<span>Open Reports</span>" +
            "<strong>" + reports + "</strong>" +
            "<a href='reports'>Review reports →</a>" +
            "</article>" +

            "</section>" +

            "<section class='panel'>" +
            "<h2>Live Traffic Monitoring</h2>" +
            "<p>Current signal status and saved traffic density for each junction.</p>" +
            "<div class='cards'>" +
            junctionCards.toString() +
            "</div>" +
            "<div class='quicklinks'>" +
            "<a href='signals'>Manage Signals and Density</a>" +
            "</div>" +
            "</section>" +

            "<section class='panel'>" +
            "<h2>Traffic Management</h2>" +
            "<div class='quicklinks'>" +
            "<a href='signals'>Traffic Signal Management</a>" +
            "<a href='emergency'>Emergency Priority</a>" +
            "</div>" +
            "</section>" +

            "<section class='panel'>" +
            "<h2>Officer Tasks</h2>" +
            "<div class='quicklinks'>" +
            "<a href='vehicles'>View All Vehicles</a>" +
            "<a href='challans'>Create / View E-Challans</a>" +
            "<a href='reports'>Review Traffic Reports</a>" +
            "</div>" +
            "</section>" +

          "<script>" +
            "document.querySelectorAll('.signal-countdown').forEach(function(timer) {" +
            "  const card = timer.closest('article');" +
            "  const signalText = card.querySelector('.signal-phase');" +
            "  const density = timer.dataset.density;" +
            "  const started = Number(timer.dataset.started);" +
            "  const green = density === 'HIGH' ? 60 : density === 'MEDIUM' ? 40 : 30;" +
            "  const yellow = 5;" +
            "  const red = density === 'HIGH' ? 30 : density === 'MEDIUM' ? 45 : 60;" +
            "  const cycle = green + yellow + red;" +
            "  function update() {" +
            "    const elapsed = Math.max(0, Math.floor(Date.now() / 1000 - started));" +
            "    const position = elapsed % cycle;" +
            "    let signal, remaining;" +
            "    if (position < green) {" +
            "      signal = 'GREEN'; remaining = green - position;" +
            "    } else if (position < green + yellow) {" +
            "      signal = 'YELLOW'; remaining = green + yellow - position;" +
            "    } else {" +
            "      signal = 'RED'; remaining = cycle - position;" +
            "    }" +
            "    signalText.textContent = signal;" +
            "    signalText.style.color = signal === 'GREEN' ? '#15803d' : signal === 'YELLOW' ? '#ca8a04' : '#dc2626';" +
            "    timer.dataset.phase = signal;" +
            "    timer.textContent = remaining + ' seconds';" +
            "  }" +
            "  update();" +
            "  setInterval(update, 1000);" +
            "});" +
            "</script>" +
            HtmlUtil.pageEnd()
        );
        
    }
}
