package com.example.plantillaweb.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Mensajes de una sola lectura: se guardan en sesión antes de un redirect y la vista
 * los muestra una vez (con SweetAlert2) y los borra. Así se implementa Post/Redirect/Get
 * sin perder el mensaje de éxito/error al recargar la página.
 */
public class Flash {

    private Flash() {
        // Clase de utilidad: no se instancia
    }

    public static void exito(HttpServletRequest request, String mensaje) {
        guardar(request, "success", mensaje);
    }

    public static void error(HttpServletRequest request, String mensaje) {
        guardar(request, "error", mensaje);
    }

    public static void advertencia(HttpServletRequest request, String mensaje) {
        guardar(request, "warning", mensaje);
    }

    private static void guardar(HttpServletRequest request, String tipo, String mensaje) {
        request.getSession().setAttribute("flashTipo", tipo);
        request.getSession().setAttribute("flashMsg", mensaje);
    }
}
