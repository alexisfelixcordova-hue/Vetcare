<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    // Esta página no muestra nada: solo redirige. AuthFilter decide si el visitante
    // va al dashboard (con sesión) o al login (sin sesión).
    response.sendRedirect(request.getContextPath() + "/dashboard");
%>
