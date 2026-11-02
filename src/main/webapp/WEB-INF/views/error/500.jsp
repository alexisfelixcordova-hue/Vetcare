<%-- Página de error: no usa menu.jspf porque puede mostrarse sin sesión iniciada. --%>
<%@ include file="/WEB-INF/views/layout/head.jspf" %>

<div class="pagina-login px-3 text-center">
    <div>
        <h1 class="display-1 fw-bold text-danger">500</h1>
        <p class="fs-5 text-muted mb-4">Ocurrió un error inesperado. Intenta nuevamente.</p>
        <a href="${ctx}/dashboard" class="btn btn-primary"><i class="fa-solid fa-house me-1"></i>Volver al inicio</a>
    </div>
</div>

</body>
</html>
