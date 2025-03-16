package com.bank.admin;

import jakarta.servlet.*;

import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;

public class AdminLoginServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/bank__Db", "root", "root");

            PreparedStatement ps = con.prepareStatement("SELECT * FROM admin WHERE username=? AND password=?");
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                HttpSession session = request.getSession();
                session.setAttribute("username", username);
                response.sendRedirect("adminDashboard.jsp");
            } else {
                response.sendRedirect("adminLogin.jsp?error=1");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
