🏦 Bank Account Simulator — Java Swing GUI

This project is a fully interactive Bank Account Simulator built using Java Swing, packaged neatly in a single file: BankAccountSimulatorGUI.java. It demonstrates realistic banking operations, secure authentication, data persistence, and a clean UI — making it an excellent learning project for Java beginners and intermediate developers.

⭐ Key Features (Explained)
🔐 Secure PIN Login

Every account is protected with a SHA-256 hashed PIN, ensuring safer authentication and teaching secure password-handling techniques in Java.

👤 Multiple Accounts

Users can create and manage several accounts. Each account includes its own balance, transaction history, and active/frozen status.

💼 Banking Operations

Perform essential actions easily through the GUI:

Deposit money

Withdraw funds

Transfer between accounts
All operations include proper validation like incorrect PIN handling, insufficient balance checks, and frozen account restrictions.

🧾 Transaction History

Each account stores a detailed list of all operations with timestamps, helping users track activity clearly and realistically.

💾 Data Persistence

The simulator uses Java Serialization to save all accounts and histories to a file, allowing data to automatically load when the program restarts.

🛠️ Admin Controls

A simple admin interface allows:

Viewing all account details

Freezing or unfreezing accounts

Exporting account and transaction data to CSV

🖥️ Clean Swing Interface

Built with Java Swing, the UI is simple, user-friendly, and includes clear prompts, warnings, and success messages for smooth interaction.

▶️ How to Run
javac BankAccountSimulatorGUI.java
java BankAccountSimulatorGUI
