package com.financeapp.ui;

import com.financeapp.database.Database;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardView {

    private final String BLUE = "#172554";
    private final String LIGHT_BLUE = "#2563EB";
    private final String BACKGROUND = "#F1F5F9";
    private final String WHITE = "#FFFFFF";
    private final String GREEN = "#16A34A";
    private final String RED = "#DC2626";

    private BorderPane root;
    private VBox contentArea;

    private final ObservableList<Transaction> transactions =
            FXCollections.observableArrayList();

    private final Map<String, BigDecimal> budgets = new HashMap<>();

    private Label incomeValue;
    private Label expenseValue;
    private Label balanceValue;

    private TableView<Transaction> transactionTable;

    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private ComboBox<String> filterCategory;

    private final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    public Parent createView() {
        try {
            Database.initialize();
            loadTransactions();
        } catch (Exception e) {
            showError("Database Error", e.getMessage());
        }

        root = new BorderPane();
        root.setStyle("-fx-background-color: " + BACKGROUND + ";");

        root.setLeft(createSidebar());

        contentArea = new VBox(20);
        contentArea.setPadding(new Insets(30));
        contentArea.setStyle("-fx-background-color: " + BACKGROUND + ";");

        root.setCenter(contentArea);

        showDashboard();

        return root;
    }

    // SIDEBAR

    private VBox createSidebar() {
        VBox sidebar = new VBox(20);
        sidebar.setPadding(new Insets(30, 18, 30, 18));
        sidebar.setPrefWidth(230);
        sidebar.setStyle("-fx-background-color: " + BLUE + ";");

        Label logo = new Label("FinanceFlow");
        logo.setFont(Font.font("Arial", FontWeight.BOLD, 25));
        logo.setTextFill(Color.WHITE);

        Label subtitle = new Label("PERSONAL FINANCE");
        subtitle.setTextFill(Color.web("#94A3B8"));
        subtitle.setFont(Font.font("Arial", 10));

        VBox logoBox = new VBox(5, logo, subtitle);

        Button dashboardButton = createMenuButton("Dashboard");
        Button transactionsButton = createMenuButton("Transactions");
        Button budgetsButton = createMenuButton("Budgets");
        Button reportsButton = createMenuButton("Reports");

        dashboardButton.setOnAction(e -> showDashboard());
        transactionsButton.setOnAction(e -> showTransactions());
        budgetsButton.setOnAction(e -> showBudgets());
        reportsButton.setOnAction(e -> showReports());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label footer = new Label("Manage your money smarter.");
        footer.setWrapText(true);
        footer.setTextFill(Color.web("#94A3B8"));

        sidebar.getChildren().addAll(
                logoBox,
                new Separator(),
                dashboardButton,
                transactionsButton,
                budgetsButton,
                reportsButton,
                spacer,
                footer
        );

        return sidebar;
    }

    private Button createMenuButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setPadding(new Insets(13, 15, 13, 15));
        button.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-background-radius: 8;
            -fx-cursor: hand;
            """);

        button.setOnMouseEntered(e ->
                button.setStyle("""
                    -fx-background-color: #334155;
                    -fx-text-fill: white;
                    -fx-font-size: 14px;
                    -fx-background-radius: 8;
                    -fx-cursor: hand;
                    """));

        button.setOnMouseExited(e ->
                button.setStyle("""
                    -fx-background-color: transparent;
                    -fx-text-fill: white;
                    -fx-font-size: 14px;
                    -fx-background-radius: 8;
                    -fx-cursor: hand;
                    """));

        return button;
    }

    // DASHBOARD

    private void showDashboard() {
        contentArea.getChildren().clear();

        Label heading = createHeading("Dashboard");
        Label description = createSubtitle(
                "Your financial overview for the current month."
        );

        HBox summaryCards = new HBox(15);

        incomeValue = new Label("₹0.00");
        expenseValue = new Label("₹0.00");
        balanceValue = new Label("₹0.00");

        summaryCards.getChildren().addAll(
                createSummaryCard("Monthly Income", incomeValue, GREEN),
                createSummaryCard("Monthly Expenses", expenseValue, RED),
                createSummaryCard("Net Cash Flow", balanceValue, LIGHT_BLUE)
        );

        updateMonthlySummary();

        VBox recentBox = createPanel();

        Label recentHeading = new Label("Recent Transactions");
        recentHeading.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        TableView<Transaction> recentTable = createTransactionTable();
        recentTable.setItems(FXCollections.observableArrayList(
                transactions.stream().limit(5).collect(Collectors.toList())
        ));

        recentBox.getChildren().addAll(recentHeading, recentTable);

        contentArea.getChildren().addAll(
                heading,
                description,
                summaryCards,
                recentBox
        );
    }

    private VBox createSummaryCard(
            String title, Label value, String color) {

        VBox card = createPanel();
        card.setPrefWidth(280);
        card.setMinHeight(125);

        Label titleLabel = new Label(title);
        titleLabel.setTextFill(Color.web("#64748B"));
        titleLabel.setFont(Font.font("Arial", 13));

        value.setFont(Font.font("Arial", FontWeight.BOLD, 25));
        value.setTextFill(Color.web(color));

        card.getChildren().addAll(titleLabel, value);

        HBox.setHgrow(card, Priority.ALWAYS);

        return card;
    }

    private void updateMonthlySummary() {
        YearMonth currentMonth = YearMonth.now();

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;

        for (Transaction transaction : transactions) {
            if (YearMonth.from(transaction.getTimestamp())
                    .equals(currentMonth)) {

                if (transaction.getType().equalsIgnoreCase("Income")) {
                    income = income.add(transaction.getAmount());
                } else {
                    expense = expense.add(transaction.getAmount());
                }
            }
        }

        BigDecimal balance = income.subtract(expense);

        if (incomeValue != null) {
            incomeValue.setText(formatMoney(income));
            expenseValue.setText(formatMoney(expense));
            balanceValue.setText(formatMoney(balance));
        }
    }

    // TRANSACTIONS

    private void showTransactions() {
        contentArea.getChildren().clear();

        Label heading = createHeading("Transactions");
        Label description = createSubtitle(
                "Add, view, filter and manage your income and expenses."
        );

        VBox form = createPanel();

        Label formHeading = new Label("Add New Transaction");
        formHeading.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        TextField descriptionField =
                new TextField();
        descriptionField.setPromptText("Transaction description");

        TextField amountField =
                new TextField();
        amountField.setPromptText("Amount (e.g. 1500.00)");

        ComboBox<String> typeBox =
                new ComboBox<>(FXCollections.observableArrayList(
                        "Income", "Expense"
                ));
        typeBox.setPromptText("Select type");
        typeBox.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> categoryBox =
                new ComboBox<>(FXCollections.observableArrayList(
                        "Salary", "Business", "Food", "Travel",
                        "Shopping", "Bills", "Education",
                        "Health", "Entertainment", "Other"
                ));
        categoryBox.setPromptText("Select category");
        categoryBox.setMaxWidth(Double.MAX_VALUE);

        TextField remarksField =
                new TextField();
        remarksField.setPromptText("Remarks (optional)");

        Button addButton = new Button("Add Transaction");
        addButton.setStyle("""
            -fx-background-color: #2563EB;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-padding: 11 20;
            -fx-background-radius: 7;
            -fx-cursor: hand;
            """);

        addButton.setOnAction(e -> {
            saveTransaction(
                    descriptionField.getText(),
                    amountField.getText(),
                    typeBox.getValue(),
                    categoryBox.getValue(),
                    remarksField.getText()
            );

            descriptionField.clear();
            amountField.clear();
            remarksField.clear();
            typeBox.setValue(null);
            categoryBox.setValue(null);
        });

        GridPane formGrid = new GridPane();
        formGrid.setHgap(12);
        formGrid.setVgap(12);

        formGrid.add(descriptionField, 0, 0);
        formGrid.add(amountField, 1, 0);
        formGrid.add(typeBox, 0, 1);
        formGrid.add(categoryBox, 1, 1);
        formGrid.add(remarksField, 0, 2, 2, 1);

        ColumnConstraints column1 = new ColumnConstraints();
        column1.setPercentWidth(50);

        ColumnConstraints column2 = new ColumnConstraints();
        column2.setPercentWidth(50);

        formGrid.getColumnConstraints().addAll(column1, column2);

        form.getChildren().addAll(
                formHeading,
                formGrid,
                addButton
        );

        // FILTERS

        VBox filterPanel = createPanel();

        Label filterHeading = new Label("Filter Transactions");
        filterHeading.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        fromDatePicker = new DatePicker();
        fromDatePicker.setPromptText("From date");

        toDatePicker = new DatePicker();
        toDatePicker.setPromptText("To date");

        filterCategory = new ComboBox<>(
                FXCollections.observableArrayList(
                        "All", "Salary", "Business", "Food",
                        "Travel", "Shopping", "Bills", "Education",
                        "Health", "Entertainment", "Other"
                )
        );

        filterCategory.setValue("All");

        Button filterButton = new Button("Apply Filters");
        Button clearButton = new Button("Clear Filters");

        filterButton.setOnAction(e -> applyTransactionFilters());

        clearButton.setOnAction(e -> {
            fromDatePicker.setValue(null);
            toDatePicker.setValue(null);
            filterCategory.setValue("All");
            applyTransactionFilters();
        });

        HBox filterControls = new HBox(
                10,
                fromDatePicker,
                toDatePicker,
                filterCategory,
                filterButton,
                clearButton
        );

        filterControls.setAlignment(Pos.CENTER_LEFT);

        filterPanel.getChildren().addAll(
                filterHeading,
                filterControls
        );

        // TRANSACTION TABLE

        VBox tablePanel = createPanel();

        Label tableHeading = new Label("Transaction History");
        tableHeading.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        transactionTable = createTransactionTable();
        transactionTable.setItems(transactions);

        Button deleteButton = new Button("Delete Selected");
        deleteButton.setStyle("""
            -fx-background-color: #DC2626;
            -fx-text-fill: white;
            -fx-padding: 9 16;
            -fx-background-radius: 6;
            """);

        deleteButton.setOnAction(e -> deleteSelectedTransaction());

        tablePanel.getChildren().addAll(
                tableHeading,
                transactionTable,
                deleteButton
        );

        VBox.setVgrow(tablePanel, Priority.ALWAYS);

        contentArea.getChildren().addAll(
                heading,
                description,
                form,
                filterPanel,
                tablePanel
        );
    }

    private void saveTransaction(
            String description,
            String amountText,
            String type,
            String category,
            String remarks) {

        if (description == null || description.isBlank()
                || amountText == null || amountText.isBlank()
                || type == null || category == null) {

            showError(
                    "Missing Information",
                    "Please enter a description, amount, type and category."
            );
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(amountText.trim())
                    .setScale(2, RoundingMode.UNNECESSARY);

            if (amount.signum() <= 0) {
                showError("Invalid Amount", "Amount must be greater than zero.");
                return;
            }

            Database.addTransaction(
                    description.trim(),
                    amount,
                    type,
                    category,
                    remarks == null ? "" : remarks.trim(),
                    LocalDateTime.now()
            );

            loadTransactions();
            showTransactions();

            showInformation(
                    "Success",
                    "Transaction added successfully."
            );

        } catch (NumberFormatException | ArithmeticException ex) {
            showError(
                    "Invalid Amount",
                    "Enter a valid amount with up to 2 decimal places."
            );
        } catch (Exception ex) {
            showError("Database Error", ex.getMessage());
        }
    }

    private void loadTransactions() throws Exception {
        transactions.clear();

        for (Database.TransactionData data :
                Database.getAllTransactions()) {

            transactions.add(new Transaction(
                    data.id(),
                    data.description(),
                    data.amount(),
                    data.type(),
                    data.category(),
                    data.remarks(),
                    data.timestamp()
            ));
        }

        FXCollections.sort(
                transactions,
                Comparator.comparing(Transaction::getTimestamp).reversed()
        );

        updateMonthlySummary();
    }

    private void applyTransactionFilters() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();
        String category = filterCategory.getValue();

        if (from != null && to != null && from.isAfter(to)) {
            showError(
                    "Invalid Date Range",
                    "The start date cannot be after the end date."
            );
            return;
        }

        ObservableList<Transaction> filtered =
                FXCollections.observableArrayList();

        for (Transaction transaction : transactions) {

            LocalDate transactionDate =
                    transaction.getTimestamp().toLocalDate();

            boolean matchesFrom =
                    from == null || !transactionDate.isBefore(from);

            boolean matchesTo =
                    to == null || !transactionDate.isAfter(to);

            boolean matchesCategory =
                    category == null
                            || category.equals("All")
                            || transaction.getCategory().equals(category);

            if (matchesFrom && matchesTo && matchesCategory) {
                filtered.add(transaction);
            }
        }

        transactionTable.setItems(filtered);
    }

    private void deleteSelectedTransaction() {
        Transaction selected =
                transactionTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showError(
                    "No Selection",
                    "Please select a transaction to delete."
            );
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Transaction");
        confirmation.setHeaderText("Delete this transaction?");
        confirmation.setContentText(selected.getDescription());

        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isPresent()
                && result.get() == ButtonType.OK) {

            try {
                Database.deleteTransaction(selected.getId());
                loadTransactions();
                showTransactions();

            } catch (Exception ex) {
                showError("Delete Error", ex.getMessage());
            }
        }
    }
    // BUDGETS

    private void showBudgets() {
        contentArea.getChildren().clear();

        Label heading = createHeading("Budgets");
        Label description = createSubtitle(
                "Set monthly spending limits and monitor your expenses."
        );

        VBox budgetForm = createPanel();

        Label formHeading = new Label("Set Category Budget");
        formHeading.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        ComboBox<String> categoryBox =
                new ComboBox<>(FXCollections.observableArrayList(
                        "Food", "Travel", "Shopping", "Bills",
                        "Education", "Health", "Entertainment", "Other"
                ));

        categoryBox.setPromptText("Select category");
        categoryBox.setMaxWidth(Double.MAX_VALUE);

        TextField amountField = new TextField();
        amountField.setPromptText("Monthly budget amount");

        Button saveButton = new Button("Save Budget");
        saveButton.setStyle("""
            -fx-background-color: #2563EB;
            -fx-text-fill: white;
            -fx-padding: 10 18;
            -fx-background-radius: 7;
            """);

        saveButton.setOnAction(e -> {
            if (categoryBox.getValue() == null
                    || amountField.getText().isBlank()) {
                showError(
                        "Missing Information",
                        "Select a category and enter a budget amount."
                );
                return;
            }

            try {
                BigDecimal amount = new BigDecimal(
                        amountField.getText().trim()
                ).setScale(2, RoundingMode.UNNECESSARY);

                if (amount.signum() <= 0) {
                    showError(
                            "Invalid Budget",
                            "Budget must be greater than zero."
                    );
                    return;
                }

                budgets.put(categoryBox.getValue(), amount);
                showBudgets();

            } catch (NumberFormatException | ArithmeticException ex) {
                showError(
                        "Invalid Amount",
                        "Enter a valid amount with up to 2 decimal places."
                );
            }
        });

        HBox budgetInputs = new HBox(
                12, categoryBox, amountField, saveButton
        );
        budgetInputs.setAlignment(Pos.CENTER_LEFT);

        budgetForm.getChildren().addAll(
                formHeading,
                budgetInputs
        );

        VBox budgetList = createPanel();

        Label listHeading = new Label("Monthly Budget Overview");
        listHeading.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        budgetList.getChildren().add(listHeading);

        if (budgets.isEmpty()) {
            budgetList.getChildren().add(
                    createSubtitle("No budgets added yet.")
            );
        } else {
            for (Map.Entry<String, BigDecimal> entry :
                    budgets.entrySet()) {

                String category = entry.getKey();
                BigDecimal limit = entry.getValue();

                BigDecimal spent = getCategoryExpenses(category);
                double progress = spent.divide(
                        limit,
                        4,
                        RoundingMode.HALF_UP
                ).doubleValue();

                Label categoryLabel = new Label(category);
                categoryLabel.setFont(
                        Font.font("Arial", FontWeight.BOLD, 14)
                );

                Label amountLabel = new Label(
                        formatMoney(spent) + " / " + formatMoney(limit)
                );

                ProgressBar progressBar = new ProgressBar(
                        Math.min(progress, 1.0)
                );
                progressBar.setMaxWidth(Double.MAX_VALUE);

                Label status = new Label(
                        spent.compareTo(limit) > 0
                                ? "Budget exceeded"
                                : "Within budget"
                );

                status.setTextFill(
                        spent.compareTo(limit) > 0
                                ? Color.web(RED)
                                : Color.web(GREEN)
                );

                VBox item = new VBox(
                        7,
                        categoryLabel,
                        amountLabel,
                        progressBar,
                        status
                );

                item.setPadding(new Insets(12));
                item.setStyle("""
                    -fx-background-color: #F8FAFC;
                    -fx-background-radius: 8;
                    """);

                budgetList.getChildren().add(item);
            }
        }

        contentArea.getChildren().addAll(
                heading,
                description,
                budgetForm,
                budgetList
        );
    }

    private BigDecimal getCategoryExpenses(String category) {
        BigDecimal total = BigDecimal.ZERO;
        YearMonth currentMonth = YearMonth.now();

        for (Transaction transaction : transactions) {
            if (transaction.getType().equalsIgnoreCase("Expense")
                    && transaction.getCategory().equals(category)
                    && YearMonth.from(transaction.getTimestamp())
                    .equals(currentMonth)) {

                total = total.add(transaction.getAmount());
            }
        }

        return total;
    }

    // REPORTS

    private void showReports() {
        contentArea.getChildren().clear();

        Label heading = createHeading("Financial Reports");
        Label description = createSubtitle(
                "Monthly financial summary and category-wise spending analysis."
        );

        VBox reportPanel = createPanel();

        Label reportHeading = new Label(
                "Current Month Financial Summary"
        );
        reportHeading.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        YearMonth currentMonth = YearMonth.now();

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;

        Map<String, BigDecimal> categoryExpenses = new TreeMap<>();

        for (Transaction transaction : transactions) {

            if (!YearMonth.from(transaction.getTimestamp())
                    .equals(currentMonth)) {
                continue;
            }

            if (transaction.getType().equalsIgnoreCase("Income")) {
                income = income.add(transaction.getAmount());
            } else {
                expenses = expenses.add(transaction.getAmount());

                categoryExpenses.merge(
                        transaction.getCategory(),
                        transaction.getAmount(),
                        BigDecimal::add
                );
            }
        }

        BigDecimal netCashFlow = income.subtract(expenses);

        VBox summary = new VBox(10);

        summary.getChildren().addAll(
                reportLine("Total Income", formatMoney(income)),
                reportLine("Total Expenses", formatMoney(expenses)),
                reportLine("Net Cash Flow", formatMoney(netCashFlow))
        );

        Label categoryHeading = new Label("Category-wise Spending");
        categoryHeading.setFont(
                Font.font("Arial", FontWeight.BOLD, 16)
        );

        VBox categoryList = new VBox(10);

        if (expenses.compareTo(BigDecimal.ZERO) == 0) {
            categoryList.getChildren().add(
                    createSubtitle("No expenses recorded this month.")
            );
        } else {
            for (Map.Entry<String, BigDecimal> entry :
                    categoryExpenses.entrySet()) {

                BigDecimal percentage = entry.getValue()
                        .multiply(new BigDecimal("100"))
                        .divide(expenses, 2, RoundingMode.HALF_UP);

                Label categoryLabel = new Label(entry.getKey());
                categoryLabel.setFont(
                        Font.font("Arial", FontWeight.BOLD, 14)
                );

                Label amountLabel = new Label(
                        formatMoney(entry.getValue())
                                + "  (" + percentage.toPlainString() + "%)"
                );

                ProgressBar bar = new ProgressBar(
                        percentage.doubleValue() / 100.0
                );
                bar.setMaxWidth(Double.MAX_VALUE);

                categoryList.getChildren().add(
                        new VBox(5, categoryLabel, amountLabel, bar)
                );
            }
        }

        reportPanel.getChildren().addAll(
                reportHeading,
                summary,
                new Separator(),
                categoryHeading,
                categoryList
        );

        Button exportCsvButton = new Button("Export CSV");
        Button exportTextButton = new Button("Export Text Report");

        exportCsvButton.setOnAction(e -> exportCSV());
        exportTextButton.setOnAction(e -> exportTextReport());

        HBox exportButtons = new HBox(
                12, exportCsvButton, exportTextButton
        );

        contentArea.getChildren().addAll(
                heading,
                description,
                reportPanel,
                exportButtons
        );
    }

    private HBox reportLine(String label, String value) {
        Label name = new Label(label);
        Label amount = new Label(value);

        name.setFont(Font.font("Arial", 14));
        amount.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        return new HBox(10, name, spacer, amount);
    }

    // CSV EXPORT

    private void exportCSV() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Financial Summary");
        chooser.setInitialFileName("finance_summary.csv");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        java.io.File file = chooser.showSaveDialog(root.getScene().getWindow());

        if (file == null) return;

        YearMonth currentMonth = YearMonth.now();

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;

        Map<String, BigDecimal> categoryExpenses = new TreeMap<>();

        for (Transaction transaction : transactions) {
            if (!YearMonth.from(transaction.getTimestamp())
                    .equals(currentMonth)) {
                continue;
            }

            if (transaction.getType().equalsIgnoreCase("Income")) {
                income = income.add(transaction.getAmount());
            } else {
                expenses = expenses.add(transaction.getAmount());

                categoryExpenses.merge(
                        transaction.getCategory(),
                        transaction.getAmount(),
                        BigDecimal::add
                );
            }
        }

        try (var writer = Files.newBufferedWriter(file.toPath())) {

            writer.write("FINANCEFLOW - MONTHLY FINANCIAL SUMMARY");
            writer.newLine();
            writer.write("Month," + currentMonth);
            writer.newLine();
            writer.write("Total Income," + income.toPlainString());
            writer.newLine();
            writer.write("Total Expenses," + expenses.toPlainString());
            writer.newLine();
            writer.write(
                    "Net Cash Flow,"
                            + income.subtract(expenses).toPlainString()
            );
            writer.newLine();
            writer.newLine();

            writer.write("CATEGORY-WISE EXPENSES");
            writer.newLine();
            writer.write("Category,Amount,Spending Percentage");
            writer.newLine();

            for (Map.Entry<String, BigDecimal> entry :
                    categoryExpenses.entrySet()) {

                BigDecimal percentage = expenses.compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : entry.getValue()
                        .multiply(new BigDecimal("100"))
                        .divide(expenses, 2, RoundingMode.HALF_UP);

                writer.write(
                        csv(entry.getKey()) + ","
                                + entry.getValue().toPlainString() + ","
                                + percentage.toPlainString() + "%"
                );
                writer.newLine();
            }

            writer.newLine();
            writer.write("TRANSACTION HISTORY");
            writer.newLine();

            writer.write(
                    "ID,Description,Amount,Type,Category,Remarks,Timestamp"
            );
            writer.newLine();

            for (Transaction transaction : transactions) {
                writer.write(
                        transaction.getId() + ","
                                + csv(transaction.getDescription()) + ","
                                + transaction.getAmount().toPlainString() + ","
                                + csv(transaction.getType()) + ","
                                + csv(transaction.getCategory()) + ","
                                + csv(transaction.getRemarks()) + ","
                                + csv(transaction.getTimestamp().toString())
                );
                writer.newLine();
            }

            showInformation(
                    "Export Successful",
                    "CSV report saved successfully."
            );

        } catch (IOException ex) {
            showError("Export Error", ex.getMessage());
        }
    }

    private String csv(String value) {
        if (value == null) value = "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    // TEXT REPORT EXPORT

    private void exportTextReport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Text Report");
        chooser.setInitialFileName("finance_report.txt");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );

        java.io.File file = chooser.showSaveDialog(root.getScene().getWindow());

        if (file == null) return;

        YearMonth currentMonth = YearMonth.now();

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;

        for (Transaction transaction : transactions) {
            if (YearMonth.from(transaction.getTimestamp())
                    .equals(currentMonth)) {

                if (transaction.getType().equalsIgnoreCase("Income")) {
                    income = income.add(transaction.getAmount());
                } else {
                    expenses = expenses.add(transaction.getAmount());
                }
            }
        }

        try (var writer = Files.newBufferedWriter(file.toPath())) {

            writer.write("FINANCEFLOW - MONTHLY FINANCIAL REPORT");
            writer.newLine();
            writer.write("==============================");
            writer.newLine();
            writer.write("Month: " + currentMonth);
            writer.newLine();
            writer.write("Total Income: " + formatMoney(income));
            writer.newLine();
            writer.write("Total Expenses: " + formatMoney(expenses));
            writer.newLine();
            writer.write(
                    "Net Cash Flow: "
                            + formatMoney(income.subtract(expenses))
            );
            writer.newLine();

            showInformation(
                    "Export Successful",
                    "Text report saved successfully."
            );

        } catch (IOException ex) {
            showError("Export Error", ex.getMessage());
        }
    }

    // TABLE

    private TableView<Transaction> createTransactionTable() {
        TableView<Transaction> table = new TableView<>();

        TableColumn<Transaction, String> descriptionColumn =
                new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(
                data -> data.getValue().descriptionProperty()
        );

        TableColumn<Transaction, String> amountColumn =
                new TableColumn<>("Amount");
        amountColumn.setCellValueFactory(
                data -> data.getValue().amountProperty()
        );

        TableColumn<Transaction, String> typeColumn =
                new TableColumn<>("Type");
        typeColumn.setCellValueFactory(
                data -> data.getValue().typeProperty()
        );

        TableColumn<Transaction, String> categoryColumn =
                new TableColumn<>("Category");
        categoryColumn.setCellValueFactory(
                data -> data.getValue().categoryProperty()
        );

        TableColumn<Transaction, String> remarksColumn =
                new TableColumn<>("Remarks");
        remarksColumn.setCellValueFactory(
                data -> data.getValue().remarksProperty()
        );

        TableColumn<Transaction, String> timestampColumn =
                new TableColumn<>("Timestamp");
        timestampColumn.setCellValueFactory(
                data -> data.getValue().timestampProperty()
        );

        table.getColumns().addAll(
                descriptionColumn,
                amountColumn,
                typeColumn,
                categoryColumn,
                remarksColumn,
                timestampColumn
        );

        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS
        );

        table.setPlaceholder(new Label("No transactions found."));

        return table;
    }

    // UI HELPERS

    private VBox createPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 12;
            -fx-border-color: #E2E8F0;
            -fx-border-radius: 12;
            """);
        return panel;
    }

    private Label createHeading(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 27));
        label.setTextFill(Color.web("#0F172A"));
        return label;
    }

    private Label createSubtitle(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", 13));
        label.setTextFill(Color.web("#64748B"));
        return label;
    }

    private String formatMoney(BigDecimal amount) {
        return "₹" + amount.setScale(2, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(
                message == null ? "An unexpected error occurred." : message
        );
        alert.showAndWait();
    }

    private void showInformation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // TRANSACTION MODEL

    public static class Transaction {

        private final int id;
        private final String description;
        private final BigDecimal amount;
        private final String type;
        private final String category;
        private final String remarks;
        private final LocalDateTime timestamp;

        private final javafx.beans.property.SimpleStringProperty
                descriptionProperty;

        private final javafx.beans.property.SimpleStringProperty
                amountProperty;
        private final javafx.beans.property.SimpleStringProperty
                typeProperty;

        private final javafx.beans.property.SimpleStringProperty
                categoryProperty;

        private final javafx.beans.property.SimpleStringProperty
                remarksProperty;

        private final javafx.beans.property.SimpleStringProperty
                timestampProperty;

        public Transaction(
                int id,
                String description,
                BigDecimal amount,
                String type,
                String category,
                String remarks,
                LocalDateTime timestamp) {

            this.id = id;
            this.description = description;
            this.amount = amount;
            this.type = type;
            this.category = category;
            this.remarks = remarks == null ? "" : remarks;
            this.timestamp = timestamp;

            descriptionProperty =
                    new javafx.beans.property.SimpleStringProperty(description);

            amountProperty =
                    new javafx.beans.property.SimpleStringProperty(
                            amount.toPlainString()
                    );

            typeProperty =
                    new javafx.beans.property.SimpleStringProperty(type);

            categoryProperty =
                    new javafx.beans.property.SimpleStringProperty(category);

            remarksProperty =
                    new javafx.beans.property.SimpleStringProperty(this.remarks);

            timestampProperty =
                    new javafx.beans.property.SimpleStringProperty(
                            timestamp.format(
                                    DateTimeFormatter.ofPattern(
                                            "dd MMM yyyy, hh:mm a"
                                    )
                            )
                    );
        }

        public int getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public String getType() {
            return type;
        }

        public String getCategory() {
            return category;
        }

        public String getRemarks() {
            return remarks;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public javafx.beans.property.StringProperty descriptionProperty() {
            return descriptionProperty;
        }

        public javafx.beans.property.StringProperty amountProperty() {
            return amountProperty;
        }

        public javafx.beans.property.StringProperty typeProperty() {
            return typeProperty;
        }

        public javafx.beans.property.StringProperty categoryProperty() {
            return categoryProperty;
        }

        public javafx.beans.property.StringProperty remarksProperty() {
            return remarksProperty;
        }

        public javafx.beans.property.StringProperty timestampProperty() {
            return timestampProperty;
        }
    }
}