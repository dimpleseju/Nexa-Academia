import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.time.LocalDate; // For date handling in timetable

public class UniversityManagementSystem {
    // ANSI Color Codes for UI Enhancement (Using softer tones)
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";        // Errors
    public static final String GREEN = "\u001B[32m";      // Success
    public static final String YELLOW = "\u001B[33m";     // Warnings/Highlights
    public static final String BLUE = "\u001B[34m";       // Professor Header
    public static final String SOFTER_MAGENTA = "\u001B[95m"; // Admin Header
    public static final String SOFTER_BLUE = "\u001B[94m";    // General Highlights
    public static final String WHITE = "\u001B[37m";      // General text
    public static final String BOLD = "\u001B[1m";

    private static final String DB_URL = "jdbc:mysql://localhost:3306/university";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    private static Connection connection;
    private static Scanner scanner = new Scanner(System.in);
    private static int currentUserId = -1;
    private static String currentUserRole = "";

    // Utility method for hashing passwords
    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println(RED + "Error hashing password: Algorithm not found." + RESET);
            return null;
        }
    }

    // Utility method to create directories
    private static void createExportDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                // System.out.println(GREEN + "Created export directory: " + path + RESET); // Optional debug
            } else {
                System.err.println(RED + "Failed to create export directory: " + path + RESET);
            }
        }
    }

    // Utility method to write data to a file
    private static boolean writeToFile(String filePath, String data) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.print(data);
            return true;
        } catch (IOException e) {
            System.err.println(RED + "Error writing to file: " + e.getMessage() + RESET);
            return false;
        }
    }

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            initializeDatabase();

            System.out.println(BOLD + "" +
            "███╗   ██╗███████╗██╗  ██╗ █████╗        █████╗  ██████╗ █████╗ ██████╗ ███████╗███╗   ███╗██╗ █████╗\n"+
            "████╗  ██║██╔════╝╚██╗██╔╝██╔══██╗      ██╔══██╗██╔════╝██╔══██╗██╔══██╗██╔════╝████╗ ████║██║██╔══██╗\n"+
            "██╔██╗ ██║█████╗   ╚███╔╝ ███████║█████╗███████║██║     ███████║██║  ██║█████╗  ██╔████╔██║██║███████║\n"+
            "██║╚██╗██║██╔══╝   ██╔██╗ ██╔══██║╚════╝██╔══██║██║     ██╔══██║██║  ██║██╔══╝  ██║╚██╔╝██║██║██╔══██║\n"+
            "██║ ╚████║███████╗██╔╝ ██╗██║  ██║      ██║  ██║╚██████╗██║  ██║██████╔╝███████╗██║ ╚═╝ ██║██║██║  ██║\n"+
            "╚═╝  ╚═══╝╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝      ╚═╝  ╚═╝ ╚═════╝╚═╝  ╚═╝╚═════╝ ╚══════╝╚═╝     ╚═╝╚═╝╚═╝  ╚═╝\n"
            + RESET);

            System.out.println(SOFTER_BLUE + "Connecting to database..." + RESET);
            animateLoading();

            while (true) {
                if (currentUserId == -1) {
                    showLoginMenu();
                } else {
                    switch (currentUserRole) {
                        case "admin": showAdminMenu(); break;
                        case "professor": showProfessorMenu(); break;
                        case "student": showStudentMenu(); break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(RED + "System error: " + e.getMessage() + RESET);
            e.printStackTrace();
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println(RED + "Error closing database connection: " + e.getMessage() + RESET);
            }
        }
    }

    private static void initializeDatabase() throws SQLException {
        Statement stmt = connection.createStatement();

        // Create tables with improved constraints
        stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "username VARCHAR(50) UNIQUE NOT NULL, " +
                "password VARCHAR(64) NOT NULL, " +
                "role ENUM('admin', 'professor', 'student') NOT NULL, " +
                "name VARCHAR(100) NOT NULL)");

        stmt.execute("CREATE TABLE IF NOT EXISTS courses (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "code VARCHAR(10) UNIQUE NOT NULL, " +
                "name VARCHAR(100) NOT NULL, " +
                "credits INT NOT NULL, " +
                "professor_id INT, " +
                "FOREIGN KEY (professor_id) REFERENCES users(id) ON DELETE SET NULL)");

        stmt.execute("CREATE TABLE IF NOT EXISTS enrollments (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "student_id INT NOT NULL, " +
                "course_id INT NOT NULL, " +
                "grade VARCHAR(2), " +
                "FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE, " +
                "UNIQUE KEY unique_enrollment (student_id, course_id))");

        stmt.execute("CREATE TABLE IF NOT EXISTS notifications (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "message TEXT NOT NULL, " +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "is_read BOOLEAN DEFAULT FALSE, " +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)");

        stmt.execute("CREATE TABLE IF NOT EXISTS messages (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "sender_id INT NOT NULL, " +
                "receiver_id INT NOT NULL, " +
                "subject VARCHAR(255), " +
                "content TEXT NOT NULL, " +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "is_read BOOLEAN DEFAULT FALSE, " +
                "FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE)");

        stmt.execute("CREATE TABLE IF NOT EXISTS attendance (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "student_id INT NOT NULL, " +
                "course_id INT NOT NULL, " +
                "attendance_date DATE NOT NULL, " +
                "status ENUM('Present', 'Absent') NOT NULL DEFAULT 'Absent', " +
                "FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE, " +
                "UNIQUE KEY unique_attendance (student_id, course_id, attendance_date))");

        // >>>>>>>>>> ADD TIMETABLE TABLE <<<<<<<<<<
        stmt.execute("CREATE TABLE IF NOT EXISTS timetable (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "course_id INT NOT NULL, " +
                "day_of_week ENUM('Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday') NOT NULL, " +
                "start_time TIME NOT NULL, " +
                "end_time TIME NOT NULL, " +
                "location VARCHAR(100), " +
                "FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE, " +
                "UNIQUE KEY unique_slot (course_id, day_of_week, start_time))");
        // >>>>>>>>>> END TIMETABLE TABLE <<<<<<<<<<

        // Create default admin if not exists (with hashed password)
        PreparedStatement checkAdmin = connection.prepareStatement(
                "SELECT COUNT(*) FROM users WHERE role='admin'");
        ResultSet rs = checkAdmin.executeQuery();
        rs.next();
        if (rs.getInt(1) == 0) {
            String hashedPassword = hashPassword("admin123");
            if (hashedPassword != null) {
                PreparedStatement createAdmin = connection.prepareStatement(
                        "INSERT INTO users (username, password, role, name) VALUES (?, ?, ?, ?)");
                createAdmin.setString(1, "admin");
                createAdmin.setString(2, hashedPassword);
                createAdmin.setString(3, "admin");
                createAdmin.setString(4, "System Administrator");
                createAdmin.execute();
                System.out.println(GREEN + "Default admin user created." + RESET);
            } else {
                System.err.println(RED + "Failed to create default admin user due to password hashing error." + RESET);
            }
        }
    }

    // --- Utility Methods for Input Validation ---
    private static String getStringInput(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        while (input.isEmpty()) {
            System.out.print(RED + "Input cannot be empty. " + prompt + RESET);
            input = scanner.nextLine().trim();
        }
        return input;
    }

    private static int getIntInput(String prompt) {
        System.out.print(prompt);
        while (true) {
            try {
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    System.out.print(RED + "Input cannot be empty. " + prompt + RESET);
                    continue;
                }
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.print(RED + "Invalid number format. " + prompt + RESET);
            }
        }
    }

    private static String getValidatedRoleInput(String prompt) {
        System.out.print(prompt);
        while (true) {
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.isEmpty()) {
                System.out.print(RED + "Input cannot be empty. " + prompt + RESET);
                continue;
            }
            if (input.equals("admin") || input.equals("professor") || input.equals("student")) {
                return input;
            } else {
                System.out.print(RED + "Invalid role. Please enter 'admin', 'professor', or 'student'. " + prompt + RESET);
            }
        }
    }

    private static String getValidatedGradeInput(String prompt) {
        System.out.print(prompt);
        while (true) {
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.isEmpty()) {
                System.out.print(RED + "Input cannot be empty. " + prompt + RESET);
                continue;
            }
            if (input.matches("[A-F][+-]?") || input.equals("F")) {
                return input;
            } else {
                System.out.print(RED + "Invalid grade format. Please enter a valid grade (e.g., A, B+, C-, F). " + prompt + RESET);
            }
        }
    }



    private static boolean getConfirmation(String prompt) {
        System.out.print(YELLOW + prompt + " (y/n): " + RESET);
        String input = scanner.nextLine().trim().toLowerCase();
        return input.startsWith("y");
    }
    // --- End Utility Methods ---

    private static void showLoginMenu() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "🎓 UNIVERSITY LOGIN" + RESET);
        System.out.println(BOLD + "==================" + RESET);
        System.out.println("1. Login");
        System.out.println("0. Exit System");
        int choice = getIntInput(" Select option: ");
        if (choice == 0) {
            System.out.println(GREEN + "👋 Goodbye!" + RESET);
            System.exit(0);
        } else if (choice == 1) {
            String username = getStringInput("Username: ");
            String password = getStringInput("Password: ");
            String hashedPassword = hashPassword(password);
            try {
                PreparedStatement stmt = connection.prepareStatement(
                        "SELECT id, role FROM users WHERE username=? AND password=?");
                stmt.setString(1, username);
                stmt.setString(2, hashedPassword);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    currentUserId = rs.getInt("id");
                    currentUserRole = rs.getString("role");
                    System.out.println(GREEN + "✅ Login successful! Welcome " + currentUserRole + RESET);
                    pressEnterToContinue();
                } else {
                    System.out.println(RED + "❌ Invalid credentials!" + RESET);
                    pressEnterToContinue();
                }
            } catch (SQLException e) {
                System.err.println(RED + "Login error: " + e.getMessage() + RESET);
            }
        } else {
            System.out.println(RED + "Invalid option!" + RESET);
            pressEnterToContinue();
        }
    }

    private static void showAdminMenu() {
        clearScreen();
        System.out.println(BOLD + SOFTER_MAGENTA + "👑 ADMINISTRATOR DASHBOARD" + RESET);
        System.out.println(BOLD + "==========================" + RESET);
        System.out.println("1. Manage Users");
        System.out.println("2. Manage Courses");
        System.out.println("3. View All Enrollments");
        System.out.println("4. Send Notification");
        System.out.println("5. View Notifications");
        System.out.println("6. Chatbot Assistant");
        System.out.println("7. Export Data");
        // >>>>>>>>>> ADD TIMETABLE OPTION <<<<<<<<<<
        System.out.println("8. Manage Timetable");
        // >>>>>>>>>> ADD FAQ OPTION <<<<<<<<<<
        System.out.println("9. FAQ / Help Guide");
        // >>>>>>>>>> END ADDITIONS <<<<<<<<<<
        System.out.println("0. Logout");
        int choice = getIntInput(" Select option: ");
        switch (choice) {
            case 1: manageUsers(); break;
            case 2: manageCourses(); break;
            case 3: viewAllEnrollments(); break;
            case 4: sendNotification(); break;
            case 5: viewNotifications(); break;
            case 6: chatbotAssistant(); break;
            case 7: exportDataMenu(); break;
            // >>>>>>>>>> ADD TIMETABLE CASE <<<<<<<<<<
            case 8: manageTimetable(); break;
            // >>>>>>>>>> ADD FAQ CASE <<<<<<<<<<
            case 9: showAdminFAQ(); break;
            // >>>>>>>>>> END ADDITIONS <<<<<<<<<<
            case 0: logout(); break;
            default: System.out.println(RED + "Invalid option!" + RESET); pressEnterToContinue();
        }
    }

    private static void showProfessorMenu() {
        clearScreen();
        System.out.println(BOLD + BLUE + "👨‍🏫 PROFESSOR DASHBOARD" + RESET);
        System.out.println(BOLD + "=====================" + RESET);
        System.out.println("1. View My Courses");
        System.out.println("2. Grade Students");
        System.out.println("3. Take Attendance");
        System.out.println("4. View Notifications");
        System.out.println("5. Chatbot Assistant");
        System.out.println("6. Send Message");
        System.out.println("7. Export Data");
        // >>>>>>>>>> ADD TIMETABLE VIEW OPTION <<<<<<<<<<
        System.out.println("8. View My Timetable");
        // >>>>>>>>>> ADD FAQ OPTION <<<<<<<<<<
        System.out.println("9. FAQ / Help Guide");
        // >>>>>>>>>> END ADDITIONS <<<<<<<<<<
        System.out.println("0. Logout");
        int choice = getIntInput(" Select option: ");
        switch (choice) {
            case 1: viewMyCourses(); break;
            case 2: gradeStudents(); break;
            case 3: takeAttendance(); break;
            case 4: viewNotifications(); break;
            case 5: chatbotAssistant(); break;
            case 6: sendMessage(); break;
            case 7: exportDataMenu(); break;
            // >>>>>>>>>> ADD TIMETABLE VIEW CASE <<<<<<<<<<
            case 8: viewTimetable(); break;
            // >>>>>>>>>> ADD FAQ CASE <<<<<<<<<<
            case 9: showProfessorFAQ(); break;
            // >>>>>>>>>> END ADDITIONS <<<<<<<<<<
            case 0: logout(); break;
            default: System.out.println(RED + "Invalid option!" + RESET); pressEnterToContinue();
        }
    }

    private static void showStudentMenu() {
        clearScreen();
        System.out.println(BOLD + GREEN + "📚 STUDENT DASHBOARD" + RESET);
        System.out.println(BOLD + "===================" + RESET);
        System.out.println("1. View Available Courses");
        System.out.println("2. Enroll in Course");
        System.out.println("3. View My Enrollments");
        System.out.println("4. View Notifications");
        System.out.println("5. Chatbot Assistant");
        System.out.println("6. Send Message"); // Changed from sendNotification to sendMessage
        System.out.println("7. Export Data");
        // >>>>>>>>>> ADD ATTENDANCE VIEW OPTION <<<<<<<<<<
        System.out.println("8. View My Attendance");
        // >>>>>>>>>> ADD TIMETABLE VIEW OPTION <<<<<<<<<<
        System.out.println("9. View My Timetable");
        // >>>>>>>>>> ADD FAQ OPTION <<<<<<<<<<
        System.out.println("10. FAQ / Help Guide");
        // >>>>>>>>>> END ADDITIONS <<<<<<<<<<
        System.out.println("0. Logout");
        int choice = getIntInput(" Select option: ");
        switch (choice) {
            case 1: viewAvailableCourses(); break;
            case 2: enrollInCourse(); break;
            case 3: viewMyEnrollments(); break;
            case 4: viewNotifications(); break;
            case 5: chatbotAssistant(); break;
            case 6: sendMessage(); break; // Changed from sendNotification to sendMessage
            case 7: exportDataMenu(); break;
            // >>>>>>>>>> ADD ATTENDANCE VIEW CASE <<<<<<<<<<
            case 8: viewMyAttendance(); break;
            // >>>>>>>>>> ADD TIMETABLE VIEW CASE <<<<<<<<<<
            case 9: viewTimetable(); break;
            // >>>>>>>>>> ADD FAQ CASE <<<<<<<<<<
            case 10: showStudentFAQ(); break;
            // >>>>>>>>>> END ADDITIONS <<<<<<<<<<
            case 0: logout(); break;
            default: System.out.println(RED + "Invalid option!" + RESET); pressEnterToContinue();
        }
    }

    // Admin functionalities
    private static void manageUsers() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "👥 USER MANAGEMENT" + RESET);
        System.out.println(BOLD + "=================" + RESET);
        System.out.println("1. Add User");
        System.out.println("2. View All Users");
        System.out.println("3. Edit User");
        System.out.println("4. Delete User");
        int choice = getIntInput(" Select option: ");
        switch (choice) {
            case 1: addUser(); break;
            case 2: viewAllUsers(); break;
            case 3: editUser(); break;
            case 4: deleteUser(); break;
            default: System.out.println(RED + "Invalid option!" + RESET); pressEnterToContinue();
        }
    }

    private static void addUser() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "➕ ADD NEW USER" + RESET);
        System.out.println(BOLD + "===============" + RESET);
        String username = getStringInput("Username: ");
        String password = getStringInput("Password: ");
        String hashedPassword = hashPassword(password);
        if (hashedPassword == null) {
            System.out.println(RED + "Error hashing password. User not added." + RESET);
            pressEnterToContinue();
            return;
        }
        String role = getValidatedRoleInput("Role (admin/professor/student): ");
        String name = getStringInput("Full Name: ");
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO users (username, password, role, name) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, role);
            stmt.setString(4, name);
            stmt.execute();
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int userId = generatedKeys.getInt(1);
                System.out.println(GREEN + "✅ User added successfully! User ID: " + userId + RESET);
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                System.out.println(RED + "❌ Error: Username already exists!" + RESET);
            } else {
                System.err.println(RED + "Error adding user: " + e.getMessage() + RESET);
            }
        }
        pressEnterToContinue();
    }

    private static void viewAllUsers() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "📋 ALL USERS" + RESET);
        System.out.println(BOLD + "===========" + RESET);
        String searchTerm = "";
        System.out.print("Search users (name/username/role) or press Enter to show all: ");
        searchTerm = scanner.nextLine().trim();
        try {
            String query = "SELECT * FROM users";
            if (!searchTerm.isEmpty()) {
                query += " WHERE name LIKE ? OR username LIKE ? OR role LIKE ?";
            }
            query += " ORDER BY role, name";
            PreparedStatement stmt = connection.prepareStatement(query);
            if (!searchTerm.isEmpty()) {
                String searchPattern = "%" + searchTerm + "%";
                stmt.setString(1, searchPattern);
                stmt.setString(2, searchPattern);
                stmt.setString(3, searchPattern);
            }
            ResultSet rs = stmt.executeQuery();
            System.out.printf("%-5s %-20s %-15s %-30s%n", "ID", "Username", "Role", "Name");
            System.out.println("------------------------------------------------------------");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-5d %-20s %-15s %-30s%n",
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getString("name"));
            }
            if (!found) {
                System.out.println("No users found matching the search term.");
            }
        } catch (SQLException e) {
            System.err.println(RED + "Error retrieving users: " + e.getMessage() + RESET);
        }
        pressEnterToContinue();
    }

    private static void editUser() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "✏️ EDIT USER" + RESET);
        System.out.println(BOLD + "===========" + RESET);
        viewAllUsers();
        System.out.println();
        int userId = getIntInput("Enter user ID to edit: ");
        try {
            PreparedStatement fetchStmt = connection.prepareStatement("SELECT * FROM users WHERE id = ?");
            fetchStmt.setInt(1, userId);
            ResultSet rs = fetchStmt.executeQuery();
            if (rs.next()) {
                String currentUsername = rs.getString("username");
                String currentRole = rs.getString("role");
                String currentName = rs.getString("name");
                System.out.println("Editing user: " + currentName + " (ID: " + userId + ")");
                System.out.println("Leave field blank to keep current value.");
                System.out.print("New Username (current: " + currentUsername + "): ");
                String newUsernameInput = scanner.nextLine().trim();
                String newUsername = newUsernameInput.isEmpty() ? currentUsername : newUsernameInput;
                System.out.print("New Password (leave blank to keep current): ");
                String newPasswordInput = scanner.nextLine().trim();
                String newHashedPassword;
                if (newPasswordInput.isEmpty()) {
                    newHashedPassword = rs.getString("password");
                } else {
                    newHashedPassword = hashPassword(newPasswordInput);
                    if (newHashedPassword == null) {
                        System.out.println(RED + "Error hashing new password. Password not updated." + RESET);
                        newHashedPassword = rs.getString("password");
                    }
                }
                System.out.print("New Role (current: " + currentRole + "): ");
                String newRoleInput = scanner.nextLine().trim();
                String newRole = newRoleInput.isEmpty() ? currentRole : getValidatedRoleInput("New Role (admin/professor/student): ");
                System.out.print("New Full Name (current: " + currentName + "): ");
                String newNameInput = scanner.nextLine().trim();
                String newName = newNameInput.isEmpty() ? currentName : newNameInput;

                PreparedStatement updateStmt = connection.prepareStatement(
                        "UPDATE users SET username=?, password=?, role=?, name=? WHERE id=?");
                updateStmt.setString(1, newUsername);
                updateStmt.setString(2, newHashedPassword);
                updateStmt.setString(3, newRole);
                updateStmt.setString(4, newName);
                updateStmt.setInt(5, userId);
                int rowsAffected = updateStmt.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println(GREEN + "✅ User updated successfully!" + RESET);
                } else {
                    System.out.println(YELLOW + "⚠️ No changes were made or user not found." + RESET);
                }
            } else {
                System.out.println(RED + "❌ User with ID " + userId + " not found!" + RESET);
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                System.out.println(RED + "❌ Error: Username already exists!" + RESET);
            } else {
                System.err.println(RED + "Error editing user: " + e.getMessage() + RESET);
            }
        }
        pressEnterToContinue();
    }

    private static void deleteUser() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "🗑️ DELETE USER" + RESET);
        System.out.println(BOLD + "=============" + RESET);
        viewAllUsers();
        System.out.println();
        int userId = getIntInput("Enter user ID to delete: ");
        if (userId == currentUserId) {
            System.out.println(RED + "❌ You cannot delete your own account!" + RESET);
            pressEnterToContinue();
            return;
        }
        try {
            PreparedStatement getNameStmt = connection.prepareStatement("SELECT name, username FROM users WHERE id = ?");
            getNameStmt.setInt(1, userId);
            ResultSet nameRs = getNameStmt.executeQuery();
            String userName = "Unknown User";
            if (nameRs.next()) {
                userName = nameRs.getString("name") + " (" + nameRs.getString("username") + ")";
            }
            if (!getConfirmation("Are you sure you want to delete user " + userName + " (ID: " + userId + ")?")) {
                System.out.println(YELLOW + "Deletion cancelled." + RESET);
                pressEnterToContinue();
                return;
            }
            PreparedStatement deleteStmt = connection.prepareStatement("DELETE FROM users WHERE id=?");
            deleteStmt.setInt(1, userId);
            int rows = deleteStmt.executeUpdate();
            if (rows > 0) {
                System.out.println(GREEN + "✅ User deleted successfully!" + RESET);
            } else {
                System.out.println(RED + "❌ User not found!" + RESET);
            }
        } catch (SQLException e) {
            System.err.println(RED + "Error deleting user: " + e.getMessage() + RESET);
        }
        pressEnterToContinue();
    }

    private static void manageCourses() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "📖 COURSE MANAGEMENT" + RESET);
        System.out.println(BOLD + "===================" + RESET);
        System.out.println("1. Add Course");
        System.out.println("2. View All Courses");
        System.out.println("3. Assign Professor");
        // >>>>>>>>>> ADD THIS LINE <<<<<<<<<<
        System.out.println("4. Edit Course");
        // >>>>>>>>>> END ADDITION <<<<<<<<<<
        int choice = getIntInput(" Select option: ");
        switch (choice) {
            case 1:
                addCourse();
                break;
            case 2:
                viewAllCourses();
                break;
            case 3:
                assignProfessor();
                break;
            // >>>>>>>>>> ADD THIS CASE <<<<<<<<<<
            case 4:
                editCourse();
                break;
            // >>>>>>>>>> END ADDITION <<<<<<<<<<
            default:
                System.out.println(RED + "Invalid option!" + RESET);
                pressEnterToContinue();
        }
    }

    private static void addCourse() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "➕ ADD NEW COURSE" + RESET);
        System.out.println(BOLD + "================" + RESET);
        String code = getStringInput("Course Code: ");
        String name = getStringInput("Course Name: ");
        int credits = getIntInput("Credits: ");
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO courses (code, name, credits) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, code);
            stmt.setString(2, name);
            stmt.setInt(3, credits);
            stmt.execute();
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                int courseId = generatedKeys.getInt(1);
                System.out.println(GREEN + "✅ Course added successfully! Course ID: " + courseId + RESET);
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                System.out.println(RED + "❌ Error: Course code already exists!" + RESET);
            } else {
                System.err.println(RED + "Error adding course: " + e.getMessage() + RESET);
            }
        }
        pressEnterToContinue();
    }

    private static void viewAllCourses() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "📋 ALL COURSES" + RESET);
        System.out.println(BOLD + "=============" + RESET);
        String searchTerm = "";
        System.out.print("Search courses (code/name) or press Enter to show all: ");
        searchTerm = scanner.nextLine().trim();
        try {
            String query = "SELECT c.*, u.name as professor FROM courses c LEFT JOIN users u ON c.professor_id = u.id";
            if (!searchTerm.isEmpty()) {
                query += " WHERE c.code LIKE ? OR c.name LIKE ?";
            }
            query += " ORDER BY c.code";
            PreparedStatement stmt = connection.prepareStatement(query);
            if (!searchTerm.isEmpty()) {
                String searchPattern = "%" + searchTerm + "%";
                stmt.setString(1, searchPattern);
                stmt.setString(2, searchPattern);
            }
            ResultSet rs = stmt.executeQuery();
            System.out.printf("%-5s %-10s %-30s %-10s %-25s%n", "ID", "Code", "Name", "Credits", "Professor");
            System.out.println("------------------------------------------------------------------------");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-5d %-10s %-30s %-10d %-25s%n",
                        rs.getInt("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getInt("credits"),
                        rs.getString("professor") != null ? rs.getString("professor") : "Not assigned");
            }
            if (!found) {
                System.out.println("No courses found matching the search term.");
            }
        } catch (SQLException e) {
            System.err.println(RED + "Error retrieving courses: " + e.getMessage() + RESET);
        }
        pressEnterToContinue();
    }

    // >>>>>>>>>> NEW METHOD: Edit Course Details <<<<<<<<<<
    private static void editCourse() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "✏️ EDIT COURSE" + RESET);
        System.out.println(BOLD + "=============" + RESET);

        // Show all courses first to select one
        viewAllCourses();
        System.out.println();

        int courseId = getIntInput("Enter course ID to edit: ");

        try {
            // Fetch current course details
            PreparedStatement fetchStmt = connection.prepareStatement("SELECT * FROM courses WHERE id = ?");
            fetchStmt.setInt(1, courseId);
            ResultSet rs = fetchStmt.executeQuery();

            if (rs.next()) {
                String currentCode = rs.getString("code");
                String currentName = rs.getString("name");
                int currentCredits = rs.getInt("credits");
                Integer currentProfessorId = rs.getObject("professor_id", Integer.class); // Handle potential NULL

                System.out.println("Editing course: " + currentCode + " - " + currentName + " (ID: " + courseId + ")");
                System.out.println("Leave field blank to keep current value.");

                System.out.print("New Course Code (current: " + currentCode + "): ");
                String newCodeInput = scanner.nextLine().trim();
                String newCode = newCodeInput.isEmpty() ? currentCode : newCodeInput;

                System.out.print("New Course Name (current: " + currentName + "): ");
                String newNameInput = scanner.nextLine().trim();
                String newName = newNameInput.isEmpty() ? currentName : newNameInput;

                System.out.print("New Credits (current: " + currentCredits + "): ");
                String newCreditsInput = scanner.nextLine().trim();
                int newCredits;
                if (newCreditsInput.isEmpty()) {
                    newCredits = currentCredits;
                } else {
                    try {
                        newCredits = Integer.parseInt(newCreditsInput);
                        if (newCredits <= 0) {
                            System.out.println(RED + "❌ Credits must be a positive number. Keeping current value." + RESET);
                            newCredits = currentCredits;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println(RED + "❌ Invalid number format for credits. Keeping current value." + RESET);
                        newCredits = currentCredits;
                    }
                }

                // Handle Professor Assignment/Change (Optional)
                System.out.println("Current Professor ID: " + (currentProfessorId != null ? currentProfessorId : "Not assigned"));
                System.out.print("Enter new Professor ID (or press Enter to keep current/leave unassigned): ");
                String newProfessorIdInput = scanner.nextLine().trim();
                Integer newProfessorId = currentProfessorId; // Default to current
                if (!newProfessorIdInput.isEmpty()) {
                    try {
                        int inputProfId = Integer.parseInt(newProfessorIdInput);
                        // Optional: Verify professor exists and is a professor
                        PreparedStatement profCheckStmt = connection.prepareStatement("SELECT id FROM users WHERE id = ? AND role = 'professor'");
                        profCheckStmt.setInt(1, inputProfId);
                        ResultSet profCheckRs = profCheckStmt.executeQuery();
                        if (profCheckRs.next()) {
                            newProfessorId = inputProfId;
                        } else {
                            System.out.println(YELLOW + "⚠️ Professor ID " + inputProfId + " not found or not a professor. Keeping current assignment." + RESET);
                        }
                    } catch (NumberFormatException e) {
                        System.out.println(YELLOW + "⚠️ Invalid Professor ID format. Keeping current assignment." + RESET);
                    }
                } else if (newProfessorIdInput.isEmpty() && currentProfessorId != null) {
                    // User pressed Enter and there was a professor assigned, they might want to unassign.
                    // Let's add a specific prompt for unassigning.
                    if (getConfirmation("Unassign the current professor (ID: " + currentProfessorId + ")?")) {
                        newProfessorId = null; // This will set it to NULL in the DB
                    }
                }

                // Confirm changes
                if (!getConfirmation("Update course details?")) {
                    System.out.println(YELLOW + "Course update cancelled." + RESET);
                    pressEnterToContinue();
                    return;
                }

                // Update the course
                // Use setObject to correctly handle potential NULL for professor_id
                PreparedStatement updateStmt = connection.prepareStatement(
                        "UPDATE courses SET code=?, name=?, credits=?, professor_id=? WHERE id=?");
                updateStmt.setString(1, newCode);
                updateStmt.setString(2, newName);
                updateStmt.setInt(3, newCredits);
                updateStmt.setObject(4, newProfessorId, Types.INTEGER); // Handles NULL correctly
                updateStmt.setInt(5, courseId);

                int rowsAffected = updateStmt.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println(GREEN + "✅ Course updated successfully!" + RESET);
                } else {
                    // This case is unlikely if the ID was found earlier, but good to have
                    System.out.println(YELLOW + "⚠️ No changes were made or course not found." + RESET);
                }

            } else {
                System.out.println(RED + "❌ Course with ID " + courseId + " not found!" + RESET);
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // MySQL error code for duplicate entry (e.g., code)
                System.out.println(RED + "❌ Error: Course code already exists!" + RESET);
            } else {
                System.err.println(RED + "Error editing course: " + e.getMessage() + RESET);
                // e.printStackTrace(); // Optional for debugging
            }
        }
        pressEnterToContinue();
    }
    // >>>>>>>>>> END NEW METHOD <<<<<<<<<<

    private static void assignProfessor() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "👨‍🏫 ASSIGN PROFESSOR TO COURSE" + RESET);
        System.out.println(BOLD + "=============================" + RESET);
        viewAllCourses();
        System.out.println();
        int courseId = getIntInput("Enter course ID: ");
        try {
            PreparedStatement profStmt = connection.prepareStatement("SELECT id, name FROM users WHERE role='professor' ORDER BY name");
            ResultSet profRs = profStmt.executeQuery();
            System.out.println(" Available Professors:");
            System.out.printf("%-5s %-30s%n", "ID", "Name");
            System.out.println("--------------------------------");
            boolean profFound = false;
            while (profRs.next()) {
                profFound = true;
                System.out.printf("%-5d %-30s%n",
                        profRs.getInt("id"),
                        profRs.getString("name"));
            }
            if (!profFound) {
                System.out.println("No professors found.");
                pressEnterToContinue();
                return;
            }
            int profId = getIntInput(" Enter professor ID: ");

            PreparedStatement getCourseStmt = connection.prepareStatement("SELECT name, code FROM courses WHERE id = ?");
            getCourseStmt.setInt(1, courseId);
            ResultSet courseRs = getCourseStmt.executeQuery();
            String courseInfo = "Unknown Course";
            if (courseRs.next()) {
                courseInfo = courseRs.getString("code") + " - " + courseRs.getString("name");
            }
            PreparedStatement getProfStmt = connection.prepareStatement("SELECT name FROM users WHERE id = ?");
            getProfStmt.setInt(1, profId);
            ResultSet profInfoRs = getProfStmt.executeQuery();
            String profInfo = "Unknown Professor";
            if (profInfoRs.next()) {
                profInfo = profInfoRs.getString("name");
            }
            if (!getConfirmation("Assign professor " + profInfo + " to course " + courseInfo + "?")) {
                System.out.println(YELLOW + "Assignment cancelled." + RESET);
                pressEnterToContinue();
                return;
            }
            PreparedStatement updateStmt = connection.prepareStatement("UPDATE courses SET professor_id=? WHERE id=?");
            updateStmt.setInt(1, profId);
            updateStmt.setInt(2, courseId);
            int rowsAffected = updateStmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println(GREEN + "✅ Professor assigned successfully!" + RESET);
            } else {
                System.out.println(RED + "❌ Course or Professor not found!" + RESET);
            }
        } catch (SQLException e) {
            System.err.println(RED + "Error assigning professor: " + e.getMessage() + RESET);
        }
        pressEnterToContinue();
    }

    private static void viewAllEnrollments() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "📋 ALL ENROLLMENTS" + RESET);
        System.out.println(BOLD + "=================" + RESET);
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT e.id as enrollment_id, s.name as student, c.name as course, e.grade " +
                            "FROM enrollments e " +
                            "JOIN users s ON e.student_id = s.id " +
                            "JOIN courses c ON e.course_id = c.id " +
                            "ORDER BY s.name, c.name");
            System.out.printf("%-15s %-25s %-25s %-10s%n", "Enrollment ID", "Student", "Course", "Grade");
            System.out.println("------------------------------------------------------------------------");
            while (rs.next()) {
                System.out.printf("%-15d %-25s %-25s %-10s%n",
                        rs.getInt("enrollment_id"),
                        rs.getString("student"),
                        rs.getString("course"),
                        rs.getString("grade") != null ? rs.getString("grade") : "Not graded");
            }
        } catch (SQLException e) {
            System.err.println(RED + "Error retrieving enrollments: " + e.getMessage() + RESET);
        }
        pressEnterToContinue();
    }

    private static void sendNotification() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "📢 SEND NOTIFICATION" + RESET);
        System.out.println(BOLD + "===================" + RESET);
        System.out.println("1. To All Users");
        System.out.println("2. To Specific User");
        int choice = getIntInput(" Select option: ");
        String message = getStringInput("Enter message: ");
        try {
            if (choice == 1) {
                if (!getConfirmation("Send this message to ALL users?")) {
                    System.out.println(YELLOW + "Notification cancelled." + RESET);
                    pressEnterToContinue();
                    return;
                }
                PreparedStatement stmt = connection.prepareStatement("SELECT id, username, name FROM users ORDER BY name");
                ResultSet rs = stmt.executeQuery();
                System.out.println(GREEN + " Sending to all users:" + RESET);
                System.out.printf("%-5s %-20s %-30s%n", "ID", "Username", "Name");
                System.out.println("------------------------------------------------------------");
                int userCount = 0;
                while (rs.next()) {
                    userCount++;
                    System.out.printf("%-5d %-20s %-30s%n",
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("name"));
                }
                System.out.println(GREEN + "Total users: " + userCount + RESET);
                PreparedStatement insertStmt = connection.prepareStatement(
                        "INSERT INTO notifications (user_id, message) SELECT id, ? FROM users");
                insertStmt.setString(1, message);
                int rowsInserted = insertStmt.executeUpdate();
                System.out.println(GREEN + "✅ Notification sent to " + rowsInserted + " users!" + RESET);
            } else if (choice == 2) {
                viewAllUsers();
                System.out.println();
                int userId = getIntInput("Enter user ID: ");
                PreparedStatement insertStmt = connection.prepareStatement(
                        "INSERT INTO notifications (user_id, message) VALUES (?, ?)");
                insertStmt.setInt(1, userId);
                insertStmt.setString(2, message);
                insertStmt.execute();
                System.out.println(GREEN + "✅ Notification sent successfully!" + RESET);
            } else {
                System.out.println(RED + "Invalid option!" + RESET);
            }
        } catch (SQLException e) {
            System.err.println(RED + "Error sending notification: " + e.getMessage() + RESET);
        }
        pressEnterToContinue();
    }

    // Professor functionalities
    private static void viewMyCourses() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "📘 MY COURSES" + RESET);
        System.out.println(BOLD + "=============" + RESET);
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT c.* FROM courses c WHERE c.professor_id = ? ORDER BY c.code");
            stmt.setInt(1, currentUserId);
            ResultSet rs = stmt.executeQuery();
            System.out.printf("%-5s %-10s %-30s %-10s%n", "ID", "Code", "Name", "Credits");
            System.out.println("----------------------------------------");
            while (rs.next()) {
                System.out.printf("%-5d %-10s %-30s %-10d%n",
                        rs.getInt("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getInt("credits"));
            }
        } catch (SQLException e) {
            System.err.println(RED + "Error retrieving courses: " + e.getMessage() + RESET);
        }
        pressEnterToContinue();
    }

    private static void gradeStudents() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "📝 GRADE STUDENTS" + RESET);
        System.out.println(BOLD + "=================" + RESET);
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT e.id as enrollment_id, s.name as student, c.name as course " +
                            "FROM enrollments e " +
                            "JOIN users s ON e.student_id = s.id " +
                            "JOIN courses c ON e.course_id = c.id " +
                            "WHERE c.professor_id = ? ORDER BY s.name, c.name");
            stmt.setInt(1, currentUserId);
            ResultSet rs = stmt.executeQuery();
            System.out.printf("%-15s %-25s %-25s%n", "Enrollment ID", "Student", "Course");
            System.out.println("------------------------------------------------");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-15d %-25s %-25s%n",
                        rs.getInt("enrollment_id"),
                        rs.getString("student"),
                        rs.getString("course"));
            }
            if (!found) {
                System.out.println("No students enrolled in your courses.");
                pressEnterToContinue();
                return;
            }
            int enrollmentId = getIntInput(" Enter enrollment ID to grade: ");
            String grade = getValidatedGradeInput("Enter grade (A-F, e.g., A, B+, C-): ");
            PreparedStatement updateStmt = connection.prepareStatement(
                    "UPDATE enrollments SET grade=? WHERE id=?");
            updateStmt.setString(1, grade);
            updateStmt.setInt(2, enrollmentId);
            int rowsAffected = updateStmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println(GREEN + "✅ Grade submitted successfully!" + RESET);
            } else {
                System.out.println(RED + "❌ Enrollment ID not found or not associated with your course!" + RESET);
            }
        } catch (SQLException e) {
            System.err.println(RED + "Error grading student: " + e.getMessage() + RESET);
        }
        pressEnterToContinue();
    }

    // Professor functionality to take daily attendance - Enhanced with Notification
    private static void takeAttendance() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "📋 TAKE ATTENDANCE" + RESET);
        System.out.println(BOLD + "=================" + RESET);
        System.out.println("Select a course to take attendance for:");
        try {
            PreparedStatement courseStmt = connection.prepareStatement(
                    "SELECT id, code, name FROM courses WHERE professor_id = ? ORDER BY code");
            courseStmt.setInt(1, currentUserId);
            ResultSet courseRs = courseStmt.executeQuery();
            MyArrayList<Integer> courseIds = new MyArrayList<>();
            System.out.printf("%-5s %-10s %-30s%n", "ID", "Code", "Name");
            System.out.println("----------------------------------------");
            boolean courseFound = false;
            while (courseRs.next()) {
                courseFound = true;
                int courseId = courseRs.getInt("id");
                courseIds.add(courseId);
                System.out.printf("%-5d %-10s %-30s%n",
                        courseId,
                        courseRs.getString("code"),
                        courseRs.getString("name"));
            }
            if (!courseFound) {
                System.out.println("You are not assigned to any courses.");
                pressEnterToContinue();
                return;
            }
            int selectedCourseId = getIntInput("Enter Course ID: ");
            if (!courseIds.contains(selectedCourseId)) {
                System.out.println(RED + "Invalid Course ID!" + RESET);
                pressEnterToContinue();
                return;
            }

            PreparedStatement studentStmt = connection.prepareStatement(
                    "SELECT e.student_id, u.name " +
                            "FROM enrollments e " +
                            "JOIN users u ON e.student_id = u.id " +
                            "WHERE e.course_id = ? " +
                            "ORDER BY u.name");
            studentStmt.setInt(1, selectedCourseId);
            ResultSet studentRs = studentStmt.executeQuery();
            MyArrayList<Integer> studentIds = new MyArrayList<>();
            System.out.println("\nStudents enrolled in this course:");
            System.out.printf("%-5s %-30s %-15s%n", "ID", "Name", "Status");
            System.out.println("----------------------------------------");
            boolean studentFound = false;
            while (studentRs.next()) {
                studentFound = true;
                int studentId = studentRs.getInt("student_id");
                studentIds.add(studentId);
                String studentName = studentRs.getString("name");
                PreparedStatement checkStmt = connection.prepareStatement(
                        "SELECT status FROM attendance WHERE student_id = ? AND course_id = ? AND attendance_date = CURDATE()");
                checkStmt.setInt(1, studentId);
                checkStmt.setInt(2, selectedCourseId);
                ResultSet checkRs = checkStmt.executeQuery();
                String currentStatus = "Not Marked";
                if (checkRs.next()) {
                    currentStatus = checkRs.getString("status");
                }
                System.out.printf("%-5d %-30s %-15s%n", studentId, studentName, currentStatus);
            }
            if (!studentFound) {
                System.out.println("No students are enrolled in this course.");
                pressEnterToContinue();
                return;
            }

            System.out.println("\nMark Attendance (P = Present, A = Absent, press Enter to keep existing/Not Marked as Absent):");
            for (int i = 0; i < studentIds.size(); i++) {
                int studentId = studentIds.get(i);
                PreparedStatement checkStmt = connection.prepareStatement(
                        "SELECT status FROM attendance WHERE student_id = ? AND course_id = ? AND attendance_date = CURDATE()");
                checkStmt.setInt(1, studentId);
                checkStmt.setInt(2, selectedCourseId);
                ResultSet checkRs = checkStmt.executeQuery();
                String existingStatus = "";
                if (checkRs.next()) {
                    existingStatus = checkRs.getString("status");
                }
                System.out.print("Student ID " + studentId + " (" + (i+1) + "/" + studentIds.size() + "): ");
                String input = scanner.nextLine().trim().toUpperCase();
                String status = "Absent";
                if (input.equals("P")) {
                    status = "Present";
                } else if (input.equals("A") || input.isEmpty()) {
                    status = "Absent";
                } else {
                    System.out.println(YELLOW + "Invalid input. Marking as Absent." + RESET);
                    status = "Absent";
                }

                // Insert or Update attendance record
                PreparedStatement upsertStmt = connection.prepareStatement(
                        "INSERT INTO attendance (student_id, course_id, attendance_date, status) VALUES (?, ?, CURDATE(), ?) " +
                                "ON DUPLICATE KEY UPDATE status = VALUES(status)");
                upsertStmt.setInt(1, studentId);
                upsertStmt.setInt(2, selectedCourseId);
                upsertStmt.setString(3, status);
                upsertStmt.executeUpdate();

                // >>>>>>>>>> ADD NOTIFICATION LOGIC <<<<<<<<<<
                if ("Absent".equals(status) && (!"Absent".equals(existingStatus))) {
                    PreparedStatement courseNameStmt = connection.prepareStatement("SELECT name, code FROM courses WHERE id = ?");
                    courseNameStmt.setInt(1, selectedCourseId);
                    ResultSet courseNameRs = courseNameStmt.executeQuery();
                    String courseName = "Unknown Course";
                    String courseCode = "";
                    if (courseNameRs.next()) {
                        courseCode = courseNameRs.getString("code");
                        courseName = courseCode + " - " + courseNameRs.getString("name");
                    }

                    String notificationMessage = "You were marked ABSENT for the lecture of " + courseName + " on " + LocalDate.now() + ".";

                    PreparedStatement notifyStmt = connection.prepareStatement(
                            "INSERT INTO notifications (user_id, message) VALUES (?, ?)");
                    notifyStmt.setInt(1, studentId);
                    notifyStmt.setString(2, notificationMessage);
                    notifyStmt.executeUpdate();
                    System.out.println(YELLOW + " -> Notification sent to student ID " + studentId + RESET);
                }
                // >>>>>>>>>> END NOTIFICATION LOGIC <<<<<<<<<<

                System.out.println(GREEN + " -> Marked " + status + RESET);
            }
            System.out.println(GREEN + "\n✅ Attendance marked successfully for course ID " + selectedCourseId + " on " + LocalDate.now() + "!" + RESET);
        } catch (SQLException e) {
            System.err.println(RED + "Error taking attendance: " + e.getMessage() + RESET);
            e.printStackTrace();
        }
        pressEnterToContinue();
    }

    // Student functionalities
    private static void viewAvailableCourses() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "🔍 AVAILABLE COURSES" + RESET);
        System.out.println(BOLD + "===================" + RESET);
        String searchTerm = "";
        System.out.print("Search courses (code/name) or press Enter to show all: ");
        searchTerm = scanner.nextLine().trim();
        try {
            String query = "SELECT c.*, u.name as professor FROM courses c LEFT JOIN users u ON c.professor_id = u.id";
            if (!searchTerm.isEmpty()) {
                query += " WHERE c.code LIKE ? OR c.name LIKE ?";
            }
            query += " ORDER BY c.code";
            PreparedStatement stmt = connection.prepareStatement(query);
            if (!searchTerm.isEmpty()) {
                String searchPattern = "%" + searchTerm + "%";
                stmt.setString(1, searchPattern);
                stmt.setString(2, searchPattern);
            }
            ResultSet rs = stmt.executeQuery();
            System.out.printf("%-5s %-10s %-30s %-10s %-25s%n", "ID", "Code", "Name", "Credits", "Professor");
            System.out.println("------------------------------------------------------------------------");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-5d %-10s %-30s %-10d %-25s%n",
                        rs.getInt("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getInt("credits"),
                        rs.getString("professor") != null ? rs.getString("professor") : "Not assigned");
            }
            if (!found) {
                System.out.println("No courses found matching the search term.");
            }
        } catch (SQLException e) {
            System.err.println(RED + "Error retrieving courses: " + e.getMessage() + RESET);
        }
        pressEnterToContinue();
    }

    private static void enrollInCourse() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "📝 ENROLL IN COURSE" + RESET);
        System.out.println(BOLD + "==================" + RESET);
        viewAvailableCourses();
        System.out.println();
        int courseId = getIntInput("Enter course ID to enroll: ");
        try {
            PreparedStatement checkStmt = connection.prepareStatement(
                    "SELECT * FROM enrollments WHERE student_id = ? AND course_id = ?");
            checkStmt.setInt(1, currentUserId);
            checkStmt.setInt(2, courseId);
            ResultSet checkRs = checkStmt.executeQuery();
            if (checkRs.next()) {
                System.out.println(YELLOW + "⚠️ You are already enrolled in this course!" + RESET);
                pressEnterToContinue();
                return;
            }

            PreparedStatement getCourseStmt = connection.prepareStatement("SELECT name, code FROM courses WHERE id = ?");
            getCourseStmt.setInt(1, courseId);
            ResultSet courseRs = getCourseStmt.executeQuery();
            String courseInfo = "Unknown Course";
            if (courseRs.next()) {
                courseInfo = courseRs.getString("code") + " - " + courseRs.getString("name");
            }
            if (!getConfirmation("Enroll in course " + courseInfo + "?")) {
                System.out.println(YELLOW + "Enrollment cancelled." + RESET);
                pressEnterToContinue();
                return;
            }
            PreparedStatement enrollStmt = connection.prepareStatement(
                    "INSERT INTO enrollments (student_id, course_id) VALUES (?, ?)");
            enrollStmt.setInt(1, currentUserId);
            enrollStmt.setInt(2, courseId);
            enrollStmt.execute();
            System.out.println(GREEN + "✅ Successfully enrolled in course!" + RESET);
        } catch (SQLException e) {
            System.err.println(RED + "Error enrolling in course: " + e.getMessage() + RESET);
        }
        pressEnterToContinue();
    }

    private static void viewMyEnrollments() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "📘 MY ENROLLMENTS" + RESET);
        System.out.println(BOLD + "================" + RESET);
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT c.name as course, c.credits, e.grade " +
                            "FROM enrollments e " +
                            "JOIN courses c ON e.course_id = c.id " +
                            "WHERE e.student_id = ? ORDER BY c.name");
            stmt.setInt(1, currentUserId);
            ResultSet rs = stmt.executeQuery();
            System.out.printf("%-35s %-10s %-10s%n", "Course", "Credits", "Grade");
            System.out.println("----------------------------------------");
            while (rs.next()) {
                System.out.printf("%-35s %-10d %-10s%n",
                        rs.getString("course"),
                        rs.getInt("credits"),
                        rs.getString("grade") != null ? rs.getString("grade") : "Not graded");
            }
        } catch (SQLException e) {
            System.err.println(RED + "Error retrieving enrollments: " + e.getMessage() + RESET);
        }
        pressEnterToContinue();
    }

    // Notification system
    private static void viewNotifications() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "🔔 NOTIFICATIONS" + RESET);
        System.out.println(BOLD + "================" + RESET);
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT * FROM notifications WHERE user_id = ? ORDER BY timestamp DESC LIMIT 20");
            stmt.setInt(1, currentUserId);
            ResultSet rs = stmt.executeQuery();
            MyArrayList<Integer> notificationIds = new MyArrayList<>();
            boolean hasNotifications = false;
            System.out.printf("%-5s %-25s %-50s %-10s%n", "ID", "Timestamp", "Message", "Status");
            System.out.println("------------------------------------------------------------------------");
            while (rs.next()) {
                hasNotifications = true;
                int id = rs.getInt("id");
                notificationIds.add(id);
                String timestamp = rs.getTimestamp("timestamp").toString();
                String message = rs.getString("message");
                String status = rs.getBoolean("is_read") ? "Read" : "NEW";
                System.out.printf("%-5d %-25s %-50s %-10s%n", id, timestamp, message.length() > 50 ? message.substring(0, 47) + "..." : message, status);
            }
            if (!hasNotifications) {
                System.out.println("No notifications found.");
                pressEnterToContinue();
                return;
            }
            System.out.println(" Options:");
            System.out.println("1. Mark specific notification as read");
            System.out.println("2. Mark all notifications as read");
            System.out.println("3. Delete a notification");
            System.out.println("0. Back to menu");
            int choice = getIntInput("Choose an option: ");
            switch (choice) {
                case 1:
                    int notifId = getIntInput("Enter notification ID to mark as read: ");
                    if (notificationIds.contains(notifId)) {
                        PreparedStatement updateStmt = connection.prepareStatement(
                                "UPDATE notifications SET is_read = TRUE WHERE id = ?");
                        updateStmt.setInt(1, notifId);
                        updateStmt.execute();
                        System.out.println(GREEN + "Notification marked as read." + RESET);
                    } else {
                        System.out.println(RED + "Invalid notification ID!" + RESET);
                    }
                    break;
                case 2:
                    PreparedStatement updateAllStmt = connection.prepareStatement(
                            "UPDATE notifications SET is_read = TRUE WHERE user_id = ?");
                    updateAllStmt.setInt(1, currentUserId);
                    updateAllStmt.execute();
                    System.out.println(GREEN + "All notifications marked as read." + RESET);
                    break;
                case 3:
                    int deleteId = getIntInput("Enter notification ID to delete: ");
                    if (notificationIds.contains(deleteId)) {
                        if (getConfirmation("Are you sure you want to delete this notification?")) {
                            PreparedStatement deleteStmt = connection.prepareStatement(
                                    "DELETE FROM notifications WHERE id = ?");
                            deleteStmt.setInt(1, deleteId);
                            deleteStmt.execute();
                            System.out.println(GREEN + "Notification deleted." + RESET);
                        } else {
                            System.out.println(YELLOW + "Deletion cancelled." + RESET);
                        }
                    } else {
                        System.out.println(RED + "Invalid notification ID!" + RESET);
                    }
                    break;
                case 0: break;
                default: System.out.println(RED + "Invalid option!" + RESET);
            }
        } catch (SQLException e) {
            System.err.println(RED + "Error retrieving notifications: " + e.getMessage() + RESET);
        }
        pressEnterToContinue();
    }

    // Messaging system
    private static void sendMessage() { // Renamed from sendNotification for clarity
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "✉️ SEND MESSAGE" + RESET);
        System.out.println(BOLD + "==============" + RESET);
        viewAllUsers();
        System.out.println();
        int receiverId = getIntInput("Enter recipient's user ID: ");
        if (receiverId == currentUserId) {
            System.out.println(YELLOW + "⚠️ You cannot send a message to yourself!" + RESET);
            pressEnterToContinue();
            return;
        }
        try {
            PreparedStatement checkStmt = connection.prepareStatement("SELECT name FROM users WHERE id = ?");
            checkStmt.setInt(1, receiverId);
            ResultSet checkRs = checkStmt.executeQuery();
            if (!checkRs.next()) {
                System.out.println(RED + "❌ Recipient user ID not found!" + RESET);
                pressEnterToContinue();
                return;
            }
            String recipientName = checkRs.getString("name");
            String subject = getStringInput("Subject: ");
            System.out.print("Content (end with a line containing only '.'): ");
            StringBuilder contentBuilder = new StringBuilder();
            String line;
            while (!(line = scanner.nextLine()).equals(".")) {
                contentBuilder.append(line).append(" ");
            }
            String content = contentBuilder.toString().trim();
            if (content.isEmpty()) {
                System.out.println(YELLOW + "⚠️ Message content is empty. Message not sent." + RESET);
                pressEnterToContinue();
                return;
            }

            if (!getConfirmation("Send message to " + recipientName + "?")) {
                System.out.println(YELLOW + "Message sending cancelled." + RESET);
                pressEnterToContinue();
                return;
            }
            PreparedStatement insertStmt = connection.prepareStatement(
                    "INSERT INTO messages (sender_id, receiver_id, subject, content) VALUES (?, ?, ?, ?)");
            insertStmt.setInt(1, currentUserId);
            insertStmt.setInt(2, receiverId);
            insertStmt.setString(3, subject);
            insertStmt.setString(4, content);
            insertStmt.execute();
            System.out.println(GREEN + "✅ Message sent successfully to " + recipientName + "!" + RESET);
        } catch (SQLException e) {
            System.err.println(RED + "Error sending message: " + e.getMessage() + RESET);
        }
        pressEnterToContinue();
    }

    // Export Data Menu - Updated for Professor
    private static void exportDataMenu() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "📤 EXPORT DATA" + RESET);
        System.out.println(BOLD + "=============" + RESET);
        if ("admin".equals(currentUserRole)) {
            System.out.println("1. Export User List");
            System.out.println("2. Export Course List");
            System.out.println("3. Export Enrollments List");
        } else if ("professor".equals(currentUserRole)) {
            System.out.println("1. Export Course List");
            System.out.println("2. Export Enrollments List (My Courses)");
            System.out.println("3. Export Attendance List (My Courses)");
        } else if ("student".equals(currentUserRole)) {
            System.out.println("1. Export My Enrollments List");
        }
        System.out.println("0. Back to menu");
        int choice = getIntInput(" Select option: ");
        switch (currentUserRole) {
            case "admin":
                switch (choice) {
                    case 1: exportUsers(); break;
                    case 2: exportCourses(); break;
                    case 3: exportEnrollments(); break;
                    case 0: return;
                    default: System.out.println(RED + "Invalid option!" + RESET); break;
                }
                break;
            case "professor":
                switch (choice) {
                    case 1: exportCourses(); break;
                    case 2: exportEnrollments(); break;
                    case 3: exportAttendance(); break;
                    case 0: return;
                    default: System.out.println(RED + "Invalid option!" + RESET); break;
                }
                break;
            case "student":
                switch (choice) {
                    case 1: exportEnrollments(); break;
                    case 0: return;
                    default: System.out.println(RED + "Invalid option!" + RESET); break;
                }
                break;
        }
        pressEnterToContinue();
    }

    // Export Attendance to TXT (Professor specific) - Enhanced with Summary
    private static void exportAttendance() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "📤 EXPORT ATTENDANCE" + RESET);
        System.out.println(BOLD + "==================" + RESET);
        if (!"professor".equals(currentUserRole)) {
            System.out.println(RED + "Access denied. Only professors can export attendance." + RESET);
            pressEnterToContinue();
            return;
        }
        try {
            System.out.println("Select a course to export attendance for:");
            PreparedStatement courseStmt = connection.prepareStatement(
                    "SELECT id, code, name FROM courses WHERE professor_id = ? ORDER BY code");
            courseStmt.setInt(1, currentUserId);
            ResultSet courseRs = courseStmt.executeQuery();
            MyArrayList<Integer> courseIds = new MyArrayList<>();
            System.out.printf("%-5s %-10s %-30s%n", "ID", "Code", "Name");
            System.out.println("----------------------------------------");
            boolean courseFound = false;
            while (courseRs.next()) {
                courseFound = true;
                int courseId = courseRs.getInt("id");
                courseIds.add(courseId);
                System.out.printf("%-5d %-10s %-30s%n",
                        courseId,
                        courseRs.getString("code"),
                        courseRs.getString("name"));
            }
            if (!courseFound) {
                System.out.println("You are not assigned to any courses.");
                pressEnterToContinue();
                return;
            }
            int selectedCourseId = getIntInput("Enter Course ID to export attendance: ");
            if (!courseIds.contains(selectedCourseId)) {
                System.out.println(RED + "Invalid Course ID!" + RESET);
                pressEnterToContinue();
                return;
            }

            PreparedStatement attendanceStmt = connection.prepareStatement(
                    "SELECT u.name AS student_name, u.username AS student_username, " +
                            "a.attendance_date, a.status " +
                            "FROM attendance a " +
                            "JOIN users u ON a.student_id = u.id " +
                            "WHERE a.course_id = ? " +
                            "ORDER BY a.attendance_date DESC, u.name ASC");
            attendanceStmt.setInt(1, selectedCourseId);
            ResultSet attendanceRs = attendanceStmt.executeQuery();

            StringBuilder exportData = new StringBuilder();
            PreparedStatement courseHeaderStmt = connection.prepareStatement("SELECT code, name FROM courses WHERE id = ?");
            courseHeaderStmt.setInt(1, selectedCourseId);
            ResultSet courseHeaderRs = courseHeaderStmt.executeQuery();
            String courseCode = "Unknown";
            String courseName = "Unknown Course";
            if (courseHeaderRs.next()) {
                courseCode = courseHeaderRs.getString("code");
                courseName = courseHeaderRs.getString("name");
            }
            exportData.append("Attendance Export for Course: ").append(courseCode).append(" - ").append(courseName).append("\n");
            exportData.append("Exported by: ").append(currentUserRole).append(" (User ID: ").append(currentUserId).append(") on ").append(LocalDate.now()).append("\n");
            exportData.append("=====================================================================================================\n");
            exportData.append(String.format("%-25s %-15s %-12s %-15s%n", "Student Name", "Username", "Date", "Status"));
            exportData.append("-----------------------------------------------------------------------------------------------------\n");
            boolean attendanceFound = false;
            while (attendanceRs.next()) {
                attendanceFound = true;
                exportData.append(String.format("%-25s %-15s %-12s %-15s%n",
                        attendanceRs.getString("student_name"),
                        attendanceRs.getString("student_username"),
                        attendanceRs.getDate("attendance_date").toString(),
                        attendanceRs.getString("status")));
            }
            if (!attendanceFound) {
                exportData.append("No attendance records found for this course.\n");
            } else {
                // >>>>>>>>>> ADD SUMMARY STATISTICS <<<<<<<<<<
                exportData.append("\n--- Attendance Summary per Student ---\n");
                PreparedStatement summaryStmt = connection.prepareStatement(
                        "SELECT u.name AS student_name, u.username AS student_username, " +
                                "COUNT(CASE WHEN a.status = 'Present' THEN 1 END) AS present_count, " +
                                "COUNT(CASE WHEN a.status = 'Absent' THEN 1 END) AS absent_count, " +
                                "COUNT(a.id) AS total_classes, " +
                                "ROUND((COUNT(CASE WHEN a.status = 'Present' THEN 1 END) * 100.0) / COUNT(a.id), 2) AS percentage " +
                                "FROM users u " +
                                "JOIN enrollments e ON u.id = e.student_id " +
                                "LEFT JOIN attendance a ON u.id = a.student_id AND a.course_id = ? " +
                                "WHERE e.course_id = ? " +
                                "GROUP BY u.id, u.name, u.username " +
                                "ORDER BY u.name ASC"
                );
                summaryStmt.setInt(1, selectedCourseId);
                summaryStmt.setInt(2, selectedCourseId);
                ResultSet summaryRs = summaryStmt.executeQuery();
                exportData.append(String.format("%-25s %-15s %-10s %-10s %-15s %-12s%n", "Student Name", "Username", "Present", "Absent", "Total Classes", "Percentage"));
                exportData.append("-----------------------------------------------------------------------------------------------------\n");
                while(summaryRs.next()) {
                    exportData.append(String.format("%-25s %-15s %-10d %-10d %-15d %-11.2f%%%n",
                            summaryRs.getString("student_name"),
                            summaryRs.getString("student_username"),
                            summaryRs.getInt("present_count"),
                            summaryRs.getInt("absent_count"),
                            summaryRs.getInt("total_classes"),
                            summaryRs.getDouble("percentage")
                    ));
                }
                // >>>>>>>>>> END SUMMARY ADDITION <<<<<<<<<<
            }

            String exportPath = "exports/" + currentUserRole + "/";
            createExportDirectory(exportPath);
            String fileName = "attendance_export_course_" + courseCode + "_" + System.currentTimeMillis() + ".txt";
            String fullPath = exportPath + fileName;
            if (writeToFile(fullPath, exportData.toString())) {
                System.out.println(GREEN + "✅ Attendance list exported successfully to: " + fullPath + RESET);
            } else {
                System.out.println(RED + "❌ Failed to export attendance list." + RESET);
            }
        } catch (SQLException e) {
            System.err.println(RED + "Error exporting attendance: " + e.getMessage() + RESET);
            e.printStackTrace();
        }
        pressEnterToContinue();
    }

    // Export Users to TXT
    private static void exportUsers() {
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM users ORDER BY role, name");
            ResultSet rs = stmt.executeQuery();
            StringBuilder exportData = new StringBuilder();
            exportData.append("User List Export\n");
            exportData.append("================\n");
            exportData.append(String.format("%-5s %-20s %-15s %-30s%n", "ID", "Username", "Role", "Name"));
            exportData.append("------------------------------------------------------------\n");
            while (rs.next()) {
                exportData.append(String.format("%-5d %-20s %-15s %-30s%n",
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getString("name")));
            }
            String exportPath = "exports/" + currentUserRole + "/";
            createExportDirectory(exportPath);
            String fileName = "users_export_" + System.currentTimeMillis() + ".txt";
            String fullPath = exportPath + fileName;
            if (writeToFile(fullPath, exportData.toString())) {
                System.out.println(GREEN + "✅ User list exported successfully to: " + fullPath + RESET);
            } else {
                System.out.println(RED + "❌ Failed to export user list." + RESET);
            }
        } catch (SQLException e) {
            System.err.println(RED + "Error exporting users: " + e.getMessage() + RESET);
        }
        pressEnterToContinue();
    }

    // Export Courses to TXT
    private static void exportCourses() {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT c.*, u.name as professor FROM courses c LEFT JOIN users u ON c.professor_id = u.id ORDER BY c.code");
            ResultSet rs = stmt.executeQuery();
            StringBuilder exportData = new StringBuilder();
            exportData.append("Course List Export\n");
            exportData.append("=================\n");
            exportData.append(String.format("%-5s %-10s %-30s %-10s %-25s%n", "ID", "Code", "Name", "Credits", "Professor"));
            exportData.append("------------------------------------------------------------------------\n");
            while (rs.next()) {
                exportData.append(String.format("%-5d %-10s %-30s %-10d %-25s%n",
                        rs.getInt("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getInt("credits"),
                        rs.getString("professor") != null ? rs.getString("professor") : "Not assigned"));
            }
            String exportPath = "exports/" + currentUserRole + "/";
            createExportDirectory(exportPath);
            String fileName = "courses_export_" + System.currentTimeMillis() + ".txt";
            String fullPath = exportPath + fileName;
            if (writeToFile(fullPath, exportData.toString())) {
                System.out.println(GREEN + "✅ Course list exported successfully to: " + fullPath + RESET);
            } else {
                System.out.println(RED + "❌ Failed to export course list." + RESET);
            }
        } catch (SQLException e) {
            System.err.println(RED + "Error exporting courses: " + e.getMessage() + RESET);
        }
        pressEnterToContinue();
    }

    // Export Enrollments to TXT
    private static void exportEnrollments() {
        try {
            String query;
            if ("admin".equals(currentUserRole)) {
                query = "SELECT e.id as enrollment_id, s.name as student, c.name as course, e.grade " +
                        "FROM enrollments e " +
                        "JOIN users s ON e.student_id = s.id " +
                        "JOIN courses c ON e.course_id = c.id " +
                        "ORDER BY s.name, c.name";
            } else if ("professor".equals(currentUserRole)) {
                query = "SELECT e.id as enrollment_id, s.name as student, c.name as course, e.grade " +
                        "FROM enrollments e " +
                        "JOIN users s ON e.student_id = s.id " +
                        "JOIN courses c ON e.course_id = c.id " +
                        "WHERE c.professor_id = ? " +
                        "ORDER BY s.name, c.name";
            } else { // student
                query = "SELECT c.name as course, c.credits, e.grade " +
                        "FROM enrollments e " +
                        "JOIN courses c ON e.course_id = c.id " +
                        "WHERE e.student_id = ? " +
                        "ORDER BY c.name";
            }
            PreparedStatement stmt = connection.prepareStatement(query);
            if (!"admin".equals(currentUserRole)) {
                stmt.setInt(1, currentUserId);
            }
            ResultSet rs = stmt.executeQuery();
            StringBuilder exportData = new StringBuilder();
            exportData.append("Enrollment List Export\n");
            exportData.append("=====================\n");
            if ("admin".equals(currentUserRole) || "professor".equals(currentUserRole)) {
                exportData.append(String.format("%-15s %-25s %-25s %-10s%n", "Enrollment ID", "Student", "Course", "Grade"));
                exportData.append("------------------------------------------------------------------------\n");
            } else {
                exportData.append(String.format("%-35s %-10s %-10s%n", "Course", "Credits", "Grade"));
                exportData.append("----------------------------------------\n");
            }
            boolean found = false;
            while (rs.next()) {
                found = true;
                if ("admin".equals(currentUserRole) || "professor".equals(currentUserRole)) {
                    exportData.append(String.format("%-15d %-25s %-25s %-10s%n",
                            rs.getInt("enrollment_id"),
                            rs.getString("student"),
                            rs.getString("course"),
                            rs.getString("grade") != null ? rs.getString("grade") : "Not graded"));
                } else {
                    exportData.append(String.format("%-35s %-10d %-10s%n",
                            rs.getString("course"),
                            rs.getInt("credits"),
                            rs.getString("grade") != null ? rs.getString("grade") : "Not graded"));
                }
            }
            if (!found) {
                exportData.append("No enrollments found.\n");
            }
            String exportPath = "exports/" + currentUserRole + "/";
            createExportDirectory(exportPath);
            String fileName = "enrollments_export_" + System.currentTimeMillis() + ".txt";
            String fullPath = exportPath + fileName;
            if (writeToFile(fullPath, exportData.toString())) {
                System.out.println(GREEN + "✅ Enrollment list exported successfully to: " + fullPath + RESET);
            } else {
                System.out.println(RED + "❌ Failed to export enrollment list." + RESET);
            }
        } catch (SQLException e) {
            System.err.println(RED + "Error exporting enrollments: " + e.getMessage() + RESET);
        }
        pressEnterToContinue();
    }

    // Chatbot assistant - Enhanced with procedural commands and smart queries
    private static void chatbotAssistant() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "🤖 UNIVERSITY CHATBOT" + RESET);
        System.out.println(BOLD + "====================" + RESET);
        System.out.println("Ask me anything about the university system!");
        System.out.println("Type '" + BOLD + "'exit'" + RESET + "' to return to main menu.");
        System.out.println("Examples: 'Enroll me in CS101', 'Show my grades', 'What courses does Professor Davis teach?', 'Show grade distribution for MATH202'");
        String lastContextCourseCode = null;
        while (true) {
            System.out.print(" You: ");
            String input = scanner.nextLine().toLowerCase().trim();
            if (input.equals("exit")) {
                break;
            }
            System.out.print(BOLD + "Bot: " + RESET);

            // --- Procedural Command Execution ---
            if (input.matches("enroll me in [a-z0-9]+")) {
                if (!"student".equals(currentUserRole)) {
                    System.out.println("Only students can enroll in courses.");
                } else {
                    String courseCode = input.substring(input.lastIndexOf(' ') + 1).toUpperCase();
                    handleEnrollCommand(courseCode);
                    lastContextCourseCode = courseCode;
                }
                continue;
            }

            if (input.contains("list students with grades below") && input.contains("in")) {
                String[] parts = input.split(" ");
                String gradeThreshold = "C";
                String courseCode = null;
                for (int i = 0; i < parts.length; i++) {
                    if ("in".equals(parts[i]) && i + 1 < parts.length) {
                        courseCode = parts[i + 1].toUpperCase();
                        break;
                    }
                }
                if (courseCode != null) {
                    handleListStudentsBelowGradeCommand(courseCode, gradeThreshold);
                    lastContextCourseCode = courseCode;
                } else {
                    System.out.println("Could not identify the course. Please specify like '... in CS101'.");
                }
                continue;
            }

            // 4. What courses does Dr. Smith teach?
            if (input.contains("what courses does") && input.contains("teach")) {
                // Improved parsing: Find text between "what courses does" and "teach"
                int startIndex = input.indexOf("what courses does") + "what courses does".length();
                int endIndex = input.indexOf("teach");
                if (startIndex > -1 && endIndex > startIndex) {
                    String potentialName = input.substring(startIndex, endIndex).trim();
                    if (!potentialName.isEmpty()) {
                        handleCoursesByProfessorCommand(potentialName);
                    } else {
                        System.out.println("Could not identify the professor's name.");
                    }
                } else {
                    System.out.println("Could not parse the command structure correctly.");
                }
                continue; // Important: skip general responses
            }

            if (input.contains("show grade distribution for")) {
                String courseCode = input.substring(input.lastIndexOf(' ') + 1).toUpperCase();
                handleGradeDistributionCommand(courseCode);
                lastContextCourseCode = courseCode;
                continue;
            }

            if (input.contains("assign") && input.contains("to course") && "admin".equals(currentUserRole)) {
                int profStart = input.indexOf("assign") + 7;
                int profEnd = input.indexOf(" to course");
                int courseStart = input.indexOf(" to course") + 11;
                if (profStart > 6 && profEnd > profStart && courseStart > (profEnd + 11)) {
                    String professorName = input.substring(profStart, profEnd).trim();
                    String courseCode = input.substring(courseStart).trim().toUpperCase();
                    handleAssignProfessorCommand(professorName, courseCode);
                    lastContextCourseCode = courseCode;
                } else {
                    System.out.println("Could not parse the command. Please use format: 'Assign Professor Name to course CODE'");
                }
                continue;
            }

            // --- Smart Queries & Information Retrieval ---
            if (input.contains("show") && input.contains("grade")) {
                handleShowMyGradesCommand();
                continue;
            }

            if (input.contains("my") && (input.contains("course") || input.contains("class"))) {
                if ("student".equals(currentUserRole)) {
                    System.out.println("Let me fetch your enrolled courses...");
                    try {
                        PreparedStatement stmt = connection.prepareStatement(
                                "SELECT c.name as course, c.code FROM enrollments e JOIN courses c ON e.course_id = c.id WHERE e.student_id = ? ORDER BY c.name");
                        stmt.setInt(1, currentUserId);
                        ResultSet rs = stmt.executeQuery();
                        System.out.print("Your courses: ");
                        boolean found = false;
                        while (rs.next()) {
                            if (found) System.out.print(", ");
                            System.out.print(rs.getString("code") + " - " + rs.getString("course"));
                            found = true;
                        }
                        if (!found) {
                            System.out.print("You are not enrolled in any courses.");
                        }
                        System.out.println();
                    } catch (SQLException e) {
                        System.out.println("Sorry, I couldn't fetch your courses right now.");
                    }
                } else if ("professor".equals(currentUserRole)) {
                    System.out.println("Let me fetch your courses...");
                    try {
                        PreparedStatement stmt = connection.prepareStatement(
                                "SELECT c.name as course, c.code FROM courses c WHERE c.professor_id = ? ORDER BY c.name");
                        stmt.setInt(1, currentUserId);
                        ResultSet rs = stmt.executeQuery();
                        System.out.print("Your courses: ");
                        boolean found = false;
                        while (rs.next()) {
                            if (found) System.out.print(", ");
                            System.out.print(rs.getString("code") + " - " + rs.getString("course"));
                            found = true;
                        }
                        if (!found) {
                            System.out.print("You are not assigned to any courses.");
                        }
                        System.out.println();
                    } catch (SQLException e) {
                        System.out.println("Sorry, I couldn't fetch your courses right now.");
                    }
                } else {
                    System.out.println("Your role does not have specific courses assigned in this way.");
                }
                continue;
            }

            // --- General/Existing Responses ---
            if (input.contains("hello") || input.contains("hi")) {
                System.out.println("Hello! How can I assist you today?");
            } else if (input.contains("course")) {
                System.out.println("You can view available courses in the course menu. " +
                        "Students can enroll in courses, and professors can manage their courses.");
            } else if (input.contains("notification") || input.contains("message")) {
                System.out.println("You can view your notifications in the notifications section. " +
                        "Administrators can send messages to users.");
            } else if (input.contains("user") || input.contains("account")) {
                System.out.println("Administrators can manage user accounts. " +
                        "Each user has a specific role: admin, professor, or student.");
            } else if (input.contains("help")) {
                System.out.println("Available commands:");
                System.out.println("- Courses: 'View available courses', 'Enroll me in CS101'");
                System.out.println("- Grades: 'Show my grades', 'Show grade distribution for CS101'");
                System.out.println("- Users/Courses: 'What courses does Professor Smith teach?', 'List students with grades below C in CS101'");
                if ("admin".equals(currentUserRole)) {
                    System.out.println("- Admin: 'Assign Professor Name to course CODE'");
                }
                System.out.println("- General: 'My courses', 'Notifications', 'Users', 'Help', 'Exit'");
            } else if (input.contains("export")) {
                System.out.println("You can export data (user lists, course lists, enrollments) from the Export Data menu in your dashboard.");
            } else {
                if (input.contains("enroll")) {
                    System.out.println("Did you mean 'Enroll me in [Course Code]'?");
                } else if (input.contains("grade") || input.contains("score")) {
                    System.out.println("Did you mean 'Show my grades' or 'Show grade distribution for [Course Code]'?");
                } else if (input.contains("professor") && input.contains("course")) {
                    System.out.println("Did you mean 'What courses does [Professor Name] teach?' or 'Assign [Professor Name] to course [Code]' (Admin)?");
                } else {
                    System.out.println("I'm not sure about that. Try asking about courses, grades, notifications, or export. Type 'help' for suggestions.");
                }
            }
        }
    }

    // --- Helper Methods for Chatbot Commands ---
    private static void handleEnrollCommand(String courseCode) {
        try {
            PreparedStatement courseStmt = connection.prepareStatement("SELECT id FROM courses WHERE code = ?");
            courseStmt.setString(1, courseCode);
            ResultSet courseRs = courseStmt.executeQuery();
            if (!courseRs.next()) {
                System.out.println("❌ Course " + courseCode + " not found.");
                return;
            }
            int courseId = courseRs.getInt("id");

            PreparedStatement checkStmt = connection.prepareStatement(
                    "SELECT * FROM enrollments WHERE student_id = ? AND course_id = ?");
            checkStmt.setInt(1, currentUserId);
            checkStmt.setInt(2, courseId);
            ResultSet checkRs = checkStmt.executeQuery();
            if (checkRs.next()) {
                System.out.println("⚠️ You are already enrolled in " + courseCode + ".");
                return;
            }

            PreparedStatement enrollStmt = connection.prepareStatement(
                    "INSERT INTO enrollments (student_id, course_id) VALUES (?, ?)");
            enrollStmt.setInt(1, currentUserId);
            enrollStmt.setInt(2, courseId);
            enrollStmt.execute();
            System.out.println("✅ Successfully enrolled you in " + courseCode + "!");
        } catch (SQLException e) {
            System.err.println(RED + "Error processing enrollment: " + e.getMessage() + RESET);
            System.out.println("Sorry, I couldn't process your enrollment request.");
        }
    }

    private static void handleShowMyGradesCommand() {
        System.out.println("Let me fetch your grades...");
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT c.code, c.name, e.grade " +
                            "FROM enrollments e " +
                            "JOIN courses c ON e.course_id = c.id " +
                            "WHERE e.student_id = ? " +
                            "ORDER BY c.code");
            stmt.setInt(1, currentUserId);
            ResultSet rs = stmt.executeQuery();
            System.out.println("Your Grades:");
            System.out.printf("%-10s %-30s %-10s%n", "Code", "Course Name", "Grade");
            System.out.println("----------------------------------------------");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-10s %-30s %-10s%n",
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("grade") != null ? rs.getString("grade") : "Not graded");
            }
            if (!found) {
                System.out.println("You are not enrolled in any courses or have no grades yet.");
            }
        } catch (SQLException e) {
            System.err.println(RED + "Error fetching grades: " + e.getMessage() + RESET);
            System.out.println("Sorry, I couldn't fetch your grades right now.");
        }
    }

    private static void handleListStudentsBelowGradeCommand(String courseCode, String gradeThreshold) {
        MyArrayList<String> g = new MyArrayList<>();
        MyArrayList<Integer> v = new MyArrayList<>();
        String[] grades = {"F","D-","D","D+","C-","C","C+","B-","B","B+","A-","A","A+"};
        for (int i = 0; i < grades.length; i++) { g.add(grades[i]); v.add(i); }
        int thresholdIndex = gradeIndex(gradeThreshold, g, v);
        if (thresholdIndex == -1) thresholdIndex = gradeIndex("C", g, v);
        try {
            PreparedStatement courseStmt = connection.prepareStatement("SELECT id FROM courses WHERE code = ?");
            courseStmt.setString(1, courseCode);
            ResultSet courseRs = courseStmt.executeQuery();
            if (!courseRs.next()) {
                System.out.println("❌ Course " + courseCode + " not found.");
                return;
            }
            int courseId = courseRs.getInt("id");
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT u.name, e.grade " +
                            "FROM enrollments e " +
                            "JOIN users u ON e.student_id = u.id " +
                            "WHERE e.course_id = ? AND e.grade IS NOT NULL");
            stmt.setInt(1, courseId);
            ResultSet rs = stmt.executeQuery();
            System.out.println("Students with grades below " + gradeThreshold + " in " + courseCode + ":");
            System.out.printf("%-30s %-10s%n", "Student Name", "Grade");
            System.out.println("----------------------------------------");
            boolean found = false;
            while (rs.next()) {
                String studentGrade = rs.getString("grade");
                int studentGradeIndex = gradeIndex(studentGrade, g, v);
                if (studentGradeIndex >= 0 && studentGradeIndex < thresholdIndex) {
                    found = true;
                    System.out.printf("%-30s %-10s%n", rs.getString("name"), studentGrade);
                }
            }
            if (!found) {
                System.out.println("No students found with grades below " + gradeThreshold + " in this course.");
            }
        } catch (SQLException e) {
            System.err.println(RED + "Error fetching student list: " + e.getMessage() + RESET);
            System.out.println("Sorry, I couldn't fetch the student list right now.");
        }
    }

    private static void handleCoursesByProfessorCommand(String professorName) {
        try {
            PreparedStatement profStmt = connection.prepareStatement(
                    "SELECT id, name FROM users WHERE role = 'professor' AND (name LIKE ? OR username LIKE ?)");
            profStmt.setString(1, "%" + professorName + "%");
            profStmt.setString(2, professorName);
            ResultSet profRs = profStmt.executeQuery();
            if (!profRs.next()) {
                System.out.println("❌ Professor '" + professorName + "' not found.");
                return;
            }
            int professorId = profRs.getInt("id");
            String foundProfessorName = profRs.getString("name");
            PreparedStatement courseStmt = connection.prepareStatement(
                    "SELECT code, name FROM courses WHERE professor_id = ? ORDER BY code");
            courseStmt.setInt(1, professorId);
            ResultSet courseRs = courseStmt.executeQuery();
            System.out.println(foundProfessorName + " teaches the following courses:");
            System.out.printf("%-10s %-30s%n", "Code", "Course Name");
            System.out.println("----------------------------------------");
            boolean found = false;
            while (courseRs.next()) {
                found = true;
                System.out.printf("%-10s %-30s%n",
                        courseRs.getString("code"),
                        courseRs.getString("name"));
            }
            if (!found) {
                System.out.println(foundProfessorName + " is not currently assigned to any courses.");
            }
        } catch (SQLException e) {
            System.err.println(RED + "Error fetching professor's courses: " + e.getMessage() + RESET);
            System.out.println("Sorry, I couldn't fetch the courses right now.");
        }
    }

    private static void handleGradeDistributionCommand(String courseCode) {
        try {
            PreparedStatement courseStmt = connection.prepareStatement("SELECT id, name FROM courses WHERE code = ?");
            courseStmt.setString(1, courseCode);
            ResultSet courseRs = courseStmt.executeQuery();
            if (!courseRs.next()) {
                System.out.println("❌ Course " + courseCode + " not found.");
                return;
            }
            int courseId = courseRs.getInt("id");
            String courseName = courseRs.getString("name");
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT grade, COUNT(*) as count FROM enrollments WHERE course_id = ? AND grade IS NOT NULL GROUP BY grade ORDER BY grade");
            stmt.setInt(1, courseId);
            ResultSet rs = stmt.executeQuery();
            System.out.println("Grade Distribution for " + courseCode + " - " + courseName + ":");
            System.out.printf("%-10s %-10s%n", "Grade", "Count");
            System.out.println("--------------------");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-10s %-10d%n",
                        rs.getString("grade"),
                        rs.getInt("count"));
            }
            if (!found) {
                System.out.println("No grades have been recorded for this course yet.");
            }
        } catch (SQLException e) {
            System.err.println(RED + "Error fetching grade distribution: " + e.getMessage() + RESET);
            System.out.println("Sorry, I couldn't fetch the grade distribution right now.");
        }
    }

    private static void handleAssignProfessorCommand(String professorName, String courseCode) {
        try {
            PreparedStatement profStmt = connection.prepareStatement(
                    "SELECT id, name FROM users WHERE role = 'professor' AND (name LIKE ? OR username LIKE ?)");
            profStmt.setString(1, "%" + professorName + "%");
            profStmt.setString(2, professorName);
            ResultSet profRs = profStmt.executeQuery();
            if (!profRs.next()) {
                System.out.println("❌ Professor '" + professorName + "' not found.");
                return;
            }
            int professorId = profRs.getInt("id");
            String foundProfessorName = profRs.getString("name");

            PreparedStatement courseStmt = connection.prepareStatement("SELECT id, name FROM courses WHERE code = ?");
            courseStmt.setString(1, courseCode);
            ResultSet courseRs = courseStmt.executeQuery();
            if (!courseRs.next()) {
                System.out.println("❌ Course " + courseCode + " not found.");
                return;
            }
            int courseId = courseRs.getInt("id");
            String foundCourseName = courseRs.getString("name");

            System.out.println("Assigning " + foundProfessorName + " to " + courseCode + " - " + foundCourseName + "?");
            if (!getConfirmation("Confirm assignment?")) {
                System.out.println("Assignment cancelled.");
                return;
            }

            PreparedStatement updateStmt = connection.prepareStatement("UPDATE courses SET professor_id=? WHERE id=?");
            updateStmt.setInt(1, professorId);
            updateStmt.setInt(2, courseId);
            int rowsAffected = updateStmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println(GREEN + "✅ Professor " + foundProfessorName + " assigned successfully to " + courseCode + "!" + RESET);
            } else {
                System.out.println(RED + "❌ Failed to assign professor. Course or Professor not found!" + RESET);
            }
        } catch (SQLException e) {
            System.err.println(RED + "Error assigning professor: " + e.getMessage() + RESET);
            System.out.println("Sorry, I couldn't process the assignment request.");
        }
    }

    // >>>>>>>>>> NEW STUDENT FUNCTIONALITY: VIEW MY ATTENDANCE <<<<<<<<<<
    private static void viewMyAttendance() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "📅 MY ATTENDANCE RECORDS" + RESET);
        System.out.println(BOLD + "======================" + RESET);
        try {
            System.out.println("Select a course to view attendance for:");
            PreparedStatement courseStmt = connection.prepareStatement(
                    "SELECT c.id, c.code, c.name " +
                            "FROM courses c " +
                            "JOIN enrollments e ON c.id = e.course_id " +
                            "WHERE e.student_id = ? " +
                            "ORDER BY c.code");
            courseStmt.setInt(1, currentUserId);
            ResultSet courseRs = courseStmt.executeQuery();
            MyArrayList<Integer> courseIds = new MyArrayList<>();
            System.out.printf("%-5s %-10s %-30s%n", "ID", "Code", "Name");
            System.out.println("----------------------------------------");
            boolean courseFound = false;
            while (courseRs.next()) {
                courseFound = true;
                int courseId = courseRs.getInt("id");
                courseIds.add(courseId);
                System.out.printf("%-5d %-10s %-30s%n",
                        courseId,
                        courseRs.getString("code"),
                        courseRs.getString("name"));
            }
            if (!courseFound) {
                System.out.println("You are not enrolled in any courses.");
                pressEnterToContinue();
                return;
            }
            int selectedCourseId = getIntInput("Enter Course ID: ");
            if (!courseIds.contains(selectedCourseId)) {
                System.out.println(RED + "Invalid Course ID!" + RESET);
                pressEnterToContinue();
                return;
            }

            PreparedStatement attendanceStmt = connection.prepareStatement(
                    "SELECT attendance_date, status " +
                            "FROM attendance " +
                            "WHERE student_id = ? AND course_id = ? " +
                            "ORDER BY attendance_date DESC");
            attendanceStmt.setInt(1, currentUserId);
            attendanceStmt.setInt(2, selectedCourseId);
            ResultSet attendanceRs = attendanceStmt.executeQuery();

            PreparedStatement courseHeaderStmt = connection.prepareStatement("SELECT code, name FROM courses WHERE id = ?");
            courseHeaderStmt.setInt(1, selectedCourseId);
            ResultSet courseHeaderRs = courseHeaderStmt.executeQuery();
            String courseCode = "Unknown";
            String courseName = "Unknown Course";
            if (courseHeaderRs.next()) {
                courseCode = courseHeaderRs.getString("code");
                courseName = courseHeaderRs.getString("name");
            }

            System.out.println("\nAttendance Records for " + courseCode + " - " + courseName + ":");
            System.out.printf("%-15s %-15s%n", "Date", "Status");
            System.out.println("--------------------------");
            boolean recordsFound = false;
            int presentCount = 0;
            int absentCount = 0;
            while (attendanceRs.next()) {
                recordsFound = true;
                java.sql.Date date = attendanceRs.getDate("attendance_date");
                String status = attendanceRs.getString("status");
                if ("Present".equals(status)) presentCount++;
                else if ("Absent".equals(status)) absentCount++;
                System.out.printf("%-15s %-15s%n", date.toString(), status);
            }
            if (!recordsFound) {
                System.out.println("No attendance records found for this course yet.");
            } else {
                int totalCount = presentCount + absentCount;
                double percentage = (totalCount > 0) ? (double) presentCount / totalCount * 100 : 0;
                System.out.println("--------------------------");
                System.out.printf("Total Classes: %d | Present: %d | Absent: %d | Percentage: %.2f%%\n", totalCount, presentCount, absentCount, percentage);
            }

        } catch (SQLException e) {
            System.err.println(RED + "Error retrieving attendance: " + e.getMessage() + RESET);
            e.printStackTrace();
        }
        pressEnterToContinue();
    }
    // >>>>>>>>>> END NEW STUDENT FUNCTIONALITY <<<<<<<<<<

    // >>>>>>>>>> NEW TIMETABLE FUNCTIONALITY <<<<<<<<<<
    private static void manageTimetable() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "🗓️ MANAGE TIMETABLE" + RESET);
        System.out.println(BOLD + "=================" + RESET);
        System.out.println("1. Add Timetable Entry");
        System.out.println("2. View Timetable");
        System.out.println("3. Delete Timetable Entry");
        int choice = getIntInput("Select option: ");
        switch (choice) {
            case 1: addTimetableEntry(); break;
            case 2: viewTimetable(); break;
            case 3: deleteTimetableEntry(); break;
            default: System.out.println(RED + "Invalid option!" + RESET); pressEnterToContinue();
        }
    }

    private static void addTimetableEntry() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "➕ ADD TIMETABLE ENTRY" + RESET);
        System.out.println(BOLD + "====================" + RESET);

        try {
            // Show all courses to select one
            viewAllCourses(); // Reuse existing method
            System.out.println();
            int courseId = getIntInput("Enter Course ID: ");

            // Validate course exists
            PreparedStatement checkCourseStmt = connection.prepareStatement("SELECT name FROM courses WHERE id = ?");
            checkCourseStmt.setInt(1, courseId);
            ResultSet courseRs = checkCourseStmt.executeQuery();
            if (!courseRs.next()) {
                System.out.println(RED + "❌ Course ID not found!" + RESET);
                pressEnterToContinue();
                return;
            }
            String courseName = courseRs.getString("name");

            System.out.println("Selected Course: " + courseName);

            String dayOfWeek = getValidatedDayInput("Enter Day of Week (e.g., Monday, Tuesday): ");

            // >>>>>>>>>> MODIFIED TIME INPUT WITH VALIDATION <<<<<<<<<<
            String startTimeStr = getValidatedTimeInput("Enter Start Time (HH:MM:SS): ");
            if (startTimeStr == null) {
                pressEnterToContinue();
                return; // User cancelled or invalid input after retries
            }
            String endTimeStr = getValidatedTimeInput("Enter End Time (HH:MM:SS): ");
            if (endTimeStr == null) {
                pressEnterToContinue();
                return; // User cancelled or invalid input after retries
            }
            // >>>>>>>>>> END MODIFIED TIME INPUT <<<<<<<<<<

            String location = getStringInput("Enter Location (optional): ");

            // Confirm addition
            if (!getConfirmation("Add this timetable entry for " + courseName + " on " + dayOfWeek + " from " + startTimeStr + " to " + endTimeStr + " at " + location + "?")) {
                System.out.println(YELLOW + "Entry addition cancelled." + RESET);
                pressEnterToContinue();
                return;
            }

            PreparedStatement insertStmt = connection.prepareStatement(
                    "INSERT INTO timetable (course_id, day_of_week, start_time, end_time, location) VALUES (?, ?, ?, ?, ?)");
            insertStmt.setInt(1, courseId);
            insertStmt.setString(2, dayOfWeek);
            insertStmt.setTime(3, java.sql.Time.valueOf(startTimeStr)); // Should be safe now
            insertStmt.setTime(4, java.sql.Time.valueOf(endTimeStr));   // Should be safe now
            insertStmt.setString(5, location.isEmpty() ? null : location);

            insertStmt.executeUpdate();
            System.out.println(GREEN + "✅ Timetable entry added successfully!" + RESET);

        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // Duplicate entry for unique constraint
                System.out.println(RED + "❌ Error: An entry already exists for this course on the same day and start time!" + RESET);
            } else {
                System.err.println(RED + "Error adding timetable entry: " + e.getMessage() + RESET);
                e.printStackTrace();
            }
        } // Note: IllegalArgumentException from Time.valueOf is now caught by the helper
        pressEnterToContinue();
    }

    // >>>>>>>>>> NEW HELPER METHOD FOR TIME VALIDATION <<<<<<<<<<
    /**
     * Prompts the user for a time input and validates it against HH:MM:SS format.
     * Allows the user to retry up to 3 times.
     * @param prompt The message to display to the user.
     * @return The validated time string in HH:MM:SS format, or null if cancelled/failed.
     */
    private static String getValidatedTimeInput(String prompt) {
        int attempts = 0;
        final int maxAttempts = 3;
        while (attempts < maxAttempts) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                System.out.println(RED + "Input cannot be empty." + RESET);
                attempts++;
                continue;
            }

            // Basic regex check for HH:MM:SS format
            if (input.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$")) {
                return input; // Valid format
            } else {
                System.out.println(RED + "Invalid time format. Please use HH:MM:SS (e.g., 09:30:00, 14:15:30)." + RESET);
                attempts++;
            }
        }
        System.out.println(RED + "Too many invalid attempts. Operation cancelled." + RESET);
        return null; // Indicate failure/cancellation
    }
    // >>>>>>>>>> END NEW HELPER METHOD <<<<<<<<<<

    // (Ensure getValidatedDayInput is also present, as added previously)
    private static String getValidatedDayInput(String prompt) {
        System.out.print(prompt);
        while(true) {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                System.out.print(RED + "Input cannot be empty. " + prompt + RESET);
                continue;
            }
            // Capitalize first letter, lowercase the rest for consistency
            String formattedInput = input.substring(0, 1).toUpperCase() +
                    (input.length() > 1 ? input.substring(1).toLowerCase() : "");

            if ("Monday".equals(formattedInput) || "Tuesday".equals(formattedInput) ||
                    "Wednesday".equals(formattedInput) || "Thursday".equals(formattedInput) ||
                    "Friday".equals(formattedInput) || "Saturday".equals(formattedInput) ||
                    "Sunday".equals(formattedInput)) {
                return formattedInput;
            } else {
                System.out.print(RED + "Invalid day. Please enter a valid day of the week (e.g., Monday). " + prompt + RESET);
            }
        }
    }

    private static void viewTimetable() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "📅 VIEW TIMETABLE" + RESET);
        System.out.println(BOLD + "================" + RESET);

        String role = currentUserRole;
        int userId = currentUserId;

        String query;
        if ("admin".equals(role)) {
            query = "SELECT t.*, c.code, c.name as course_name, u.name as professor_name " +
                    "FROM timetable t " +
                    "JOIN courses c ON t.course_id = c.id " +
                    "LEFT JOIN users u ON c.professor_id = u.id " +
                    "ORDER BY FIELD(t.day_of_week, 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'), t.start_time";
        } else if ("professor".equals(role)) {
            query = "SELECT t.*, c.code, c.name as course_name " +
                    "FROM timetable t " +
                    "JOIN courses c ON t.course_id = c.id " +
                    "WHERE c.professor_id = ? " +
                    "ORDER BY FIELD(t.day_of_week, 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'), t.start_time";
        } else if ("student".equals(role)) {
            query = "SELECT t.*, c.code, c.name as course_name, u.name as professor_name " +
                    "FROM timetable t " +
                    "JOIN courses c ON t.course_id = c.id " +
                    "JOIN enrollments e ON c.id = e.course_id " +
                    "LEFT JOIN users u ON c.professor_id = u.id " +
                    "WHERE e.student_id = ? " +
                    "ORDER BY FIELD(t.day_of_week, 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'), t.start_time";
        } else {
            System.out.println(RED + "❌ Access denied." + RESET);
            pressEnterToContinue();
            return;
        }

        try {
            PreparedStatement stmt = connection.prepareStatement(query);
            if (!"admin".equals(role)) {
                stmt.setInt(1, userId);
            }
            ResultSet rs = stmt.executeQuery();

            String title = "Full Timetable";
            if ("professor".equals(role)) title = "My Courses Timetable";
            else if ("student".equals(role)) title = "My Enrolled Courses Timetable";

            System.out.println(title + ":");
            System.out.printf("%-5s %-12s %-10s %-10s %-20s %-10s %-25s%n", "ID", "Day", "Start", "End", "Location", "Course Code", "Course Name" + ("admin".equals(role) || "student".equals(role) ? " / Professor" : ""));
            System.out.println("----------------------------------------------------------------------------------------------------------");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-5d %-12s %-10s %-10s %-20s %-10s %-25s",
                        rs.getInt("id"),
                        rs.getString("day_of_week"),
                        rs.getTime("start_time").toString(),
                        rs.getTime("end_time").toString(),
                        rs.getString("location") != null ? rs.getString("location") : "N/A",
                        rs.getString("code"),
                        rs.getString("course_name")
                );
                if ("admin".equals(role) || "student".equals(role)) {
                    String professor = rs.getString("professor_name");
                    System.out.print(" / " + (professor != null ? professor : "Not Assigned"));
                }
                System.out.println();
            }
            if (!found) {
                System.out.println("No timetable entries found.");
            }
        } catch (SQLException e) {
            System.err.println(RED + "Error retrieving timetable: " + e.getMessage() + RESET);
            e.printStackTrace();
        }
        pressEnterToContinue();
    }

    private static void deleteTimetableEntry() {
        clearScreen();
        System.out.println(BOLD + SOFTER_BLUE + "🗑️ DELETE TIMETABLE ENTRY" + RESET);
        System.out.println(BOLD + "======================" + RESET);

        viewTimetable();
        System.out.println();

        int entryId = getIntInput("Enter Timetable Entry ID to delete: ");

        try {
            PreparedStatement getStmt = connection.prepareStatement(
                    "SELECT t.day_of_week, t.start_time, c.code, c.name " +
                            "FROM timetable t " +
                            "JOIN courses c ON t.course_id = c.id " +
                            "WHERE t.id = ?");
            getStmt.setInt(1, entryId);
            ResultSet rs = getStmt.executeQuery();
            String entryInfo = "Unknown Entry";
            if (rs.next()) {
                entryInfo = rs.getString("code") + " - " + rs.getString("name") +
                        " on " + rs.getString("day_of_week") + " at " + rs.getTime("start_time");
            }

            if (!getConfirmation("Are you sure you want to delete timetable entry: " + entryInfo + " (ID: " + entryId + ")?")) {
                System.out.println(YELLOW + "Deletion cancelled." + RESET);
                pressEnterToContinue();
                return;
            }

            PreparedStatement deleteStmt = connection.prepareStatement("DELETE FROM timetable WHERE id = ?");
            deleteStmt.setInt(1, entryId);
            int rowsAffected = deleteStmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println(GREEN + "✅ Timetable entry deleted successfully!" + RESET);
            } else {
                System.out.println(RED + "❌ Timetable entry not found!" + RESET);
            }
        } catch (SQLException e) {
            System.err.println(RED + "Error deleting timetable entry: " + e.getMessage() + RESET);
            e.printStackTrace();
        }
        pressEnterToContinue();
    }
    // >>>>>>>>>> END TIMETABLE FUNCTIONALITY <<<<<<<<<<

    // >>>>>>>>>> NEW DYNAMIC FAQ FUNCTIONALITY <<<<<<<<<<
    private static void showAdminFAQ() {
        clearScreen();
        System.out.println(BOLD + SOFTER_MAGENTA + "❓ ADMIN FAQ & GUIDE" + RESET);
        System.out.println(BOLD + "===================" + RESET);
        System.out.println(WHITE + "Welcome to the Administrator Dashboard Guide!" + RESET);
        System.out.println();

        try {
            System.out.println(BOLD + "1. How do I add a new user?" + RESET);
            System.out.println("   - Go to 'Manage Users' -> 'Add User'.");
            System.out.println("   - Enter a unique username, password, select the role (admin/professor/student), and full name.");
            System.out.println("   Example: Add a new student named 'Alice Johnson' with username 'alicej'.");
            System.out.println();

            System.out.println(BOLD + "2. How do I assign a professor to a course?" + RESET);
            System.out.println("   - Go to 'Manage Courses' -> 'Assign Professor'.");
            System.out.println("   - Select the course from the list, then choose the professor from the available list.");
            String exampleCourse = "N/A";
            String exampleProf = "N/A";
            PreparedStatement courseStmt = connection.prepareStatement("SELECT code, name FROM courses LIMIT 1");
            ResultSet courseRs = courseStmt.executeQuery();
            if (courseRs.next()) {
                exampleCourse = courseRs.getString("code") + " - " + courseRs.getString("name");
            }
            PreparedStatement profStmt = connection.prepareStatement("SELECT name FROM users WHERE role='professor' LIMIT 1");
            ResultSet profRs = profStmt.executeQuery();
            if (profRs.next()) {
                exampleProf = profRs.getString("name");
            }
            System.out.println("   Example: Assign professor '" + exampleProf + "' to course '" + exampleCourse + "'.");
            System.out.println();

            System.out.println(BOLD + "3. How do I send a notification?" + RESET);
            System.out.println("   - Go to 'Send Notification'.");
            System.out.println("   - Choose 'To All Users' or 'To Specific User'.");
            System.out.println("   - Enter your message and confirm.");
            System.out.println("   Example: Send 'Welcome to the new semester!' to all users.");
            System.out.println();

            System.out.println(BOLD + "4. What can I do with the Chatbot?" + RESET);
            System.out.println("   - Ask procedural questions or give commands.");
            System.out.println("   - Example commands:");
            System.out.println("     * 'Assign Professor Smith to course CS101' (if Professor Smith and CS101 exist)");
            System.out.println("     * 'Show grade distribution for MATH202' (if MATH202 exists)");
            System.out.println("     * 'List students with grades below C in PHYS101' (if PHYS101 exists)");
            System.out.println();

            System.out.println(BOLD + "5. How do I export data?" + RESET);
            System.out.println("   - Go to 'Export Data'.");
            System.out.println("   - Choose the type of data to export (Users, Courses, Enrollments).");
            System.out.println("   - Files are saved in the 'exports/admin/' directory.");
            System.out.println();

            System.out.println(BOLD + "6. How do I manage the timetable?" + RESET);
            System.out.println("   - Go to 'Manage Timetable'.");
            System.out.println("   - Add entries by selecting a course, day, time, and location.");
            System.out.println("   - View the full timetable.");
            System.out.println("   Example: Add 'CS101 - Intro to CS' on Monday from 09:00 to 11:00 in Room A101.");
            System.out.println();

            // Example 7: Recent Notifications (using MyLinkedList)
            System.out.println(BOLD + "7. How can I see recent system activity (like notifications sent)?" + RESET);
            System.out.println("   - While there isn't a direct menu for this, we can demonstrate how custom data structures");
            System.out.println("     like the built-in 'MyLinkedList' could be used to process such data.");
            System.out.println("   - For example, fetching the last 5 notifications sent by admins:");

            PreparedStatement notifStmt = connection.prepareStatement(
                    "SELECT message, timestamp FROM notifications ORDER BY timestamp DESC LIMIT 5");
            ResultSet notifRs = notifStmt.executeQuery();

            MyLinkedList<String> recentNotifications = new MyLinkedList<>();
            while(notifRs.next()) {
                String msg = notifRs.getString("message");
                Timestamp ts = notifRs.getTimestamp("timestamp");
                recentNotifications.add("[" + ts.toString() + "] " + msg);
            }

            if (recentNotifications.size() > 0) {
                System.out.println("     Last 5 notifications (stored in MyLinkedList):");
                System.out.println("     (Using MyLinkedList to store them...)");
                System.out.println("     Count of recent notifications fetched: " + recentNotifications.size());
                System.out.println("     (Linked list used to collect notifications efficiently.)");
            } else {
                System.out.println("     No recent notifications found.");
            }
            System.out.println();

        } catch (SQLException e) {
            System.out.println(YELLOW + "Note: Some dynamic examples might not be available if the database is empty." + RESET);
            e.printStackTrace();
        }

        System.out.println(BOLD + "General Tips:" + RESET);
        System.out.println(" - Always confirm destructive actions like deleting users or courses.");
        System.out.println(" - Use search functions in 'View All' sections to find specific items quickly.");
        pressEnterToContinue();
    }

    private static void showProfessorFAQ() {
        clearScreen();
        System.out.println(BOLD + BLUE + "❓ PROFESSOR FAQ & GUIDE" + RESET);
        System.out.println(BOLD + "======================" + RESET);
        System.out.println(WHITE + "Welcome to the Professor Dashboard Guide!" + RESET);
        System.out.println();

        System.out.println(BOLD + "1. How do I see my courses?" + RESET);
        System.out.println("   - Go to 'View My Courses'.");
        System.out.println("   - This lists all courses assigned to you.");
        System.out.println();

        System.out.println(BOLD + "2. How do I grade my students?" + RESET);
        System.out.println("   - Go to 'Grade Students'.");
        System.out.println("   - Select the enrollment ID from the list of your students.");
        System.out.println("   - Enter a grade (A-F, with optional + or -).");
        System.out.println("   Example: Grade enrollment ID 5 with 'A-'.");
        System.out.println();

        System.out.println(BOLD + "3. How do I take attendance?" + RESET);
        System.out.println("   - Go to 'Take Attendance'.");
        System.out.println("   - Select one of your courses.");
        System.out.println("   - For each student, enter 'P' for Present or 'A' for Absent (Enter defaults to Absent).");
        System.out.println("   - Students marked 'Absent' will receive an automatic notification.");
        System.out.println();

        System.out.println(BOLD + "4. What can I do with the Chatbot?" + RESET);
        System.out.println("   - Ask questions about your courses or students.");
        System.out.println("   - Example commands:");
        System.out.println("     * 'What courses do I teach?'");
        System.out.println("     * 'Show grade distribution for MY_COURSE_CODE' (replace with your actual course code)");
        System.out.println("     * 'List students with grades below B in MY_COURSE_CODE'");
        System.out.println();

        System.out.println(BOLD + "5. How do I send a message to someone?" + RESET);
        System.out.println("   - Go to 'Send Message'.");
        System.out.println("   - Search for and select the recipient's user ID.");
        System.out.println("   - Enter a subject and content (end content with a line containing only '.').");
        System.out.println();

        System.out.println(BOLD + "6. How do I export data?" + RESET);
        System.out.println("   - Go to 'Export Data'.");
        System.out.println("   - You can export lists of courses, your course enrollments, and attendance records.");
        System.out.println("   - Files are saved in the 'exports/professor/' directory.");
        System.out.println();

        System.out.println(BOLD + "7. How do I see my timetable?" + RESET);
        System.out.println("   - Go to 'View My Timetable'.");
        System.out.println("   - This shows the schedule for the courses you are assigned to.");
        System.out.println();

        System.out.println(BOLD + "General Tips:" + RESET);
        System.out.println(" - Check notifications regularly for system messages.");
        System.out.println(" - Use the Chatbot for quick information retrieval.");
        pressEnterToContinue();
    }

    private static void showStudentFAQ() {
        clearScreen();
        System.out.println(BOLD + GREEN + "❓ STUDENT FAQ & GUIDE" + RESET);
        System.out.println(BOLD + "====================" + RESET);
        System.out.println(WHITE + "Welcome to the Student Dashboard Guide!" + RESET);
        System.out.println();

        System.out.println(BOLD + "1. How do I see courses I can enroll in?" + RESET);
        System.out.println("   - Go to 'View Available Courses'.");
        System.out.println("   - Use the search bar to filter courses by code or name.");
        System.out.println();

        System.out.println(BOLD + "2. How do I enroll in a course?" + RESET);
        System.out.println("   - Go to 'Enroll in Course'.");
        System.out.println("   - Browse or search for the course, note its ID.");
        System.out.println("   - Enter the Course ID and confirm enrollment.");
        System.out.println("   Example: Enroll in a course listed as 'ID: 3, Code: CS101, Name: Intro to Computer Science'.");
        System.out.println();

        System.out.println(BOLD + "3. How do I see my enrolled courses and grades?" + RESET);
        System.out.println("   - Go to 'View My Enrollments'.");
        System.out.println("   - This shows a list of your courses, credits, and current grades.");
        System.out.println();

        System.out.println(BOLD + "4. What can I do with the Chatbot?" + RESET);
        System.out.println("   - Ask questions about your academic status.");
        System.out.println("   - Example commands:");
        System.out.println("     * 'Show my grades'");
        System.out.println("     * 'What courses am I taking?' or 'My courses'");
        System.out.println("     * 'Enroll me in CS101' (if CS101 exists and you are not enrolled)");
        System.out.println();

        System.out.println(BOLD + "5. How do I send a message to someone?" + RESET);
        System.out.println("   - Go to 'Send Message'.");
        System.out.println("   - Search for and select the recipient's user ID (e.g., your professor's ID).");
        System.out.println("   - Enter a subject and content (end content with a line containing only '.').");
        System.out.println();

        System.out.println(BOLD + "6. How do I manage my notifications?" + RESET);
        System.out.println("   - Go to 'View Notifications'.");
        System.out.println("   - You will see a list of messages, including automatic attendance notifications.");
        System.out.println("   - You can mark them as read or delete them.");
        System.out.println("   Example: You might see 'You were marked ABSENT for the lecture of CS101 - Intro to CS on 2023-10-27.'");
        System.out.println();

        System.out.println(BOLD + "7. How do I export my data?" + RESET);
        System.out.println("   - Go to 'Export Data'.");
        System.out.println("   - You can export a list of your current enrollments.");
        System.out.println("   - Files are saved in the 'exports/student/' directory.");
        System.out.println();

        System.out.println(BOLD + "8. How do I check my attendance for a course?" + RESET);
        System.out.println("   - Go to 'View My Attendance'.");
        System.out.println("   - Select the course you want to check attendance for.");
        System.out.println("   - You will see a list of dates and your status (Present/Absent) along with a summary.");
        System.out.println();

        System.out.println(BOLD + "9. How do I see my timetable?" + RESET);
        System.out.println("   - Go to 'View My Timetable'.");
        System.out.println("   - This shows the schedule for the courses you are enrolled in.");
        System.out.println();

        System.out.println(BOLD + "General Tips:" + RESET);
        System.out.println(" - Enroll in courses early to secure your spot.");
        System.out.println(" - Check notifications frequently, especially for attendance alerts.");
        System.out.println(" - Use the Chatbot for quick answers to common questions.");
        pressEnterToContinue();
    }
    // >>>>>>>>>> END DYNAMIC FAQ FUNCTIONALITY <<<<<<<<<<

    // Utility methods
    private static void logout() {
        currentUserId = -1;
        currentUserRole = "";
        System.out.println(GREEN + "👋 Logged out successfully!" + RESET);
        pressEnterToContinue();
    }

    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static void animateLoading() {
        String[] animation = {"⢿", "⣻", "⣽", "⣾", "⣷", "⣯", "⣟", "⡿"};
        for (int i = 0; i < 20; i++) {
            System.out.print("\r" + SOFTER_BLUE + animation[i % animation.length] + " Loading..." + RESET);
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("\r" + GREEN + "✅ System ready!           " + RESET);
    }

    private static void pressEnterToContinue() {
        System.out.println(GREEN + " Press Enter to continue..." + RESET);
        scanner.nextLine();
    }

    private static int gradeIndex(String grade, MyArrayList<String> keys, MyArrayList<Integer> vals) {
        for (int i = 0; i < keys.size(); i++) if (keys.get(i).equals(grade)) return vals.get(i);
        return -1;
    }


    /* =========================================================
   CUSTOM DATA-STRUCTURE IMPLEMENTATIONS
   ========================================================= */
    /* Dynamic array (ArrayList replacement) */
    static class MyArrayList<T> {
        private Object[] arr;
        private int size;
        MyArrayList() { arr = new Object[10]; size = 0; }
        void add(T val) {
            if (size == arr.length) grow();
            arr[size++] = val;
        }
        T get(int idx) { return (T) arr[idx]; }
        int size() { return size; }
        boolean isEmpty() { return size == 0; }
        boolean contains(T val) {
            for (int i = 0; i < size; i++) {
                if (arr[i] != null && arr[i].equals(val)) return true;
            }
            return false;
        }
        private void grow() {
            Object[] bigger = new Object[arr.length * 2];
            for (int i = 0; i < size; i++) bigger[i] = arr[i];
            arr = bigger;
        }
    }
    /* Singly linked list */
    static class MyLinkedList<T> {
        private class Node { T data; Node next; Node(T d) { data = d; } }
        private Node head; private int size;
        void add(T val) {
            Node n = new Node(val);
            if (head == null) { head = n; } else {
                Node cur = head;
                while (cur.next != null) cur = cur.next;
                cur.next = n;
            }
            size++;
        }
        T get(int idx) {
            Node cur = head;
            for (int i = 0; i < idx; i++) cur = cur.next;
            return cur.data;
        }
        int size() { return size; }
        boolean isEmpty() { return size == 0; }
    }
    /* Queue (FIFO) using linked nodes */
    static class MyQueue<T> {
        private class Node { T data; Node next; Node(T d) { data = d; } }
        private Node front, rear; private int size;
        void enqueue(T val) {
            Node n = new Node(val);
            if (rear == null) { front = rear = n; } else { rear.next = n; rear = n; }
            size++;
        }
        T dequeue() {
            if (front == null) return null;
            T val = front.data;
            front = front.next;
            if (front == null) rear = null;
            size--;
            return val;
        }
        T peek() { return front == null ? null : front.data; }
        boolean isEmpty() { return size == 0; }
    }
    /* Stack (LIFO) using linked nodes */
    static class MyStack<T> {
        private class Node { T data; Node next; Node(T d) { data = d; } }
        private Node top; private int size;
        void push(T val) { Node n = new Node(val); n.next = top; top = n; size++; }
        T pop() { if (top == null) return null; T val = top.data; top = top.next; size--; return val; }
        T peek() { return top == null ? null : top.data; }
        boolean isEmpty() { return size == 0; }
    }
    /* tiny helper for index-of */
    private static int indexOf(MyArrayList<Integer> list, int value) {
        for (int i = 0; i < list.size(); i++) if (list.get(i) == value) return i;
        return -1;
    }
}