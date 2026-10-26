TRAFFIX — SMART TRAFFIC MANAGEMENT SYSTEM
=========================================

Technology:
- Java Servlets (Jakarta Servlet API)
- Apache Tomcat 10.1
- MySQL 8
- JDBC
- HTML/CSS

IMPORTANT:
1. This project is written for Tomcat 10.1 (jakarta.servlet.*).
2. Never upload real database passwords to GitHub.
3. This is a college/demo project. Passwords are compared as plain text for simplicity.
   For production, use password hashing and stronger validation.
4. The "Pay Demo" action only changes the database status to Paid; it does not process real money.

PROJECT STRUCTURE
-----------------
sql/traffix.sql
src/DBConnection.java
src/HtmlUtil.java
src/LoginServlet.java
src/LogoutServlet.java
src/OfficerDashboardServlet.java
src/CitizenDashboardServlet.java
src/VehiclesServlet.java
src/ChallansServlet.java
src/ReportsServlet.java
src/PaymentServlet.java
web/index.html
web/login.html
web/register.html
web/assets/style.css
web/WEB-INF/web.xml

SETUP
-----
A. Create the database
1. Open MySQL Workbench.
2. Open sql/traffix.sql and run it.
3. It creates the database and tables and adds demo users/vehicles.

Demo login:
Officer:
  Email: officer@traffix.com
  Password: demo123
  Role: Officer

Citizen:
  Email: citizen@traffix.com
  Password: demo123
  Role: Citizen

B. Configure database connection
Open src/DBConnection.java and set PASSWORD to your own MySQL password.
Do not paste your actual password into public repositories.

C. Compile (run these commands from the Traffix project folder in PowerShell)
Make sure the MySQL Connector/J JAR is located at:
web/WEB-INF/lib/mysql-connector-j-<version>.jar

Create output folder:
New-Item -ItemType Directory -Force web/WEB-INF/classes | Out-Null

Compile:
javac -cp "C:\Program Files\Apache Software Foundation\Tomcat 10.1\lib\servlet-api.jar;web\WEB-INF\lib\*" -d web\WEB-INF\classes src\*.java

D. Deploy
1. Copy the entire contents of the web folder into:
   C:\Program Files\Apache Software Foundation\Tomcat 10.1\webapps\Traffix
2. Copy the compiled classes into:
   C:\Program Files\Apache Software Foundation\Tomcat 10.1\webapps\Traffix\WEB-INF\classes
3. Ensure the MySQL connector JAR is in:
   C:\Program Files\Apache Software Foundation\Tomcat 10.1\webapps\Traffix\WEB-INF\lib
4. Restart Tomcat (Administrator PowerShell may be required).
5. Open:
   http://localhost:8080/Traffix/

If compiling from the project root, these commands can copy files:
Copy-Item web\* "C:\Program Files\Apache Software Foundation\Tomcat 10.1\webapps\Traffix\" -Recurse -Force
Copy-Item web\WEB-INF\classes\*.class "C:\Program Files\Apache Software Foundation\Tomcat 10.1\webapps\Traffix\WEB-INF\classes\" -Force

FEATURES
--------
- Login with Citizen/Officer role selection
- Role-protected dashboards
- Citizen vehicle registration and own-vehicle list
- Officer view of all registered vehicles
- Officers issue challans to registered vehicles
- Citizens view challans for their own vehicles
- Demo payment status update
- Citizens submit traffic reports/complaints
- Officers review reports and update report status
- Logout and session invalidation

NOTE ABOUT VEHICLE OWNERSHIP
----------------------------
The SQL demo rows link the sample citizen to a vehicle using owner_email.
When a citizen registers a vehicle, the current account's email is stored as owner_email.
