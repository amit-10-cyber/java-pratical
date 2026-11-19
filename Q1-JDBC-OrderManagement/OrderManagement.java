import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class OrderManagement {

    private static final String URL = "jdbc:mysql://localhost:3306/ecommerce_db";
    private static final String USER = "root";
    private static final String PASSWORD = "Amit@mysql2026";
    private static final List<String> VALID_STATUS = Arrays.asList("Pending", "Shipped", "Delivered");

    private static Connection con;
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connected to ecommerce_db successfully.");
            createTableIfNotExists();

            int choice;
            do {
                System.out.println("\n===== ORDER MANAGEMENT SYSTEM =====");
                System.out.println("1. Insert sample orders");
                System.out.println("2. Show orders by status (quantity > 2, price DESC)");
                System.out.println("3. Increase order quantity");
                System.out.println("4. Update order status");
                System.out.println("5. Total order value / Delete cancelled order");
                System.out.println("6. Display all orders");
                System.out.println("0. Exit");
                System.out.print("Enter choice: ");
                choice = Integer.parseInt(sc.nextLine().trim());

                switch (choice) {
                    case 1: insertOrders(); break;
                    case 2: showOrdersByStatus(); break;
                    case 3: increaseQuantity(); break;
                    case 4: updateStatus(); break;
                    case 5: totalValueAndDelete(); break;
                    case 6: displayAll(); break;
                    case 0: System.out.println("Goodbye!"); break;
                    default: System.out.println("Invalid choice.");
                }
            } while (choice != 0);

            con.close();
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        }
    }

    private static void createTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS Orders ("
                + "order_id INT PRIMARY KEY, "
                + "customer_name VARCHAR(100), "
                + "product_name VARCHAR(100), "
                + "quantity INT, "
                + "unit_price DOUBLE, "
                + "status VARCHAR(30))";
        try (Statement st = con.createStatement()) {
            st.executeUpdate(sql);
        }
    }

    // a) Insert records using PreparedStatement, handle duplicate order IDs
    private static void insertOrders() {
        Object[][] data = {
            {101, "Ravi", "Laptop", 3, 55000.0, "Pending"},
            {102, "Anita", "Mouse", 5, 500.0, "Shipped"},
            {103, "Karan", "Keyboard", 2, 1200.0, "Cancelled"}
        };
        String sql = "INSERT INTO Orders (order_id, customer_name, product_name, quantity, unit_price, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (Object[] row : data) {
                try {
                    ps.setInt(1, (Integer) row[0]);
                    ps.setString(2, (String) row[1]);
                    ps.setString(3, (String) row[2]);
                    ps.setInt(4, (Integer) row[3]);
                    ps.setDouble(5, (Double) row[4]);
                    ps.setString(6, (String) row[5]);
                    ps.executeUpdate();
                    System.out.println("Order " + row[0] + " inserted successfully.");
                } catch (SQLIntegrityConstraintViolationException e) {
                    System.out.println("Order ID " + row[0] + " already exists. Skipping duplicate.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Insert failed: " + e.getMessage());
        }
    }

    // b) Orders by status with quantity > 2, sorted by unit_price DESC
    private static void showOrdersByStatus() {
        System.out.print("Enter status: ");
        String status = sc.nextLine().trim();
        String sql = "SELECT * FROM Orders WHERE status = ? AND quantity > 2 ORDER BY unit_price DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                boolean found = false;
                printHeader();
                while (rs.next()) {
                    found = true;
                    printRow(rs);
                }
                if (!found) {
                    System.out.println("No orders found with status '" + status + "' and quantity > 2.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Query failed: " + e.getMessage());
        }
    }

    // c) Increase quantity of an existing order
    private static void increaseQuantity() {
        try {
            System.out.print("Enter order ID: ");
            int id = Integer.parseInt(sc.nextLine().trim());
            System.out.print("Enter additional quantity: ");
            int extra = Integer.parseInt(sc.nextLine().trim());

            String sql = "UPDATE Orders SET quantity = quantity + ? WHERE order_id = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, extra);
                ps.setInt(2, id);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    System.out.println("Quantity increased by " + extra + " for order " + id + ".");
                } else {
                    System.out.println("Order with ID " + id + " does not exist.");
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid number entered.");
        } catch (SQLException e) {
            System.out.println("Update failed: " + e.getMessage());
        }
    }

    // d) Update status if order exists and new status is valid
    private static void updateStatus() {
        try {
            System.out.print("Enter order ID: ");
            int id = Integer.parseInt(sc.nextLine().trim());
            System.out.print("Enter new status (Pending/Shipped/Delivered): ");
            String newStatus = sc.nextLine().trim();

            String select = "SELECT * FROM Orders WHERE order_id = ?";
            try (PreparedStatement ps = con.prepareStatement(select)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Order with ID " + id + " does not exist.");
                        return;
                    }
                }
            }

            String valid = null;
            for (String s : VALID_STATUS) {
                if (s.equalsIgnoreCase(newStatus)) {
                    valid = s;
                }
            }
            if (valid == null) {
                System.out.println("Invalid status. Allowed: " + VALID_STATUS);
                return;
            }

            String update = "UPDATE Orders SET status = ? WHERE order_id = ?";
            try (PreparedStatement ps = con.prepareStatement(update)) {
                ps.setString(1, valid);
                ps.setInt(2, id);
                ps.executeUpdate();
            }

            System.out.println("Status updated. Updated order details:");
            try (PreparedStatement ps = con.prepareStatement(select)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    printHeader();
                    while (rs.next()) {
                        printRow(rs);
                    }
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid order ID.");
        } catch (SQLException e) {
            System.out.println("Update failed: " + e.getMessage());
        }
    }

    // e) Total order value and delete only Cancelled orders
    private static void totalValueAndDelete() {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT SUM(quantity * unit_price) AS total FROM Orders")) {
            if (rs.next()) {
                System.out.println("Total order value: Rs. " + rs.getDouble("total"));
            }
        } catch (SQLException e) {
            System.out.println("Query failed: " + e.getMessage());
        }

        try {
            System.out.print("Enter order ID to delete: ");
            int id = Integer.parseInt(sc.nextLine().trim());

            String select = "SELECT status FROM Orders WHERE order_id = ?";
            try (PreparedStatement ps = con.prepareStatement(select)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Order with ID " + id + " does not exist.");
                        return;
                    }
                    String status = rs.getString("status");
                    if (!"Cancelled".equalsIgnoreCase(status)) {
                        System.out.println("Cannot delete order " + id + ". Status is '" + status
                                + "', only Cancelled orders can be deleted.");
                        return;
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM Orders WHERE order_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                System.out.println("Cancelled order " + id + " deleted successfully.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid order ID.");
        } catch (SQLException e) {
            System.out.println("Delete failed: " + e.getMessage());
        }
    }

    private static void displayAll() {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM Orders ORDER BY order_id")) {
            printHeader();
            while (rs.next()) {
                printRow(rs);
            }
        } catch (SQLException e) {
            System.out.println("Query failed: " + e.getMessage());
        }
    }

    private static void printHeader() {
        System.out.printf("%-5s %-15s %-16s %-4s %-10s %-10s%n",
                "ID", "Customer", "Product", "Qty", "Price", "Status");
        System.out.println("---------------------------------------------------------------");
    }

    private static void printRow(ResultSet rs) throws SQLException {
        System.out.printf("%-5d %-15s %-16s %-4d %-10.2f %-10s%n",
                rs.getInt("order_id"), rs.getString("customer_name"), rs.getString("product_name"),
                rs.getInt("quantity"), rs.getDouble("unit_price"), rs.getString("status"));
    }
}
