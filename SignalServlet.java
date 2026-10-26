
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/signals")
public class SignalServlet extends HttpServlet {

    private static final String[] JUNCTIONS = {
        "Anna Nagar", "Guindy", "T Nagar", "Adyar"
    };

    private static final String[] DENSITIES = {
        "LOW", "MEDIUM", "HIGH"
    };

    private static final String[] PHASES = {
        "GREEN", "YELLOW", "RED"
    };

    private static int duration(String density, String phase) {
        switch (density) {
            case "LOW":
                if ("GREEN".equals(phase)) return 30;
                if ("YELLOW".equals(phase)) return 5;
                return 60;

            case "MEDIUM":
                if ("GREEN".equals(phase)) return 40;
                if ("YELLOW".equals(phase)) return 5;
                return 45;

            case "HIGH":
                if ("GREEN".equals(phase)) return 60;
                if ("YELLOW".equals(phase)) return 5;
                return 30;

            default:
                return 30;
        }
    }

    private static class JunctionState {
        String junction;
        String density;
        String phase;
        int remaining;

        JunctionState(String junction, String density,
                      String phase, int remaining) {
            this.junction = junction;
            this.density = density;
            this.phase = phase;
            this.remaining = remaining;
        }
    }

    private JunctionState calculate(String junction, String density,
                                    long elapsed) {
        int total = duration(density, "GREEN")
                  + duration(density, "YELLOW")
                  + duration(density, "RED");

        long position = Math.floorMod(elapsed, (long) total);

        for (String phase : PHASES) {
            int seconds = duration(density, phase);

            if (position < seconds) {
                int remaining = seconds - (int) position;

                return new JunctionState(
                    junction, density, phase, remaining
                );
            }

            position -= seconds;
        }

        return new JunctionState(junction, density, "GREEN",
                                 duration(density, "GREEN"));
    }

    private boolean isOfficer(HttpServletRequest request,
                              HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);

        if (session == null ||
            !"Officer".equalsIgnoreCase(
                String.valueOf(session.getAttribute("role")))) {
            response.sendError(403,
                "Officer access required. Please log in.");
            return false;
        }

        return true;
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException {

        if (!isOfficer(request, response)) return;

        response.setContentType("text/html;charset=UTF-8");

        List<JunctionState> states = new ArrayList<>();

        try (Connection con = DBConnection.getConnection()) {

            try (PreparedStatement init = con.prepareStatement(
                    "UPDATE traffic_signals " +
                    "SET cycle_started_at=CURRENT_TIMESTAMP " +
                    "WHERE cycle_started_at IS NULL")) {
                init.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT junction_name, traffic_density, " +
                    "TIMESTAMPDIFF(SECOND, cycle_started_at, " +
                    "CURRENT_TIMESTAMP) AS elapsed_seconds " +
                    "FROM traffic_signals ORDER BY signal_id");
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    String junction = rs.getString("junction_name");
                    String density = rs.getString("traffic_density");

                    if (!Arrays.asList(JUNCTIONS).contains(junction) ||
                        !Arrays.asList(DENSITIES).contains(density)) {
                        continue;
                    }

                    long elapsed = rs.getLong("elapsed_seconds");

                    states.add(calculate(junction, density, elapsed));
                }
            }

