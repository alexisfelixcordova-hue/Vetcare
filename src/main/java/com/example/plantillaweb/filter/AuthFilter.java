package com.example.plantillaweb.filter;

import com.example.plantillaweb.model.Usuario;
import com.example.plantillaweb.util.Flash;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Guardián de acceso: pasa por aquí TODA petición ("/*").
 * Sin sesión → a /login. Con sesión pero sin rol Administrador → no entra a /usuarios.
 * Además evita que el navegador guarde en caché páginas privadas (para que "atrás" no las muestre
 * después de cerrar sesión).
 */
@WebFilter("/*")
public class AuthFilter extends HttpFilter {

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String contexto = request.getContextPath();
        String ruta = request.getRequestURI().substring(contexto.length());

        if (ruta.startsWith("/assets/")) {
            // Recursos estáticos (css/js/libs/imágenes): "no-cache" NO significa "no cachear",
            // significa "cachear pero revalidar en cada petición" (If-Modified-Since/ETag, que
            // el DefaultServlet de Tomcat ya soporta solo). Así, si el archivo no cambió, el
            // servidor responde 304 sin reenviar el contenido (rápido); si cambió, se nota al
            // instante. "public, max-age=86400" (lo que había antes) cacheaba a ciegas por 24h:
            // cualquier cambio en un .css/.js quedaba invisible para el navegador hasta que
            // expirara el caché o el usuario forzara un refresh, con las vistas ya actualizadas
            // (JSP, que nunca se cachea) mezclándose con CSS viejo. Si en el futuro se sirven
            // los assets con nombre versionado (hash en la URL), ahí sí conviene max-age largo.
            response.setHeader("Cache-Control", "no-cache");
        } else {
            // Cabeceras anti-caché: ni /login ni las páginas privadas deben quedar guardadas en el
            // navegador. Sin esto, tras reiniciar el servidor el navegador puede mostrar una copia
            // vieja de /login con un token CSRF que ya no coincide con la sesión nueva (403).
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }

        if (esRutaPublica(ruta)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            response.sendRedirect(contexto + "/login");
            return;
        }

        if (ruta.startsWith("/usuarios") && !usuario.isAdministrador()) {
            Flash.error(request, "No tienes permiso para acceder a esa sección.");
            response.sendRedirect(contexto + "/dashboard");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean esRutaPublica(String ruta) {
        return ruta.equals("/login") || ruta.startsWith("/assets/");
    }
}
