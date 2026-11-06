# Personal Finance Tracker

A JavaFX desktop application for managing personal income and expenses.

## Features
- Record income and expenses.
- Store transaction date and time, category, description, and remarks.
- View monthly income, expenses, and net cash flow.
- Filter transactions by date range and category.
- View category-wise expense percentages.
- Manage category budgets.
- Export financial reports to CSV and text files.
- Store transaction data using SQLite.

## Technology Stack
- Java 17
- JavaFX 21
- SQLite
- Maven
- BigDecimal for financial calculations
- java.time for date and time handling

## Financial Calculation Approach

All financial calculations use Java BigDecimal to reduce floating-point precision errors.

- Total Income = Sum of all income transactions.
- Total Expenses = Sum of all expense transactions.
- Net Cash Flow = Total Income - Total Expenses.
- Category Spending Percentage = (Category Expenses / Total Expenses) * 100.

Calculations are rounded to two decimal places for display.

## How to Run

1. Install JDK 17 or later.
2. Open the project in IntelliJ IDEA.
3. Allow Maven to load dependencies.
4. Open the Maven tool window.
5. Navigate to Plugins > javafx > javafx:run.

## Database

SQLite stores transaction records locally in the project database.

## Project Structure

- src/main/java/com/financeapp/MainApp.java
- src/main/java/com/financeapp/database/Database.java
- src/main/java/com/financeapp/ui/DashboardView.java
- pom.xml
- financeflow.db

## Sample Files

The repository includes sample transaction and financial summary CSV files.

## Author
Y Mahalakshmi
