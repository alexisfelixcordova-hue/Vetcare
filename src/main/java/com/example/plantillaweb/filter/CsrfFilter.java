package com.example.plantillaweb.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Protección anti-CSRF: cada sesión tiene un token aleatorio ("csrf") que todo formulario
 * debe reenviar en un campo oculto "_csrf". Si un POST llega sin el token correcto,
 * significa que no vino de nuestro propio formulario y se rechaza.
 */
@WebFilter("/*")
public class CsrfFilter extends HttpFilter {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String contexto = request.getContextPath();
        String ruta = request.getRequestURI().substring(contexto.length());
        if (ruta.startsWith("/assets/")) {
            // Los recursos estáticos no llevan formulario ni sesión: seguir de largo evita crear
            // una HttpSession (con su token CSRF) por cada imagen/CSS/JS que pide el navegador.
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession();
        String token = (String) session.getAttribute("csrf");
        if (token == null) {
            token = generarToken();
            session.setAttribute("csrf", token);
        }

        if ("POST".equalsIgnoreCase(request.getMethod())) {
            String tokenRecibido = request.getParameter("_csrf");
            if (tokenRecibido == null || !tokenRecibido.equals(token)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token CSRF inválido");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private String generarToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
