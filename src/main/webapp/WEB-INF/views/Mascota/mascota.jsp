<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/layout/head.jspf" %>
<c:set var="activo" value="mascota" />
<%@ include file="/WEB-INF/views/layout/menu.jspf" %>

<div class="vet-page" id="vetApp">
  <section class="vet-view active" data-view="patients">
    <div class="vet-welcome">
      <div>
        <div class="eyebrow">REGISTRO ANIMAL</div>
        <h1>Mascotas</h1>
        <p>Todos los compañeros de cuatro patas de nuestra clínica.</p>
      </div>
      <button class="vet-btn vet-btn-primary" data-open-modal="patient"><i class="fa-solid fa-plus"></i> Registrar mascota</button>
    </div>

    <div class="vet-toolbar">
      <div class="vet-search">
        <i class="fa-solid fa-magnifying-glass"></i>
        <input placeholder="Buscar por nombre, especie o propietario..." data-table-search="patients">
      </div>
    </div>

    <div class="vet-panel table-panel">
      <div class="vet-panel-head">
        <div>
          <h2>Directorio de mascotas</h2>
          <p>Listado de animales registrados</p>
        </div>
      </div>
      <div class="table-responsive">
        <table class="vet-table" data-table="patients">
          <thead>
            <tr>
              <th>Mascota</th>
              <th>Especie / Raza</th>
              <th>Sexo</th>
              <th>Edad</th>
              <th>Peso</th>
              <th>Propietario</th>
              <th>Estado</th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="m" items="${listaMascotas}">
              <tr>
                <td><span class="table-pet">🐾 <b>${m.nombre}</b><small>MAS-00${m.idMascota}</small></span></td>
                <td>${m.especie}<small class="cell-sub">${m.raza}</small></td>
                <td>${m.sexo}</td>
                <td>${m.edad}</td>
                <td>${m.peso} kg</td>
                <td>${m.nombreCliente}</td>
                <td><span class="status status-green">${m.estado}</span></td>
              </tr>
            </c:forEach>
            <c:if test="${empty listaMascotas}">
              <tr>
                <td colspan="7" style="text-align: center; padding: 20px;">No hay mascotas registradas en la base de datos.</td>
              </tr>
            </c:if>
          </tbody>
        </table>
      </div>
    </div>
  </section>
</div>

<%@ include file="/WEB-INF/views/layout/scripts.jspf" %>