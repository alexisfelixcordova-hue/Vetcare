package com.example.plantillaweb.controller;

import com.example.plantillaweb.dao.UsuarioDAO;
import com.example.plantillaweb.model.Usuario;
import com.example.plantillaweb.util.Flash;
import com.example.plantillaweb.util.Password;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Pantalla de inicio de sesión: es la única ruta pública además de /assets/*. */
@WebServlet(name = "loginServlet", value = "/login")
public class LoginServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(LoginServlet.class.getName());
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("usuario") != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }
        request.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String usuario = request.getParameter("usuario");
        String clave = request.getParameter("password");

        try {
            Usuario u = (usuario == null || usuario.isBlank()) ? null : usuarioDAO.buscarPorUsuario(usuario.trim());

            boolean passwordValida = false;
            if (u != null) {
                try {
                    // Intenta verificar si está encriptada con BCrypt
                    passwordValida = Password.verificar(clave == null ? "" : clave, u.getClave());
                } catch (Exception e) {
                    // Si no es un hash válido, compara directamente el texto plano ('123456')
                    passwordValida = (u.getClave() != null && u.getClave().equals(clave));
                }
            }

            if (u != null && passwordValida) {
                // Se cambia el id de sesión al iniciar sesión para evitar "session fixation"
                request.changeSessionId();
                request.getSession().setAttribute("usuario", u);
                response.sendRedirect(request.getContextPath() + "/dashboard");
                return;
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error crítico procesando el inicio de sesión", e);
            Flash.error(request, "Error del sistema: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        Flash.error(request, "Usuario o contraseña incorrectos");
        response.sendRedirect(request.getContextPath() + "/login");
    }
}