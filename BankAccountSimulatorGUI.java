import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Bank Account Simulator with Swing GUI
 * Single-file project. Save as BankAccountSimulatorGUI.java and run with:
 *   javac BankAccountSimulatorGUI.java
 *   java BankAccountSimulatorGUI
 *
 * Features included:
 * - Multiple accounts
 * - PIN-based authentication (SHA-256 hashed)
 * - Deposit / Withdraw / Transfer
 * - Transaction history (with timestamps)
 * - Persistence (save/load via Java serialization)
 * - Basic validation and error messages
 * - Simple admin: list accounts, freeze/unfreeze, export CSV
 */
public class BankAccountSimulatorGUI extends JFrame {
    private static final String DATA_FILE = "bank_data.ser";
    private AccountManager manager;

    // UI components
    private JTextField createNameField;
    private JPasswordField createPinField;
    private JTextField createInitialField;
    private JLabel createStatusLabel;

    private JTextField loginAccField;
    private JPasswordField loginPinField;
    private JLabel loginStatusLabel;

    private JLabel loggedInLabel;
    private JButton logoutButton;

    // Operations
    private JTextField depositAmountField;
    private JTextField withdrawAmountField;
    private JTextField transferToField;
    private JTextField transferAmountField;
    private JTextArea historyArea;

    private DefaultTableModel accountsTableModel;

    private BankAccount currentAccount = null;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                BankAccountSimulatorGUI gui = new BankAccountSimulatorGUI();
                gui.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public BankAccountSimulatorGUI() {
        setTitle("Bank Account Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        // load data
        try {
            manager = AccountManager.loadFromFile(DATA_FILE);
        } catch (Exception e) {
            manager = new AccountManager();
        }

        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(root);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Home", buildHomePanel());
        tabs.add("Create Account", buildCreatePanel());
        tabs.add("Login / Operations", buildOperationsPanel());
        tabs.add("Admin", buildAdminPanel());

        root.add(tabs, BorderLayout.CENTER);

        // bottom save/exit
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> saveData());
        JButton exitBtn = new JButton("Save & Exit");
        exitBtn.addActionListener(e -> { saveData(); System.exit(0); });
        bottom.add(saveBtn);
        bottom.add(exitBtn);
        root.add(bottom, BorderLayout.SOUTH);
    }

    private JPanel buildHomePanel() {
        JPanel p = new JPanel(new BorderLayout(8,8));
        JTextArea info = new JTextArea();
        info.setEditable(false);
        info.setText("Welcome to the Bank Account Simulator GUI.\n\n"
                + "Features:\n"
                + "- Create accounts (owner name, 4-digit PIN, initial deposit)\n"
                + "- Login with account number + PIN\n"
                + "- Deposit, Withdraw, Transfer, View Transaction History\n"
                + "- Admin: list accounts, freeze/unfreeze, export CSV\n\n"
                + "Notes: PINs are stored as SHA-256 hashes for demonstration only.\n"
        );
        info.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        p.add(new JScrollPane(info), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildCreatePanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.EAST;
        p.add(new JLabel("Owner name:"), c);
        c.gridx = 1; c.anchor = GridBagConstraints.WEST;
        createNameField = new JTextField(20);
        p.add(createNameField, c);

        c.gridx = 0; c.gridy++ ; c.anchor = GridBagConstraints.EAST;
        p.add(new JLabel("Set PIN (4-6 digits):"), c);
        c.gridx = 1; c.anchor = GridBagConstraints.WEST;
        createPinField = new JPasswordField(10);
        p.add(createPinField, c);

        c.gridx = 0; c.gridy++ ; c.anchor = GridBagConstraints.EAST;
        p.add(new JLabel("Initial deposit:"), c);
        c.gridx = 1; c.anchor = GridBagConstraints.WEST;
        createInitialField = new JTextField(10);
        createInitialField.setText("0");
        p.add(createInitialField, c);

        c.gridx = 1; c.gridy++;
        JButton createBtn = new JButton("Create Account");
        createBtn.addActionListener(e -> doCreateAccount());
        p.add(createBtn, c);

        c.gridy++;
        createStatusLabel = new JLabel(" ");
        p.add(createStatusLabel, c);

        return p;
    }

    private JPanel buildOperationsPanel() {
        JPanel p = new JPanel(new BorderLayout(10,10));

        // login panel
        JPanel login = new JPanel(new FlowLayout(FlowLayout.LEFT));
        login.add(new JLabel("Account #: "));
        loginAccField = new JTextField(12);
        login.add(loginAccField);
        login.add(new JLabel("PIN: "));
        loginPinField = new JPasswordField(8);
        login.add(loginPinField);
        JButton loginBtn = new JButton("Login");
        loginBtn.addActionListener(e -> doLogin());
        login.add(loginBtn);
        loginStatusLabel = new JLabel(" ");
        login.add(loginStatusLabel);

        p.add(login, BorderLayout.NORTH);

        // center split for operations and history
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.4);
        split.setLeftComponent(buildOpsLeftPanel());
        split.setRightComponent(buildHistoryPanel());

        p.add(split, BorderLayout.CENTER);

        // top status
        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        loggedInLabel = new JLabel("Not logged in");
        logoutButton = new JButton("Logout");
        logoutButton.setEnabled(false);
        logoutButton.addActionListener(e -> doLogout());
        top.add(loggedInLabel);
        top.add(logoutButton);
        p.add(top, BorderLayout.SOUTH);

        return p;
    }

