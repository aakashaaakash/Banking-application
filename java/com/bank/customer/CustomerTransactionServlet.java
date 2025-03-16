package com.bank.customer;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;

public class CustomerTransactionServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Retrieve form data
        String transactionType = request.getParameter("type");
        double amount = Double.parseDouble(request.getParameter("amount"));

        // Get current user's account number from session
        HttpSession session = request.getSession();
        String accountNo = (String) session.getAttribute("accountNo");

        // Perform database operations for transaction
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/bank__Db", "root", "root");

            // Retrieve current balance
            PreparedStatement psBalance = con.prepareStatement("SELECT initial_balance FROM customer WHERE account_no=?");
            psBalance.setString(1, accountNo);
            ResultSet rs = psBalance.executeQuery();
            if (rs.next()) {
                double currentBalance = rs.getDouble("initial_balance");
                double newBalance = currentBalance;

                // Perform deposit or withdrawal based on transaction type
                if ("Deposit".equals(transactionType)) {
                    newBalance = currentBalance + amount;
                    PreparedStatement psDeposit = con.prepareStatement("UPDATE customer SET initial_balance=? WHERE account_no=?");
                    psDeposit.setDouble(1, newBalance);
                    psDeposit.setString(2, accountNo);
                    int rowsUpdated = psDeposit.executeUpdate();

                    if (rowsUpdated > 0) {
                        // Insert transaction record
                        PreparedStatement psTransaction = con.prepareStatement("INSERT INTO transaction (account_no, type, amount, balance) VALUES (?, ?, ?, ?)");
                        psTransaction.setString(1, accountNo);
                        psTransaction.setString(2, "Deposit");
                        psTransaction.setDouble(3, amount);
                        psTransaction.setDouble(4, newBalance);
                        psTransaction.executeUpdate();

                        // Set success message
                        request.setAttribute("successMessage", "Deposit of ₹" + amount + " successful!");
                    }
                } else if ("Withdraw".equals(transactionType)) {
                    if (currentBalance >= amount) {
                        newBalance = currentBalance - amount;
                        PreparedStatement psWithdraw = con.prepareStatement("UPDATE customer SET initial_balance=? WHERE account_no=?");
                        psWithdraw.setDouble(1, newBalance);
                        psWithdraw.setString(2, accountNo);
                        int rowsUpdated = psWithdraw.executeUpdate();

                        if (rowsUpdated > 0) {
                            // Insert transaction record
                            PreparedStatement psTransaction = con.prepareStatement("INSERT INTO transaction (account_no, type, amount, balance) VALUES (?, ?, ?, ?)");
                            psTransaction.setString(1, accountNo);
                            psTransaction.setString(2, "Withdraw");
                            psTransaction.setDouble(3, amount);
                            psTransaction.setDouble(4, newBalance);
                            psTransaction.executeUpdate();

                            // Set success message
                            request.setAttribute("successMessage", "Withdrawal of ₹" + amount + " successful!");
                        }
                    } else {
                        // Insufficient balance error handling
                        request.setAttribute("errorMessage", "Insufficient balance for withdrawal.");
                    }
                }
            }

            // Forward to Transaction.jsp to display message
            RequestDispatcher dispatcher = request.getRequestDispatcher("transaction.jsp");
            dispatcher.forward(request, response);

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            // Redirect to an error page or display a generic error message
            response.sendRedirect("transaction.jsp?error=databaseError");
        }
    }
}
