package com.financeapp.database;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Database {

    private static final String URL = "jdbc:sqlite:financeflow.db";

    public static void initialize() throws SQLException {

        try (Connection connection = DriverManager.getConnection(URL);
             Statement statement = connection.createStatement()) {

            statement.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    description TEXT NOT NULL,
                    amount REAL NOT NULL,
                    type TEXT NOT NULL,
                    category TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            addColumnIfMissing(connection, "amount_decimal", "TEXT");
            addColumnIfMissing(connection, "remarks", "TEXT");

            statement.executeUpdate("""
                UPDATE transactions
                SET amount_decimal = CAST(amount AS TEXT)
                WHERE amount_decimal IS NULL OR amount_decimal = ''
            """);

            statement.executeUpdate("""
                UPDATE transactions
                SET remarks = description
                WHERE remarks IS NULL
            """);
        }
    }

    private static void addColumnIfMissing(
            Connection connection, String column, String type)
            throws SQLException {

        boolean exists = false;

        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "PRAGMA table_info(transactions)")) {

            while (result.next()) {
                if (column.equalsIgnoreCase(result.getString("name"))) {
                    exists = true;
                    break;
                }
            }
        }

        if (!exists) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(
                        "ALTER TABLE transactions ADD COLUMN "
                                + column + " " + type
                );
            }
        }
    }

    public static void addTransaction(
            String description,
            BigDecimal amount,
            String type,
            String category,
            String remarks,
            LocalDateTime timestamp) throws SQLException {

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }

        String sql = """
            INSERT INTO transactions
            (description, amount, amount_decimal, type,
             category, remarks, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection connection = DriverManager.getConnection(URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, description);
            statement.setDouble(2, amount.doubleValue());
            statement.setString(3, amount.toPlainString());
            statement.setString(4, type);
            statement.setString(5, category);
            statement.setString(6, remarks);
            statement.setString(7, timestamp.toString());

            statement.executeUpdate();
        }
    }

    public static List<TransactionData> getAllTransactions()
            throws SQLException {

        List<TransactionData> transactions = new ArrayList<>();

        String sql = """
            SELECT id, description, amount_decimal, type,
                   category, remarks, created_at
            FROM transactions
            ORDER BY created_at DESC, id DESC
        """;

        try (Connection connection = DriverManager.getConnection(URL);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {

            while (result.next()) {
                transactions.add(readTransaction(result));
            }
        }

        return transactions;
    }

    public static List<TransactionData> filterTransactions(
            LocalDateTime from,
            LocalDateTime to,
            String category) throws SQLException {

        List<TransactionData> transactions = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
            SELECT id, description, amount_decimal, type,
                   category, remarks, created_at
            FROM transactions WHERE 1=1
        """);

        if (from != null) {
            sql.append(" AND created_at >= ?");
        }

        if (to != null) {
            sql.append(" AND created_at <= ?");
        }

        if (category != null && !category.equalsIgnoreCase("All")) {
            sql.append(" AND category = ?");
        }

        sql.append(" ORDER BY created_at DESC, id DESC");

        try (Connection connection = DriverManager.getConnection(URL);
             PreparedStatement statement =
                     connection.prepareStatement(sql.toString())) {

            int index = 1;

            if (from != null) {
                statement.setString( index++, from.toString());
            }

            if (to != null) {
                statement.setString(index++, to.toString());
            }

            if (category != null && !category.equalsIgnoreCase("All")) {
                statement.setString(index, category);
            }

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    transactions.add(readTransaction(result));
                }
            }
        }

        return transactions;
    }

    private static TransactionData readTransaction(ResultSet result)
            throws SQLException {

        String amount = result.getString("amount_decimal");

        if (amount == null || amount.isBlank()) {
            amount = "0";
        }

        return new TransactionData(
                result.getInt("id"),
                result.getString("description"),
                new BigDecimal(amount),
                result.getString("type"),
                result.getString("category"),
                result.getString("remarks"),
                LocalDateTime.parse(
                        result.getString("created_at").replace(" ", "T")
                )
        );
    }

    public static void deleteTransaction(int id) throws SQLException {

        try (Connection connection = DriverManager.getConnection(URL);
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM transactions WHERE id = ?")) {

            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public record TransactionData(
            int id,
            String description,
            BigDecimal amount,
            String type,
            String category,
            String remarks,
            LocalDateTime timestamp
    ) {}
}