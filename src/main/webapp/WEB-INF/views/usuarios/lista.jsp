<%@ include file="/WEB-INF/views/layout/head.jspf" %>
<c:set var="activo" value="usuarios" />
<c:set var="esNuevo" value="${usuario.idUsuario == 0}" />
<%@ include file="/WEB-INF/views/layout/menu.jspf" %>

<div class="page-header">
    <div>
        <h3 class="page-title">Usuarios</h3>
        <nav aria-label="breadcrumb">
            <ol class="breadcrumb">
                <li class="breadcrumb-item"><a href="${ctx}/dashboard">Inicio</a></li>
                <li class="breadcrumb-item active" aria-current="page">Usuarios</li>
            </ol>
        </nav>
    </div>
</div>

<div class="table-card">
    <div class="table-header">
        <span class="card-header-tabla"><i class="fa-solid fa-list text-primary me-2"></i>Lista de usuarios</span>
        <button type="button" class="btn btn-primary" id="btnNuevoUsuario"
                data-bs-toggle="modal" data-bs-target="#modalUsuario">
            <i class="fa-solid fa-plus me-1"></i>Nuevo
        </button>
    </div>
    <div class="table-responsive">
        <table class="table table-hover align-middle mb-0 tabla-plantilla" id="tablaUsuarios">
            <thead>
            <tr>
                <th>N°</th>
                <th>Foto</th>
                <th>Nombres</th>
                <th>Apellidos</th>
                <th>DNI</th>
                <th>Usuario</th>
                <th>Rol</th>
                <th>Estado</th>
                <th class="text-end">Acción</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="u" items="${usuarios}" varStatus="fila">
                <tr>
                    <td><c:out value="${fila.index + 1}" /></td>
                    <td>
                        <c:choose>
                            <c:when test="${not empty u.foto}">
                                <img src="${ctx}/assets/images/usuarios/${u.foto}" class="rounded-circle"
                                     width="36" height="36" alt="Foto de ${fn:escapeXml(u.nombreCompleto)}">
                            </c:when>
                            <c:otherwise>
                                <img src="${ctx}/assets/images/users/avatar-1.jpg" class="rounded-circle"
                                     width="36" height="36" alt="Foto de ${fn:escapeXml(u.nombreCompleto)}">
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td><c:out value="${u.nombres}" /></td>
                    <td><c:out value="${u.apellidos}" /></td>
                    <td><c:out value="${u.dni}" /></td>
                    <td><c:out value="${u.usuario}" /></td>
                    <td><span class="badge badge-info"><c:out value="${u.nombreRol}" /></span></td>
                    <td>
                        <c:choose>
                            <c:when test="${u.estado}">
                                <span class="badge badge-success">Activo</span>
                            </c:when>
                            <c:otherwise>
                                <span class="badge badge-danger">Inactivo</span>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td class="text-end">
                        <button type="button" class="btn btn-accion btn-accion-primary btn-editar-usuario"
                                data-bs-toggle="modal" data-bs-target="#modalUsuario"
                                data-id-usuario="${u.idUsuario}"
                                data-nombres="${fn:escapeXml(u.nombres)}"
                                data-apellidos="${fn:escapeXml(u.apellidos)}"
                                data-dni="${u.dni}"
                                data-usuario="${fn:escapeXml(u.usuario)}"
                                data-email="${fn:escapeXml(u.email)}"
                                data-id-rol="${u.idRol}"
                                data-estado="${u.estado ? 1 : 0}"
                                data-foto-url="${not empty u.foto ? ctx.concat('/assets/images/usuarios/').concat(u.foto) : ''}">
                            <i class="fa-solid fa-pen-to-square"></i>
                        </button>
                        <c:choose>
                            <c:when test="${u.estado}">
                                <form method="post" action="${ctx}/usuarios" class="d-inline"
                                      data-confirmar="¿Desactivar a ${fn:escapeXml(u.nombreCompleto)}?"
                                      data-confirmar-tipo="advertencia">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrf}">
                                    <input type="hidden" name="accion" value="eliminar">
                                    <input type="hidden" name="id" value="${u.idUsuario}">
                                    <%-- Reversible (solo cambia estado=0): ambar + icono de bloqueo, para que
                                         no se confunda a simple vista con el rojo de abajo (borrado real). --%>
                                    <button type="submit" class="btn btn-accion btn-accion-warning" title="Desactivar">
                                        <i class="fa-solid fa-ban"></i>
                                    </button>
                                </form>
                            </c:when>
                            <c:otherwise>
                                <form method="post" action="${ctx}/usuarios" class="d-inline"
                                      data-confirmar="¿Eliminar definitivamente a ${fn:escapeXml(u.nombreCompleto)}? Esta acción no se puede deshacer."
                                      data-confirmar-tipo="peligro">
                                    <input type="hidden" name="_csrf" value="${sessionScope.csrf}">
                                    <input type="hidden" name="accion" value="eliminar">
                                    <input type="hidden" name="id" value="${u.idUsuario}">
                                    <%-- Irreversible: rojo + basurero, reservado solo para esto. --%>
                                    <button type="submit" class="btn btn-accion btn-accion-danger" title="Eliminar definitivamente">
                                        <i class="fa-solid fa-trash"></i>
                                    </button>
                                </form>
                            </c:otherwise>
                        </c:choose>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty usuarios}">
                <tr>
                    <td colspan="9">
                        <div class="empty-state">
                            <i class="fa-solid fa-users-slash"></i>
                            <span class="empty-state-titulo">No hay registros</span>
                            <span class="empty-state-texto">Todavía no existen usuarios registrados.</span>
                        </div>
                    </td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </div>