            try (PreparedStatement update = con.prepareStatement(
                    "UPDATE traffic_signals SET signal_status=? " +
                    "WHERE junction_name=?")) {

                for (JunctionState state : states) {
                    update.setString(1, state.phase);
                    update.setString(2, state.junction);
                    update.addBatch();
                }

                update.executeBatch();
            }

        } catch (Exception e) {
            log("Unable to calculate signal timing", e);
            response.sendError(500,
                "Unable to calculate signal timing. Check the server log.");
            return;
        }

        var out = response.getWriter();

        out.println("<!DOCTYPE html><html><head>");
        out.println("<meta charset='UTF-8'>");
        out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
        out.println("<title>Traffix Signal Management</title>");
        out.println("<style>");
        out.println("body{font-family:Arial,sans-serif;margin:25px;background:#f1f5f9;color:#0f172a}");
        out.println("h1{color:#1d4ed8}");
        out.println("table{border-collapse:collapse;width:100%;background:white}");
        out.println("th,td{padding:13px;border:1px solid #cbd5e1;text-align:left}");
        out.println("th{background:#1d4ed8;color:white}");
        out.println(".signal{display:inline-block;padding:8px 13px;border-radius:20px;color:white;font-weight:bold;min-width:65px;text-align:center}");
        out.println(".GREEN{background:#15803d}.YELLOW{background:#ca8a04}.RED{background:#dc2626}");
        out.println(".timer{font-size:20px;font-weight:bold}");
        out.println("select,button{padding:9px;margin:3px}");
        out.println("button{background:#1d4ed8;color:white;border:0;border-radius:5px;cursor:pointer}");
        out.println(".note{background:white;padding:14px;border-radius:8px;margin:15px 0}");
        out.println("@media(max-width:650px){body{margin:10px}th,td{padding:7px;font-size:13px}}");
        out.println("</style></head><body>");

        out.println("<h1>Traffic Signal Management</h1>");
        out.println("<p>Signal phase and countdown are calculated from the shared database cycle.</p>");

        out.println("<div class='note'><b>Simulation timing</b><br>");
        out.println("LOW: Green 30s, Yellow 5s, Red 60s<br>");
        out.println("MEDIUM: Green 40s, Yellow 5s, Red 45s<br>");
        out.println("HIGH: Green 60s, Yellow 5s, Red 30s</div>");

        out.println("<table><tr><th>Junction</th><th>Traffic Density</th>");
        out.println("<th>Current Signal</th><th>Time Remaining</th>");
        out.println("<th>Update Density</th></tr>");

        for (JunctionState state : states) {
            out.println("<tr class='junction' data-junction='" +
                state.junction + "' data-density='" + state.density +
                "' data-phase='" + state.phase +
                "' data-remaining='" + state.remaining + "'>");

            out.println("<td>" + state.junction + "</td>");
            out.println("<td class='density'>" + state.density + "</td>");
            out.println("<td><span class='signal " + state.phase + "'>" +
                state.phase + "</span></td>");
            out.println("<td><span class='timer'>" + state.remaining +
                " seconds</span></td>");

            out.println("<td><form method='post' action='signals'>");
            out.println("<input type='hidden' name='junction' value='" +
                state.junction + "'>");
            out.println("<select name='density'>");

            for (String d : DENSITIES) {
                out.println("<option value='" + d + "'" +
                    (d.equals(state.density) ? " selected" : "") +
                    ">" + d + "</option>");
            }

            out.println("</select><button type='submit'>Save</button>");
            out.println("</form></td></tr>");
        }

        out.println("</table>");
        out.println("<p><a href='officer-dashboard'>Back to Dashboard</a></p>");

        out.println("<script>");
        out.println("const timings={LOW:{GREEN:30,YELLOW:5,RED:60},MEDIUM:{GREEN:40,YELLOW:5,RED:45}};");
        out.println("timings.HIGH={GREEN:60,YELLOW:5,RED:30};");
        out.println("const phases=['GREEN','YELLOW','RED'];");

        out.println("document.querySelectorAll('.junction').forEach(row=>{");
        out.println("let density=row.dataset.density;");
        out.println("let phaseIndex=phases.indexOf(row.dataset.phase);");
        out.println("let remaining=Number(row.dataset.remaining);");
        out.println("const signal=row.querySelector('.signal');");
        out.println("const timer=row.querySelector('.timer');");

        out.println("function showPhase(){");
        out.println("const phase=phases[phaseIndex];");
        out.println("signal.textContent=phase;");
        out.println("signal.className='signal '+phase;");
        out.println("timer.textContent=remaining+' seconds';");
        out.println("fetch('signals',{method:'POST',credentials:'same-origin',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({action:'cycle',junction:row.dataset.junction,signal:phase})})");
        out.println(".then(r=>{if(!r.ok)console.error('Signal update failed');})");
        out.println(".catch(e=>console.error('Signal update error:',e));");
        out.println("}");

        out.println("setInterval(()=>{");
        out.println("remaining--;");
        out.println("if(remaining<=0){");
        out.println("phaseIndex=(phaseIndex+1)%phases.length;");
        out.println("remaining=timings[density][phases[phaseIndex]];");
        out.println("showPhase();");
        out.println("}else{timer.textContent=remaining+' seconds';}");
        out.println("},1000);");
        out.println("});");
        out.println("</script></body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws IOException {

        if (!isOfficer(request, response)) return;

        String junction = request.getParameter("junction");

        if (junction == null ||
            !Arrays.asList(JUNCTIONS).contains(junction)) {
            response.sendError(400, "Invalid junction.");
            return;
        }

        String action = request.getParameter("action");

        if ("cycle".equals(action)) {
            String signal = request.getParameter("signal");

            if (!Arrays.asList(PHASES).contains(signal)) {
                response.sendError(400, "Invalid signal status.");
                return;
            }

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(
                     "UPDATE traffic_signals SET signal_status=? " +
                     "WHERE junction_name=?")) {

                ps.setString(1, signal);
                ps.setString(2, junction);
                ps.executeUpdate();

                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("OK");

            } catch (Exception e) {
                log("Unable to update automatic signal status", e);
                response.sendError(500, "Unable to update signal status.");
            }

            return;
        }

        String density = request.getParameter("density");

        if (!Arrays.asList(DENSITIES).contains(density)) {
            response.sendError(400, "Invalid traffic density.");
            return;
        }

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "UPDATE traffic_signals SET traffic_density=?, " +
                 "cycle_started_at=CURRENT_TIMESTAMP, signal_status='GREEN' " +
                 "WHERE junction_name=?")) {

            ps.setString(1, density);
            ps.setString(2, junction);

            if (ps.executeUpdate() != 1) {
                response.sendError(404, "Junction not found.");
                return;
            }

            response.sendRedirect("signals");

        } catch (Exception e) {
            log("Unable to update traffic density", e);
            response.sendError(500, "Unable to save traffic density.");
        }
    }
}
