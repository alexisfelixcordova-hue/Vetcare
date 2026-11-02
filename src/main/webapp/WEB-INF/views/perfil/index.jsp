<%@ include file="/WEB-INF/views/layout/head.jspf" %>
<c:set var="activo" value="perfil" />
<%@ include file="/WEB-INF/views/layout/menu.jspf" %>

<div class="page-header">
    <div>
        <h3 class="page-title">Mi perfil</h3>
        <nav aria-label="breadcrumb">
            <ol class="breadcrumb">
                <li class="breadcrumb-item"><a href="${ctx}/dashboard">Inicio</a></li>
                <li class="breadcrumb-item active" aria-current="page">Mi perfil</li>
            </ol>
        </nav>
    </div>
</div>

<div class="row g-3">
    <div class="col-12 col-md-6">
        <div class="card shadow-sm">
            <div class="card-body">
                <h5 class="card-title">Mis datos</h5>
                <p class="mb-1"><strong>Nombres:</strong> <c:out value="${sessionScope.usuario.nombres}" /></p>
                <p class="mb-1"><strong>Apellidos:</strong> <c:out value="${sessionScope.usuario.apellidos}" /></p>
                <p class="mb-1"><strong>DNI:</strong> <c:out value="${sessionScope.usuario.dni}" /></p>
                <p class="mb-1"><strong>Usuario:</strong> <c:out value="${sessionScope.usuario.usuario}" /></p>
                <p class="mb-0"><strong>Rol:</strong> <c:out value="${sessionScope.usuario.nombreRol}" /></p>
            </div>
        </div>
    </div>

    <div class="col-12 col-md-6">
        <div class="card shadow-sm">
            <div class="card-body">
                <h5 class="card-title">Cambiar mi contraseña</h5>
                <form method="post" action="${ctx}/perfil" data-cargando novalidate>
                    <input type="hidden" name="_csrf" value="${sessionScope.csrf}">

                    <div class="mb-3">
                        <label class="form-label" for="passwordActual">Contraseña actual</label>
                        <input type="password" class="form-control" id="passwordActual" name="passwordActual" required>
                    </div>
                    <div class="mb-3">
                        <label class="form-label" for="passwordNueva">Nueva contraseña</label>
                        <input type="password" class="form-control" id="passwordNueva" name="passwordNueva" required minlength="4">
                    </div>
                    <div class="mb-3">
                        <label class="form-label" for="passwordConfirmar">Confirmar nueva contraseña</label>
                        <input type="password" class="form-control" id="passwordConfirmar" name="passwordConfirmar" required minlength="4">
                    </div>

                    <button type="submit" class="btn btn-primary"><i class="fa-solid fa-lock me-1"></i>Actualizar contraseña</button>
                </form>
            </div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/layout/scripts.jspf" %>
