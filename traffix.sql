CREATE DATABASE IF NOT EXISTS traffix;
USE traffix;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS traffic_reports;
DROP TABLE IF EXISTS challans;
DROP TABLE IF EXISTS vehicles;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role ENUM('Officer','Citizen') NOT NULL
);

CREATE TABLE vehicles (
    vehicle_id INT PRIMARY KEY AUTO_INCREMENT,
    vehicle_number VARCHAR(20) UNIQUE NOT NULL,
    owner_name VARCHAR(100) NOT NULL,
    owner_email VARCHAR(100) NOT NULL,
    vehicle_type VARCHAR(50) NOT NULL,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vehicle_owner FOREIGN KEY (owner_email)
        REFERENCES users(email) ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE challans (
    challan_id INT PRIMARY KEY AUTO_INCREMENT,
    vehicle_number VARCHAR(20) NOT NULL,
    violation VARCHAR(150) NOT NULL,
    fine_amount DECIMAL(10,2) NOT NULL,
    challan_date DATE NOT NULL,
    payment_status ENUM('Pending','Paid') NOT NULL DEFAULT 'Pending',
    issued_by INT NOT NULL,
    CONSTRAINT fk_challan_vehicle FOREIGN KEY (vehicle_number)
        REFERENCES vehicles(vehicle_number) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_challan_officer FOREIGN KEY (issued_by)
        REFERENCES users(user_id) ON DELETE RESTRICT
);

CREATE TABLE traffic_reports (
    report_id INT PRIMARY KEY AUTO_INCREMENT,
    citizen_id INT NOT NULL,
    location VARCHAR(200) NOT NULL,
    report_type VARCHAR(100) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    report_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('Submitted','Under Review','Resolved') NOT NULL DEFAULT 'Submitted',
    CONSTRAINT fk_report_citizen FOREIGN KEY (citizen_id)
        REFERENCES users(user_id) ON DELETE CASCADE
);

INSERT INTO users (name, email, password, role) VALUES
('Demo Officer', 'officer@traffix.com', 'demo123', 'Officer'),
('Demo Citizen', 'citizen@traffix.com', 'demo123', 'Citizen');

INSERT INTO vehicles (vehicle_number, owner_name, owner_email, vehicle_type) VALUES
('TN01AB1234', 'Demo Citizen', 'citizen@traffix.com', 'Car'),
('TN09CD5678', 'Demo Citizen', 'citizen@traffix.com', 'Motorcycle');

INSERT INTO challans (vehicle_number, violation, fine_amount, challan_date, payment_status, issued_by)
SELECT 'TN01AB1234', 'Speed limit violation', 1000.00, CURRENT_DATE, 'Pending', user_id
FROM users WHERE email = 'officer@traffix.com';

INSERT INTO traffic_reports (citizen_id, location, report_type, description, status)
SELECT user_id, 'Anna Salai, Chennai', 'Signal issue',
       'Traffic signal is not working properly.', 'Submitted'
FROM users WHERE email = 'citizen@traffix.com';
