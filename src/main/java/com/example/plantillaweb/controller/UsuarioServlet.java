package com.example.plantillaweb.controller;

import com.example.plantillaweb.dao.RolDAO;
import com.example.plantillaweb.dao.UsuarioDAO;
import com.example.plantillaweb.model.Usuario;
import com.example.plantillaweb.util.Flash;
import com.example.plantillaweb.util.Password;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CRUD de usuarios. Único servlet para listar, crear, editar y eliminar:
 * GET solo muestra pantallas, POST solo modifica datos (ver método "accion").
 * Crear/editar ocurre en un modal dentro de lista.jsp (no hay una página de formulario aparte).
 */
@WebServlet(name = "usuarioServlet", value = "/usuarios")
@MultipartConfig(maxFileSize = 2 * 1024 * 1024, maxRequestSize = 3 * 1024 * 1024)
public class UsuarioServlet extends HttpServlet {

    /** Solo se aceptan estos tipos de imagen; la extensión sale de acá, nunca del nombre que manda el navegador. */
    private static final Map<String, String> TIPOS_IMAGEN_PERMITIDOS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp");

    private static final Logger LOG = Logger.getLogger(UsuarioServlet.class.getName());
    // Antes paginaba de a 10 en el servidor. Ahora se trae todo de una vez para que
    // DataTables (buscador, orden y paginado) trabaje sobre el listado completo en el navegador.
    private static final int POR_PAGINA = 5000;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final RolDAO rolDAO = new RolDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            mostrarLista(request, response, new Usuario(), false);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error consultando usuarios", e);
            Flash.error(request, "No se pudo cargar la información de usuarios.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String accion = request.getParameter("accion");
        try {
            if ("guardar".equals(accion)) {
                guardar(request, response);
            } else if ("eliminar".equals(accion)) {
                eliminar(request, response);
            } else {
                response.sendRedirect(request.getContextPath() + "/usuarios");
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error guardando el usuario", e);
            Flash.error(request, "Ocurrió un error al guardar los datos.");
            response.sendRedirect(request.getContextPath() + "/usuarios");
        }
    }

    /**
     * Renderiza la lista de usuarios. Si abrirModal es true, el modal de crear/editar se abre
     * solo (vía JS) con los datos de "usuario" ya escritos: es el caso de "falló la validación".
     */
    private void mostrarLista(HttpServletRequest request, HttpServletResponse response, Usuario usuario,
            boolean abrirModal) throws SQLException, ServletException, IOException {
        String buscar = request.getParameter("buscar");
        int pagina = Math.max(parseEntero(request.getParameter("pagina"), 1), 1);

        int total = usuarioDAO.contar(buscar);
        int totalPaginas = Math.max((int) Math.ceil(total / (double) POR_PAGINA), 1);
        pagina = Math.min(pagina, totalPaginas);

        request.setAttribute("usuarios", usuarioDAO.listar(buscar, pagina, POR_PAGINA));
        request.setAttribute("buscar", buscar == null ? "" : buscar);
        request.setAttribute("pagina", pagina);
        request.setAttribute("totalPaginas", totalPaginas);
        request.setAttribute("roles", rolDAO.listar());
        request.setAttribute("usuario", usuario);
        request.setAttribute("abrirModal", abrirModal);
        request.getRequestDispatcher("/WEB-INF/views/usuarios/lista.jsp").forward(request, response);
    }

    private void guardar(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        int id = parseEntero(request.getParameter("idUsuario"), 0);
        boolean esNuevo = id == 0;

        Usuario u = new Usuario();
        u.setIdUsuario(id);
        u.setNombres(valor(request.getParameter("nombres")));
        u.setApellidos(valor(request.getParameter("apellidos")));
        u.setDni(valor(request.getParameter("dni")));
        u.setEmail(valor(request.getParameter("email")));
        u.setUsuario(valor(request.getParameter("usuario")));
        u.setPassword(valor(request.getParameter("password")));
        u.setIdRol(parseEntero(request.getParameter("idRol"), 0));
        u.setEstado(esNuevo || "1".equals(request.getParameter("estado")));
        u.setFoto(guardarFoto(request));

        Usuario actual = (Usuario) request.getSession().getAttribute("usuario");
        if (!esNuevo && !u.isEstado() && actual != null && actual.getIdUsuario() == id) {
            Flash.error(request, "No puedes desactivar tu propio usuario.");
            mostrarLista(request, response, u, true);
            return;
        }

        String error = validar(u, esNuevo);
        if (error != null) {
            Flash.error(request, error);
            mostrarLista(request, response, u, true);
            return;
        }

        if (esNuevo) {
            u.setPassword(Password.encriptar(u.getPassword()));
            usuarioDAO.insertar(u);
            Flash.exito(request, "Usuario creado correctamente.");
        } else {
            if (!u.getPassword().isBlank()) {
                u.setPassword(Password.encriptar(u.getPassword()));
            }
            usuarioDAO.actualizar(u);
            Flash.exito(request, "Usuario actualizado correctamente.");
        }
        response.sendRedirect(request.getContextPath() + "/usuarios");
    }

    /**
     * Si el usuario está activo, esto es una baja lógica (queda inactivo).
     * Si ya estaba inactivo, se borra la fila de verdad: es la forma de "limpiar" bajas viejas.
     */
    private void eliminar(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        int id = parseEntero(request.getParameter("id"), 0);
        Usuario actual = (Usuario) request.getSession().getAttribute("usuario");
        Usuario objetivo = usuarioDAO.obtenerPorId(id);

        if (actual != null && actual.getIdUsuario() == id) {
            Flash.error(request, "No puedes eliminar tu propio usuario.");
        } else if (objetivo == null) {
            Flash.error(request, "El usuario no existe.");
        } else if (objetivo.isEstado()) {
            usuarioDAO.eliminar(id);
            Flash.exito(request, "Usuario desactivado correctamente.");
        } else {
            usuarioDAO.eliminarDefinitivo(id);
            Flash.exito(request, "Usuario eliminado definitivamente.");
        }
        response.sendRedirect(request.getContextPath() + "/usuarios");
    }

    /** Validaciones de servidor. Devuelve el mensaje de error, o null si todo está bien. */
    private String validar(Usuario u, boolean esNuevo) throws SQLException {
        if (u.getNombres().isBlank() || u.getApellidos().isBlank() || u.getDni().isBlank()
                || u.getUsuario().isBlank() || u.getIdRol() == 0) {
            return "Todos los campos obligatorios deben estar completos.";
        }
        if (!u.getDni().matches("\\d{8}")) {
            return "El DNI debe tener exactamente 8 dígitos.";
        }
        if (!u.getEmail().isBlank() && !u.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return "El correo electrónico no es válido.";
        }
        if (esNuevo && u.getPassword().isBlank()) {
            return "La contraseña es obligatoria para un usuario nuevo.";
        }
        if (usuarioDAO.existeDni(u.getDni(), u.getIdUsuario())) {
            return "Ya existe otro usuario con ese DNI.";
        }
        if (usuarioDAO.existeUsuario(u.getUsuario(), u.getIdUsuario())) {
            return "Ya existe otro usuario con ese nombre de usuario.";
        }
        return null;
    }

    /**
     * Si vino un archivo nuevo en el campo "foto", lo guarda en /assets/images/usuarios con un
     * nombre generado por el servidor y devuelve ese nombre. Si no vino nada, devuelve null
     * (así actualizar() sabe que no debe tocar la foto que ya había).
     */
    private String guardarFoto(HttpServletRequest request) throws IOException, ServletException {
        Part parte = request.getPart("foto");
        if (parte == null || parte.getSize() == 0) {
            return null;
        }
        String extension = TIPOS_IMAGEN_PERMITIDOS.get(parte.getContentType());
        if (extension == null) {
            return null;
        }

        Path directorio = Path.of(getServletContext().getRealPath("/assets/images/usuarios"));
        Files.createDirectories(directorio);
        String nombreArchivo = UUID.randomUUID() + extension;
        try (InputStream entrada = parte.getInputStream()) {
            Files.copy(entrada, directorio.resolve(nombreArchivo), StandardCopyOption.REPLACE_EXISTING);
        }
        return nombreArchivo;
    }

    private String valor(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private int parseEntero(String texto, int porDefecto) {
        try {
            return texto == null ? porDefecto : Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }
}