</div>

<%-- Un solo modal para crear y editar. Se llena con JS: al hacer clic en "Nuevo" queda vacío,
     al hacer clic en "Editar" se copian los data-* del botón a los campos del formulario. --%>
<div class="modal fade" id="modalUsuario" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <%-- autocomplete="off": este mismo modal se reusa para Nuevo y Editar. Sin esto, el
                 navegador "recuerda" lo ultimo que se escribio en cada campo (nombres, DNI,
                 usuario, correo) y lo vuelve a mostrar solo al abrir "Nuevo", aunque el server
                 haya mandado el formulario vacio. --%>
            <form method="post" action="${ctx}/usuarios" enctype="multipart/form-data" autocomplete="off" data-cargando novalidate>
                <div class="modal-header">
                    <h5 class="modal-title" id="modalUsuarioTitulo">${esNuevo ? 'Nuevo usuario' : 'Editar usuario'}</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrf}">
                    <input type="hidden" name="accion" value="guardar">
                    <input type="hidden" name="idUsuario" value="${usuario.idUsuario}">

                    <div class="row g-3">
                        <div class="col-12">
                            <label class="form-label" for="foto">Foto de perfil</label>
                            <div class="d-flex align-items-center gap-3">
                                <img id="fotoPreview" src="" class="rounded-circle d-none" width="56" height="56" alt="Foto actual">
                                <input type="file" class="form-control" id="foto" name="foto"
                                       accept="image/png, image/jpeg, image/gif, image/webp">
                            </div>
                            <small class="text-muted">Opcional. JPG, PNG, GIF o WEBP, hasta 2 MB.</small>
                        </div>
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="nombres">Nombres</label>
                            <input type="text" class="form-control" id="nombres" name="nombres"
                                   value="${fn:escapeXml(usuario.nombres)}" required>
                        </div>
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="apellidos">Apellidos</label>
                            <input type="text" class="form-control" id="apellidos" name="apellidos"
                                   value="${fn:escapeXml(usuario.apellidos)}" required>
                        </div>
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="dni">DNI</label>
                            <input type="text" class="form-control" id="dni" name="dni" maxlength="8" pattern="\d{8}"
                                   value="${fn:escapeXml(usuario.dni)}" required>
                        </div>
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="idRol">Rol</label>
                            <select class="form-select" id="idRol" name="idRol" required>
                                <option value="">Selecciona...</option>
                                <c:forEach var="r" items="${roles}">
                                    <option value="${r.idRol}" ${r.idRol == usuario.idRol ? 'selected' : ''}>
                                        <c:out value="${r.nombre}" />
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="usuario">Usuario</label>
                            <input type="text" class="form-control" id="usuario" name="usuario" autocomplete="off"
                                   value="${fn:escapeXml(usuario.usuario)}" required>
                        </div>
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="email">Correo electrónico</label>
                            <input type="email" class="form-control" id="email" name="email"
                                   value="${fn:escapeXml(usuario.email)}">
                        </div>
                        <div class="col-12 col-md-6 ${esNuevo ? 'd-none' : ''}" id="estadoWrapper">
                            <label class="form-label" for="estado">Estado</label>
                            <select class="form-select" id="estado" name="estado">
                                <option value="1" ${(esNuevo || usuario.estado) ? 'selected' : ''}>Activo</option>
                                <option value="0" ${(!esNuevo && !usuario.estado) ? 'selected' : ''}>Inactivo</option>
                            </select>
                        </div>
                        <div class="col-12">
                            <label class="form-label" for="password">
                                Contraseña
                                <small id="passwordAyuda" class="text-muted ${esNuevo ? 'd-none' : ''}">
                                    (dejar vacío para no cambiarla)
                                </small>
                            </label>
                            <input type="password" class="form-control" id="password" name="password"
                                   autocomplete="new-password" ${esNuevo ? 'required' : ''}>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button>
                    <button type="submit" class="btn btn-primary"><i class="fa-solid fa-floppy-disk me-1"></i>Guardar</button>
                </div>
            </form>
        </div>
    </div>
