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
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Cada usuario ve sus propios datos y puede cambiar su contraseña. */
@WebServlet(name = "perfilServlet", value = "/perfil")
public class PerfilServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(PerfilServlet.class.getName());
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/perfil/index.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        Usuario sesionUsuario = (Usuario) session.getAttribute("usuario");

        String actual = request.getParameter("passwordActual");
        String nueva = request.getParameter("passwordNueva");
        String confirmacion = request.getParameter("passwordConfirmar");

        try {
            Usuario enBd = usuarioDAO.obtenerPorId(sesionUsuario.getIdUsuario());
            if (actual == null || !Password.verificar(actual, enBd.getPassword())) {
                Flash.error(request, "La contraseña actual no es correcta.");
            } else if (nueva == null || nueva.isBlank() || nueva.length() < 4) {
                Flash.error(request, "La nueva contraseña debe tener al menos 4 caracteres.");
            } else if (!nueva.equals(confirmacion)) {
                Flash.error(request, "La confirmación no coincide con la nueva contraseña.");
            } else {
                String nuevoHash = Password.encriptar(nueva);
                usuarioDAO.cambiarPassword(sesionUsuario.getIdUsuario(), nuevoHash);
                sesionUsuario.setPassword(nuevoHash);
                Flash.exito(request, "Tu contraseña se actualizó correctamente.");
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error cambiando la contraseña", e);
            Flash.error(request, "No se pudo actualizar la contraseña.");
        }
        response.sendRedirect(request.getContextPath() + "/perfil");
    }
}
