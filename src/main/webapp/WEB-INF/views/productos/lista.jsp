
<%@ include file="/WEB-INF/views/layout/head.jspf" %>
<c:set var="activo" value="productos" />
<%@ include file="/WEB-INF/views/layout/menu.jspf" %>

<section class="vet-page-header">
    <div>
        <h1>Productos e inventario</h1>
        <p>Administra los productos, precios y niveles de stock de la veterinaria.</p>
    </div>

    <button type="button"
            class="vet-btn vet-btn-primary"
            data-bs-toggle="modal"
            data-bs-target="#modalProducto"
            data-id="0">
        <i class="fa-solid fa-plus"></i>
        Nuevo producto
    </button>
</section>

<section class="vet-stats">
    <article class="vet-stat">
        <div>
            <span>Total de productos</span>
            <h3><c:out value="${resumen.totalProductos}" default="0" /></h3>
        </div>
        <span class="vet-stat-icon"><i class="fa-solid fa-boxes-stacked"></i></span>
    </article>

    <article class="vet-stat">
        <div>
            <span>Stock bajo</span>
            <h3><c:out value="${resumen.stockBajo}" default="0" /></h3>
        </div>
        <span class="vet-stat-icon"><i class="fa-solid fa-triangle-exclamation"></i></span>
    </article>

    <article class="vet-stat">
        <div>
            <span>Agotados</span>
            <h3><c:out value="${resumen.agotados}" default="0" /></h3>
        </div>
        <span class="vet-stat-icon"><i class="fa-solid fa-box-open"></i></span>
    </article>

    <article class="vet-stat">
        <div>
            <span>Valor del inventario</span>
<fmt:formatNumber value="${resumen.valorInventario}" type="number" minFractionDigits="2" maxFractionDigits="2" var="valorInventarioFormateado" />
<h3>S/ <c:out value="${valorInventarioFormateado}" default="0.00" /></h3>
        </div>
        <span class="vet-stat-icon"><i class="fa-solid fa-coins"></i></span>
    </article>
</section>

<section class="vet-panel">
    <div class="vet-panel-head">
        <div>
            <h2>Listado de productos</h2>
            <p>Consulta y administra los productos registrados.</p>
        </div>
    </div>

    <div class="table-responsive">
        <table id="tablaProductos" class="table vet-table">
            <thead>
                <tr>
                    <th>Código</th>
                    <th>Producto</th>
                    <th>Unidad</th>
                    <th>Precio compra</th>
                    <th>Precio venta</th>
                    <th>Stock</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                </tr>
            </thead>

            <tbody>
                <c:forEach var="p" items="${productos}">
                    <tr>
                        <td>
                            <span class="text-muted">
                                <c:out value="${p.codigo}" />
                            </span>
                        </td>

                        <td>
                            <strong><c:out value="${p.nombre}" /></strong>
                            <c:if test="${not empty p.descripcion}">
                                <br>
                                <small class="text-muted">
                                    <c:out value="${p.descripcion}" />
                                </small>
                            </c:if>
                        </td>

                        <td><c:out value="${p.unidadMedida}" /></td>

<td>
    <fmt:formatNumber value="${p.precioCompra}" minFractionDigits="2" maxFractionDigits="2" var="precioCompraFormateado" />
    S/ <c:out value="${precioCompraFormateado}" />
</td>

<td>
    <fmt:formatNumber value="${p.precioVenta}" minFractionDigits="2" maxFractionDigits="2" var="precioVentaFormateado" />
    S/ <c:out value="${precioVentaFormateado}" />
