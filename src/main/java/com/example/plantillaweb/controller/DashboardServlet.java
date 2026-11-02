package com.example.plantillaweb.controller;

import com.example.plantillaweb.dao.RolDAO;
import com.example.plantillaweb.dao.UsuarioDAO;
import com.example.plantillaweb.util.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Página de inicio tras el login: solo muestra un par de tarjetas con totales. */
@WebServlet(name = "dashboardServlet", value = "/dashboard")
public class DashboardServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(DashboardServlet.class.getName());
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final RolDAO rolDAO = new RolDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("totalUsuarios", usuarioDAO.contarActivos());
            request.setAttribute("totalRoles", rolDAO.contar());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error cargando los totales del dashboard", e);
            Flash.error(request, "No se pudieron cargar los datos del dashboard.");
            request.setAttribute("totalUsuarios", 0);
            request.setAttribute("totalRoles", 0);
        }
        request.getRequestDispatcher("/WEB-INF/views/dashboard/index.jsp").forward(request, response);
    }
}