</div>

<c:if test="${abrirModal}">
    <span class="d-none" data-modal-autoabrir="modalUsuario"></span>
</c:if>

<script>
    // Título y campo "contraseña" del modal cambian según Nuevo vs Editar.
    // Los valores de los otros campos los pone el manejador genérico de app.js (data-* -> [name]).
    (function () {
        var tituloEl = document.getElementById("modalUsuarioTitulo");
        var passwordEl = document.getElementById("password");
        var passwordAyudaEl = document.getElementById("passwordAyuda");
        var estadoWrapperEl = document.getElementById("estadoWrapper");
        var estadoEl = document.getElementById("estado");
        var fotoPreviewEl = document.getElementById("fotoPreview");

        document.getElementById("btnNuevoUsuario").addEventListener("click", function () {
            tituloEl.textContent = "Nuevo usuario";
            passwordEl.setAttribute("required", "required");
            passwordAyudaEl.classList.add("d-none");
            estadoWrapperEl.classList.add("d-none");
            estadoEl.value = "1";
            fotoPreviewEl.classList.add("d-none");
            fotoPreviewEl.src = "";
        });

        document.querySelectorAll(".btn-editar-usuario").forEach(function (boton) {
            boton.addEventListener("click", function () {
                tituloEl.textContent = "Editar usuario";
                passwordEl.removeAttribute("required");
                passwordAyudaEl.classList.remove("d-none");
                estadoWrapperEl.classList.remove("d-none");
                if (boton.dataset.fotoUrl) {
                    fotoPreviewEl.src = boton.dataset.fotoUrl;
                    fotoPreviewEl.classList.remove("d-none");
                } else {
                    fotoPreviewEl.classList.add("d-none");
                    fotoPreviewEl.src = "";
                }
            });
        });
    })();
</script>

<%-- DataTables: el servidor ahora trae el listado completo (ver UsuarioServlet), asi que
     el buscador, el orden y el paginado de esta tabla los resuelve DataTables en el navegador. --%>
<c:if test="${not empty usuarios}">
    <script>
        // DOMContentLoaded diferido: en la carga normal de la pagina, jQuery/DataTables (que
        // este bloque llegan cargados mas abajo, en scripts.jspf) todavia no existen cuando el
        // parser llega aca, asi que hay que esperar a que termine de cargar todo. Pero si esta
        // pagina llega por navegacion sin recarga (ver assets/js/navegacion-spa.js), el bloque
        // se re-ejecuta con el documento ya "complete" y las librerias ya cargadas desde la
        // primera carga real: ahi DOMContentLoaded no vuelve a disparar nunca (es un evento de
        // una sola vez), asi que hay que llamar la funcion directo.
        function inicializarTablaUsuarios() {
            $("#tablaUsuarios").DataTable({
                language: DATATABLES_ES,
                pageLength: 10,
                lengthMenu: [10, 25, 50, 100],
                responsive: true,
                autoWidth: false,
                columnDefs: [
                    { orderable: false, targets: [0, 1, 8] },
                    { responsivePriority: 1, targets: 2 },
                    { responsivePriority: 1, targets: 8 },
                    { responsivePriority: 2, targets: 6 },
                    { responsivePriority: 3, targets: 3 },
                    { responsivePriority: 4, targets: 4 },
                    { responsivePriority: 5, targets: 5 },
                    { responsivePriority: 6, targets: 7 },
                    { responsivePriority: 7, targets: 0 },
                    { responsivePriority: 8, targets: 1 }
                ]
            });
        }

        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", inicializarTablaUsuarios);
        } else {
            inicializarTablaUsuarios();
        }
    </script>
</c:if>

<%@ include file="/WEB-INF/views/layout/scripts.jspf" %>
