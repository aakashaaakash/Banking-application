package com.bank.customer;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionHistoryServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        String accountNo = (String) session.getAttribute("accountNo");

        if (accountNo == null) {
            response.sendRedirect("customerLogin.jsp");
            return;
        }

        List<Transaction> transactions = new ArrayList<>();
        String url = "jdbc:mysql://localhost:3306/bank__Db";
        String user = "root";
        String password = "root";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(url, user, password);

            String query = "SELECT * FROM transaction WHERE account_no = ? ORDER BY date DESC LIMIT 20";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, accountNo);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.setId(rs.getInt("id"));
                transaction.setAccountNo(rs.getString("account_no"));
                transaction.setDate(rs.getTimestamp("date"));
                transaction.setType(rs.getString("type"));
                transaction.setAmount(rs.getDouble("amount"));
                transaction.setBalance(rs.getDouble("balance"));
                transactions.add(transaction);
            }

            rs.close();
            ps.close();
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        request.setAttribute("transactions", transactions);
        RequestDispatcher dispatcher = request.getRequestDispatcher("transactionHistory.jsp");
        dispatcher.forward(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
