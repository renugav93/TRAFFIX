public class HtmlUtil {
    private HtmlUtil() {}

    public static String e(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }

    public static String pageStart(String title, String userName, String role) {
        return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<title>" + e(title) + " | Traffix</title>" +
            "<link rel='stylesheet' href='assets/style.css'></head><body>" +
            "<header class='topbar'><a class='brand' href='index.html'>TRAFFIX</a>" +
            "<span>Smart Traffic Management System</span>" +
            (userName == null ? "" : "<div class='account'>" + e(userName) + " · " + e(role) +
            " <a class='logout' href='logout'>Logout</a></div>") +
            "</header><main class='container'>";
    }

    public static String pageEnd() {
        return "</main><footer>TRAFFIX · Smart Traffic Management System · College Project</footer></body></html>";
    }

    public static String nav(String role) {
        String links = "<a href='" + (role.equals("Officer") ? "officer-dashboard" : "citizen-dashboard") + "'>Dashboard</a>";
        if (role.equals("Officer")) {
            links += "<a href='vehicles'>Vehicles</a><a href='challans'>Challans</a><a href='reports'>Traffic Reports</a>";
        } else {
            links += "<a href='vehicles'>My Vehicles</a><a href='challans'>My Challans</a><a href='reports'>Report Traffic Issue</a>";
        }
        return "<nav class='nav'>" + links + "</nav>";
    }

    public static String message(String value) {
        return value == null || value.isBlank() ? "" : "<p class='notice'>" + e(value) + "</p>";
    }
}