    private JPanel buildOpsLeftPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(10,10,10,10));

        // Balance
        JPanel bal = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bal.add(new JLabel("Balance: "));
        JLabel balanceLabel = new JLabel("- -");
        bal.add(balanceLabel);
        p.add(bal);

        // deposit
        JPanel dep = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dep.add(new JLabel("Deposit: "));
        depositAmountField = new JTextField(8);
        dep.add(depositAmountField);
        JButton depBtn = new JButton("Deposit");
        depBtn.addActionListener(e -> {
            try {
                ensureLoggedIn();
                double amt = Double.parseDouble(depositAmountField.getText().trim());
                currentAccount.deposit(amt);
                balanceLabel.setText(String.format("%.2f", currentAccount.getBalance()));
                appendHistoryText("Deposited " + amt);
                saveData();
            } catch (Exception ex) { showError(ex.getMessage()); }
        });
        dep.add(depBtn);
        p.add(dep);

        // withdraw
        JPanel wth = new JPanel(new FlowLayout(FlowLayout.LEFT));
        wth.add(new JLabel("Withdraw: "));
        withdrawAmountField = new JTextField(8);
        wth.add(withdrawAmountField);
        JButton wthBtn = new JButton("Withdraw");
        wthBtn.addActionListener(e -> {
            try {
                ensureLoggedIn();
                double amt = Double.parseDouble(withdrawAmountField.getText().trim());
                currentAccount.withdraw(amt);
                balanceLabel.setText(String.format("%.2f", currentAccount.getBalance()));
                appendHistoryText("Withdrew " + amt);
                saveData();
            } catch (Exception ex) { showError(ex.getMessage()); }
        });
        wth.add(wthBtn);
        p.add(wth);

        // transfer
        JPanel tr = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tr.add(new JLabel("Transfer to (acct #): "));
        transferToField = new JTextField(10);
        tr.add(transferToField);
        tr.add(new JLabel("Amount:"));
        transferAmountField = new JTextField(8);
        tr.add(transferAmountField);
        JButton trBtn = new JButton("Transfer");
        trBtn.addActionListener(e -> {
            try {
                ensureLoggedIn();
                long to = Long.parseLong(transferToField.getText().trim());
                double amt = Double.parseDouble(transferAmountField.getText().trim());
                manager.transfer(currentAccount.getAccountNumber(), new String(loginPinField.getPassword()), to, amt);
                // currentAccount might have been updated via manager.transfer; refresh label
                balanceLabel.setText(String.format("%.2f", currentAccount.getBalance()));
                appendHistoryText("Transferred " + amt + " to " + to);
                saveData();
            } catch (Exception ex) { showError(ex.getMessage()); }
        });
        tr.add(trBtn);
        p.add(tr);

        // refresh history button
        JButton refreshHist = new JButton("Refresh History");
        refreshHist.addActionListener(e -> showHistory());
        p.add(refreshHist);

        // placeholder glue
        p.add(Box.createVerticalGlue());

        return p;
    }

    private JPanel buildHistoryPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(10,10,10,10));
        historyArea = new JTextArea();
        historyArea.setEditable(false);
        historyArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        p.add(new JScrollPane(historyArea), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildAdminPanel() {
        JPanel p = new JPanel(new BorderLayout(8,8));
        accountsTableModel = new DefaultTableModel(new Object[]{"Account #","Owner","Balance","Frozen"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(accountsTableModel);
        refreshAccountsTable();
        p.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshAccountsTable());
        JButton freezeBtn = new JButton("Freeze/Unfreeze");
        freezeBtn.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r == -1) { showError("Select an account first"); return; }
            Object val = table.getValueAt(r, 0);
            long acc = (val instanceof Number) ? ((Number) val).longValue() : Long.parseLong(val.toString());
            BankAccount a = manager.getAccount(acc);
            if (a == null) { showError("Account not found"); return; }
            if (a.isFrozen()) a.unfreeze(); else a.freeze();
            refreshAccountsTable();
            saveData();
        });
        JButton exportBtn = new JButton("Export CSV");
        exportBtn.addActionListener(e -> exportCSV());
        buttons.add(refreshBtn); buttons.add(freezeBtn); buttons.add(exportBtn);
        p.add(buttons, BorderLayout.SOUTH);
        return p;
    }

    private void doCreateAccount() {
        String name = createNameField.getText().trim();
        String pin = new String(createPinField.getPassword()).trim();
        String initS = createInitialField.getText().trim();
        if (name.isEmpty()) { createStatusLabel.setText("Name required"); return; }
        if (!pin.matches("\\d{4,6}")) { createStatusLabel.setText("PIN must be 4-6 digits"); return; }
        double init = 0;
        try { init = Double.parseDouble(initS); if (init < 0) throw new NumberFormatException(); } catch (Exception e) { createStatusLabel.setText("Invalid initial amount"); return; }
        BankAccount a = manager.createAccount(name, pin, init);
        createStatusLabel.setText("Created account: " + a.getAccountNumber());
        createNameField.setText(""); createPinField.setText(""); createInitialField.setText("0");
        refreshAccountsTable();
        saveData();
    }

    private void doLogin() {
        try {
            long acc = Long.parseLong(loginAccField.getText().trim());
            String pin = new String(loginPinField.getPassword());
            BankAccount a = manager.getAccount(acc);
            if (a == null) { loginStatusLabel.setText("Account not found"); return; }
            if (!a.verifyPin(pin)) { loginStatusLabel.setText("Invalid PIN"); return; }
            if (a.isFrozen()) { loginStatusLabel.setText("Account is frozen"); return; }
            currentAccount = a;
            loggedInLabel.setText("Logged in: " + a.getOwner() + " (" + a.getAccountNumber() + ")");
            logoutButton.setEnabled(true);
            loginStatusLabel.setText("Logged in");
            showHistory();
        } catch (NumberFormatException e) { loginStatusLabel.setText("Invalid account number"); }
    }

    private void doLogout() {
        currentAccount = null;
        loggedInLabel.setText("Not logged in");
        logoutButton.setEnabled(false);
        loginStatusLabel.setText(" ");
        historyArea.setText("");
    }

    private void ensureLoggedIn() {
        if (currentAccount == null) throw new IllegalStateException("Please login first");
    }

    private void showHistory() {
        if (currentAccount == null) { historyArea.setText("Not logged in\n"); return; }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Account %d - %s\n", currentAccount.getAccountNumber(), currentAccount.getOwner()));
        sb.append(String.format("Balance: %.2f\n", currentAccount.getBalance()));
        sb.append("Transactions:\n");
        for (Transaction t : currentAccount.getTransactions()) sb.append("  " + t + "\n");
        historyArea.setText(sb.toString());
    }

    private void appendHistoryText(String s) {
        if (currentAccount == null) return;
        historyArea.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " - " + s + "\n");
    }

    private void refreshAccountsTable() {
        accountsTableModel.setRowCount(0);
        for (BankAccount a : manager.listAllAccounts()) {
            accountsTableModel.addRow(new Object[]{a.getAccountNumber(), a.getOwner(), a.getBalance(), a.isFrozen()});
        }
    }

    private void saveData() {
        try { manager.saveToFile(DATA_FILE); JOptionPane.showMessageDialog(this, "Data saved"); }
        catch (Exception e) { JOptionPane.showMessageDialog(this, "Save failed: " + e.getMessage()); }
    }

    private void exportCSV() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(f)) {
                pw.println("Account,Owner,Balance,Frozen");
                for (BankAccount a : manager.listAllAccounts()) {
                    pw.printf("%d,%s,%.2f,%b\n", a.getAccountNumber(), a.getOwner(), a.getBalance(), a.isFrozen());
                }
                JOptionPane.showMessageDialog(this, "Exported to " + f.getAbsolutePath());
            } catch (Exception e) { JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage()); }
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ----------------- Domain classes -----------------

    static class Transaction implements Serializable {
        enum Type {DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT}
        private final Date timestamp;
        private final Type type;
        private final double amount;
        private final String note;

        public Transaction(Type type, double amount, String note) {
            this.timestamp = new Date();
            this.type = type;
            this.amount = amount;
            this.note = note;
        }

        public String toString() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return String.format("[%s] %s %.2f (%s)", sdf.format(timestamp), type, amount, note);
        }
    }

    static class BankAccount implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String owner;
        private final long accountNumber;
        private final String pinHash;
        private double balance;
        private final java.util.List<Transaction> transactions = new ArrayList<>();
        private boolean frozen = false;

        public BankAccount(String owner, long accountNumber, String plainPin, double initial) {
            this.owner = owner;
            this.accountNumber = accountNumber;
            this.pinHash = hash(plainPin);
            this.balance = initial;
            transactions.add(new Transaction(Transaction.Type.DEPOSIT, initial, "Initial deposit"));
        }

        private static String hash(String input) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] b = md.digest(input.getBytes());
                StringBuilder sb = new StringBuilder();
                for (byte x : b) sb.append(String.format("%02x", x));
                return sb.toString();
            } catch (NoSuchAlgorithmException e) { throw new RuntimeException(e); }
        }

        public boolean verifyPin(String pin) { return hash(pin).equals(pinHash); }

        public synchronized void deposit(double amount) {
            if (frozen) throw new IllegalStateException("Account is frozen");
            if (amount <= 0) throw new IllegalArgumentException("Amount must be > 0");
            balance += amount;
            transactions.add(new Transaction(Transaction.Type.DEPOSIT, amount, "Deposit"));
        }

        public synchronized void withdraw(double amount) {
            if (frozen) throw new IllegalStateException("Account is frozen");
            if (amount <= 0) throw new IllegalArgumentException("Amount must be > 0");
            if (amount > balance) throw new IllegalArgumentException("Insufficient funds");
            balance -= amount;
            transactions.add(new Transaction(Transaction.Type.WITHDRAWAL, amount, "Withdrawal"));
        }

        public synchronized void transferOut(double amount, long toAccount) {
            if (frozen) throw new IllegalStateException("Account is frozen");
            if (amount <= 0) throw new IllegalArgumentException("Amount must be > 0");
            if (amount > balance) throw new IllegalArgumentException("Insufficient funds");
            balance -= amount;
            transactions.add(new Transaction(Transaction.Type.TRANSFER_OUT, amount, "Transfer to " + toAccount));
        }

        public synchronized void transferIn(double amount, long fromAccount) {
            balance += amount;
            transactions.add(new Transaction(Transaction.Type.TRANSFER_IN, amount, "Transfer from " + fromAccount));
        }

        public double getBalance() { return balance; }
        public long getAccountNumber() { return accountNumber; }
        public String getOwner() { return owner; }
        public java.util.List<Transaction> getTransactions() { return Collections.unmodifiableList(transactions); }
        public void freeze() { frozen = true; }
        public void unfreeze() { frozen = false; }
        public boolean isFrozen() { return frozen; }
    }

    static class AccountManager implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Map<Long, BankAccount> accounts = new HashMap<>();
        private long nextAccount = 1000000000L;

        public synchronized BankAccount createAccount(String owner, String pin, double initial) {
            long acc = nextAccount++;
            BankAccount a = new BankAccount(owner, acc, pin, initial);
            accounts.put(acc, a);
            return a;
        }

        public BankAccount getAccount(long acc) { return accounts.get(acc); }

        public Collection<BankAccount> listAllAccounts() { return accounts.values(); }

        public synchronized void transfer(long fromAcc, String fromPin, long toAcc, double amount) {
            BankAccount from = accounts.get(fromAcc);
            BankAccount to = accounts.get(toAcc);
            if (from == null || to == null) throw new IllegalArgumentException("Account not found");
            if (!from.verifyPin(fromPin)) throw new SecurityException("Invalid PIN");
            from.transferOut(amount, toAcc);
            to.transferIn(amount, fromAcc);
        }

        public synchronized void saveToFile(String filename) throws IOException {
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename))) {
                out.writeObject(this);
            }
        }

        public static AccountManager loadFromFile(String filename) throws IOException, ClassNotFoundException {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
                return (AccountManager) in.readObject();
            }
        }
    }
}