</td>

                        <td>
                            <c:out value="${p.stock}" />
                            <small class="text-muted">
                                / mín. <c:out value="${p.stockMinimo}" />
                            </small>
                        </td>

                        <td>
                            <c:choose>
                                <c:when test="${p.stock == 0}">
                                    <span class="badge text-bg-danger">Agotado</span>
                                </c:when>
                                <c:when test="${p.stock <= p.stockMinimo}">
                                    <span class="badge text-bg-warning">Stock bajo</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge text-bg-success">Disponible</span>
                                </c:otherwise>
                            </c:choose>
                        </td>

                        <td>
                            <div class="d-flex gap-2">
                                <button type="button"
                                        class="vet-btn vet-btn-light btn-editar-producto"
                                        title="Editar producto"
                                        data-bs-toggle="modal"
                                        data-bs-target="#modalProducto"
                                        data-id="<c:out value='${p.id}' />"
                                        data-codigo="<c:out value='${p.codigo}' />"
                                        data-nombre="<c:out value='${p.nombre}' />"
                                        data-descripcion="<c:out value='${p.descripcion}' />"
                                        data-unidad-medida="<c:out value='${p.unidadMedida}' />"
                                        data-precio-compra="<c:out value='${p.precioCompra}' />"
                                        data-precio-venta="<c:out value='${p.precioVenta}' />"
                                        data-stock="<c:out value='${p.stock}' />"
                                        data-stock-minimo="<c:out value='${p.stockMinimo}' />">
                                    <i class="fa-solid fa-pen"></i>
                                </button>

                                <form method="post"
                                      action="${ctx}/productos"
                                      class="d-inline form-eliminar-producto">
                                    <input type="hidden" name="_csrf"
                                           value="${sessionScope.csrf}">
                                    <input type="hidden" name="accion" value="eliminar">
                                    <input type="hidden" name="id"
                                           value="<c:out value='${p.id}' />">

                                    <button type="submit"
                                            class="vet-btn vet-btn-light"
                                            title="Eliminar producto">
                                        <i class="fa-solid fa-trash text-danger"></i>
                                    </button>
                                </form>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
</section>

<div class="modal fade" id="modalProducto" tabindex="-1"
     aria-labelledby="tituloModalProducto" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered">
        <div class="modal-content">

            <form method="post"
                  action="${ctx}/productos"
                  autocomplete="off"
                  data-cargando
                  novalidate>

                <div class="modal-header">
                    <div>
                        <h5 class="modal-title" id="tituloModalProducto">
                            Nuevo producto
                        </h5>
                        <p class="text-muted mb-0">
                            Completa los datos del producto.
                        </p>
                    </div>

                    <button type="button" class="btn-close"
                            data-bs-dismiss="modal"
                            aria-label="Cerrar"></button>
                </div>

                <div class="modal-body">
                    <input type="hidden" name="_csrf"
                           value="${sessionScope.csrf}">
                    <input type="hidden" name="accion" value="guardar">
                    <input type="hidden" name="id" id="productoId" value="0">

                    <div class="vet-form-grid">
                        <div class="mb-3">
                            <label for="productoCodigo" class="form-label">
                                Código *
                            </label>
                            <input type="text" class="form-control"
                                   id="productoCodigo" name="codigo"
                                   maxlength="30" required
                                   pattern="[A-Za-z0-9_-]+"
                                   placeholder="Ej. MED-001">
                        </div>

                        <div class="mb-3">
                            <label for="productoNombre" class="form-label">
                                Nombre *
                            </label>
                            <input type="text" class="form-control"
                                   id="productoNombre" name="nombre"
                                   maxlength="150" required
                                   placeholder="Nombre del producto">
                        </div>

                        <div class="mb-3">
                            <label for="productoDescripcion" class="form-label">
                                Descripción
                            </label>
                            <input type="text" class="form-control"
                                   id="productoDescripcion" name="descripcion"
                                   maxlength="255"
                                   placeholder="Descripción opcional">
                        </div>

                        <div class="mb-3">
                            <label for="productoUnidad" class="form-label">
                                Unidad de medida *
                            </label>
                            <select class="form-select" id="productoUnidad"
                                    name="unidadMedida" required>
                                <c:forEach var="u" items="${unidades}">
                                    <option value="<c:out value='${u}' />">
                                        <c:out value="${u}" />
                                    </option>
                                </c:forEach>
                            </select>
                        </div>

                        <div class="mb-3">
                            <label for="productoPrecioCompra" class="form-label">
                                Precio de compra (S/) *
                            </label>
                            <input type="number" class="form-control"
                                   id="productoPrecioCompra" name="precioCompra"
                                   min="0" max="99999999.99" step="0.01"
                                   required placeholder="0.00">
                        </div>

                        <div class="mb-3">
                            <label for="productoPrecioVenta" class="form-label">
                                Precio de venta (S/) *
                            </label>
                            <input type="number" class="form-control"
                                   id="productoPrecioVenta" name="precioVenta"
                                   min="0" max="99999999.99" step="0.01"
                                   required placeholder="0.00">
                        </div>

                        <div class="mb-3">
                            <label for="productoStock" class="form-label">
                                Stock actual *
                            </label>
                            <input type="number" class="form-control"
                                   id="productoStock" name="stock"
                                   min="0" step="1" required placeholder="0">
                        </div>

                        <div class="mb-3">
                            <label for="productoStockMinimo" class="form-label">
                                Stock mínimo *
                            </label>
                            <input type="number" class="form-control"
                                   id="productoStockMinimo" name="stockMinimo"
                                   min="0" step="1" required placeholder="0">
                        </div>
                    </div>

                    <p class="text-muted small mb-0">
                        Los campos marcados con * son obligatorios.
                    </p>
                </div>

                <div class="modal-footer">
                    <button type="button" class="vet-btn vet-btn-light"
                            data-bs-dismiss="modal">
                        Cancelar
                    </button>

                    <button type="submit" class="vet-btn vet-btn-primary">
                        <i class="fa-solid fa-floppy-disk"></i>
                        Guardar producto
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<c:if test="${abrirModal}">
    <span class="d-none" data-modal-autoabrir="modalProducto"></span>
