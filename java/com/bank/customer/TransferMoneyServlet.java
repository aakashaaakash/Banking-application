package com.bank.customer;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;

public class TransferMoneyServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Retrieve form data
        String recipientAccountNo = request.getParameter("recipientAccountNo");
        double amount = Double.parseDouble(request.getParameter("amount"));

        // Get current user's account number from session
        HttpSession session = request.getSession();
        String accountNo = (String) session.getAttribute("accountNo");

        // Perform database operations to transfer money
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/bank__Db", "root", "root");

            // Check if the recipient account exists
            PreparedStatement psCheckRecipient = con.prepareStatement("SELECT * FROM customer WHERE account_no=?");
            psCheckRecipient.setString(1, recipientAccountNo);
            ResultSet rsRecipient = psCheckRecipient.executeQuery();

            if (rsRecipient.next()) {
                // Begin transaction (optional, for ACID properties)
                con.setAutoCommit(false);

                // Update balance for current user (deduct amount)
                PreparedStatement psUpdateSender = con.prepareStatement("UPDATE customer SET initial_balance = initial_balance - ? WHERE account_no = ?");
                psUpdateSender.setDouble(1, amount);
                psUpdateSender.setString(2, accountNo);
                psUpdateSender.executeUpdate();

                // Update balance for recipient (add amount)
                PreparedStatement psUpdateRecipient = con.prepareStatement("UPDATE customer SET initial_balance = initial_balance + ? WHERE account_no = ?");
                psUpdateRecipient.setDouble(1, amount);
                psUpdateRecipient.setString(2, recipientAccountNo);
                psUpdateRecipient.executeUpdate();

                // Insert into transaction table for sender (transfer out)
                String insertSenderTransaction = "INSERT INTO transaction (account_no, date, type, amount, balance) VALUES (?, NOW(), 'transfer', ?, ?)";
                PreparedStatement psInsertSenderTransaction = con.prepareStatement(insertSenderTransaction);
                psInsertSenderTransaction.setString(1, accountNo);
                psInsertSenderTransaction.setDouble(2, -amount); // Negative amount for transfer out
                psInsertSenderTransaction.setDouble(3, getBalance(con, accountNo)); // Get updated balance
                psInsertSenderTransaction.executeUpdate();

                // Insert into transaction table for recipient (transfer in)
                String insertRecipientTransaction = "INSERT INTO transaction (account_no, date, type, amount, balance) VALUES (?, NOW(), 'transfer', ?, ?)";
                PreparedStatement psInsertRecipientTransaction = con.prepareStatement(insertRecipientTransaction);
                psInsertRecipientTransaction.setString(1, recipientAccountNo);
                psInsertRecipientTransaction.setDouble(2, amount); // Positive amount for transfer in
                psInsertRecipientTransaction.setDouble(3, getBalance(con, recipientAccountNo)); // Get updated balance
                psInsertRecipientTransaction.executeUpdate();

                // Commit transaction
                con.commit();

                // Set success attribute to true
                request.setAttribute("successMessage", "transfer of ₹" + amount + " successful!");

            } else {
                // Handle if recipient account doesn't exist
                // Redirect to transfermoney.jsp with error parameter
                response.sendRedirect("transferMoney.jsp?error=recipientNotFound");
                return; // Exit the method to avoid further execution
            }

            // Forward request to transferMoney.jsp
            RequestDispatcher dispatcher = request.getRequestDispatcher("transferMoney.jsp");
            dispatcher.forward(request, response);

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            // Redirect to transfermoney.jsp with error parameter for database error
            response.sendRedirect("transfermoney.jsp?error=databaseError");
        }
    }

    // Helper method to get current balance from database
    private double getBalance(Connection con, String accountNo) throws SQLException {
        String query = "SELECT initial_balance FROM customer WHERE account_no = ?";
        PreparedStatement ps = con.prepareStatement(query);
        ps.setString(1, accountNo);
        ResultSet rs = ps.executeQuery();
        double balance = 0;
        if (rs.next()) {
            balance = rs.getDouble("initial_balance");
        }
        rs.close();
        ps.close();
        return balance;
    }
}