</c:if>

<script>
document.addEventListener("DOMContentLoaded", function () {
    const modal = document.getElementById("modalProducto");

    if (!modal) {
        return;
    }

    modal.addEventListener("show.bs.modal", function (evento) {
        const boton = evento.relatedTarget;
        const formulario = modal.querySelector("form");

        if (!boton || !formulario) {
            return;
        }

        const id = boton.getAttribute("data-id") || "0";
        const esEdicion = id !== "0";

        document.getElementById("tituloModalProducto").textContent =
            esEdicion ? "Editar producto" : "Nuevo producto";

        document.getElementById("productoId").value = id;
        document.getElementById("productoCodigo").value =
            boton.getAttribute("data-codigo") || "";
        document.getElementById("productoNombre").value =
            boton.getAttribute("data-nombre") || "";
        document.getElementById("productoDescripcion").value =
            boton.getAttribute("data-descripcion") || "";
        document.getElementById("productoUnidad").value =
            boton.getAttribute("data-unidad-medida") || "Unidad";
        document.getElementById("productoPrecioCompra").value =
            boton.getAttribute("data-precio-compra") || "";
        document.getElementById("productoPrecioVenta").value =
            boton.getAttribute("data-precio-venta") || "";
        document.getElementById("productoStock").value =
            boton.getAttribute("data-stock") || "0";
        document.getElementById("productoStockMinimo").value =
            boton.getAttribute("data-stock-minimo") || "0";
    });
});

document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll(".form-eliminar-producto").forEach(function (formulario) {
        formulario.addEventListener("submit", function (evento) {
            if (!confirm("¿Seguro que deseas eliminar este producto?")) {
                evento.preventDefault();
            }
        });
    });

    if (window.jQuery && jQuery.fn.DataTable &&
        !jQuery.fn.DataTable.isDataTable("#tablaProductos")) {
        jQuery("#tablaProductos").DataTable({
            language: DATATABLES_ES,
            pageLength: 10,
            lengthMenu: [10, 25, 50, 100],
            responsive: true,
            autoWidth: false
        });
    }
});
</script>

<%@ include file="/WEB-INF/views/layout/scripts.jspf" %>
</WritingBlock>
