<%@ include file="/WEB-INF/views/layout/head.jspf" %>
<c:set var="activo" value="dashboard" />
<%@ include file="/WEB-INF/views/layout/menu.jspf" %>

<div class="vet-page" id="vetApp">
  <section class="vet-view active" data-view="dashboard">
    <div class="vet-welcome">
      <div>
        <div class="eyebrow">MIÉRCOLES, 15 DE OCTUBRE DE 2026</div>
        <h1>¡Hola, equipo VetCare xd wqdwqdqwdwq<span>🐾</span></h1>
        <p>Esto es lo que está pasando hoy en tu clínica.</p>
      </div>
      <button class="vet-btn vet-btn-primary" data-open-modal="appointment"><i class="fa-solid fa-plus"></i> Nueva cita</button>
    </div>

    <div class="vet-stats">
      <article class="vet-stat"><div class="vet-stat-top"><span class="vet-stat-icon blue"><i class="fa-solid fa-calendar-check"></i></span><span class="vet-trend">↗ 12%</span></div><div class="vet-stat-value">24</div><div class="vet-stat-label">Citas para hoy</div><div class="vet-stat-foot">8 por atender</div></article>
      <article class="vet-stat"><div class="vet-stat-top"><span class="vet-stat-icon mint"><i class="fa-solid fa-paw"></i></span><span class="vet-trend">↗ 8%</span></div><div class="vet-stat-value">386</div><div class="vet-stat-label">Pacientes registrados</div><div class="vet-stat-foot">18 nuevos este mes</div></article>
      <article class="vet-stat"><div class="vet-stat-top"><span class="vet-stat-icon violet"><i class="fa-solid fa-users"></i></span><span class="vet-trend">↗ 5%</span></div><div class="vet-stat-value">294</div><div class="vet-stat-label">Clientes registrados</div><div class="vet-stat-foot">12 nuevos este mes</div></article>
      <article class="vet-stat"><div class="vet-stat-top"><span class="vet-stat-icon amber"><i class="fa-solid fa-wallet"></i></span><span class="vet-trend">↗ 9%</span></div><div class="vet-stat-value">S/ 2,840</div><div class="vet-stat-label">Ingresos del mes</div><div class="vet-stat-foot">Actualizado al día de hoy</div></article>
    </div>

    <div class="vet-grid-main">
      <article class="vet-panel">
        <div class="vet-panel-head"><div><h2>Resumen de actividad</h2><p>Ingresos y citas de esta semana</p></div><select class="vet-select"><option>Esta semana</option><option>Este mes</option></select></div>
        <div class="chart-legend"><span><i class="legend-dot blue-dot"></i> Ingresos</span><span><i class="legend-dot mint-dot"></i> Citas</span></div>
        <div class="fake-chart"><div class="chart-y"><span>S/ 800</span><span>S/ 600</span><span>S/ 400</span><span>S/ 200</span><span>S/ 0</span></div><div class="chart-area"><div class="chart-grid-lines"><i></i><i></i><i></i><i></i><i></i></div><div class="chart-bars"><div class="bar-pair"><b style="height:47%"></b><i style="height:32%"></i><small>Lun</small></div><div class="bar-pair"><b style="height:66%"></b><i style="height:48%"></i><small>Mar</small></div><div class="bar-pair"><b style="height:53%"></b><i style="height:39%"></i><small>Mié</small></div><div class="bar-pair"><b style="height:83%"></b><i style="height:62%"></i><small>Jue</small></div><div class="bar-pair"><b style="height:70%"></b><i style="height:52%"></i><small>Vie</small></div><div class="bar-pair"><b style="height:94%"></b><i style="height:74%"></i><small>Sáb</small></div><div class="bar-pair"><b style="height:43%"></b><i style="height:25%"></i><small>Dom</small></div></div></div></div>
      </article>

      <article class="vet-panel">
        <div class="vet-panel-head"><div><h2>Próximas citas</h2><p>Pacientes que llegan pronto</p></div></div>
        <div class="vet-upcoming"><div class="pet-avatar dog">🐶</div><div class="pet-info"><b>Max</b><span>Canino · Carlos Mendoza</span></div><div class="pet-time"><b>09:30</b><span>Control</span></div></div>
        <div class="vet-upcoming"><div class="pet-avatar cat">🐱</div><div class="pet-info"><b>Luna</b><span>Felino · Andrea Torres</span></div><div class="pet-time"><b>10:00</b><span>Vacunación</span></div></div>
        <div class="vet-upcoming"><div class="pet-avatar bunny">🐰</div><div class="pet-info"><b>Copito</b><span>Lagomorfo · Luis Rojas</span></div><div class="pet-time"><b>10:30</b><span>Consulta</span></div></div>
        <div class="vet-upcoming"><div class="pet-avatar dog">🐕</div><div class="pet-info"><b>Rocky</b><span>Canino · María Salas</span></div><div class="pet-time"><b>11:15</b><span>Revisión</span></div></div>
      </article>
    </div>

    <div class="vet-grid-bottom">
      <article class="vet-panel">
        <div class="vet-panel-head"><div><h2>Actividad reciente</h2><p>Últimos movimientos de la clínica</p></div></div>
        <div class="vet-activity"><span class="activity-icon green"><i class="fa-solid fa-check"></i></span><div><b>Pago registrado</b><p>Consulta general · Max</p></div><time>Hace 8 min</time><strong>S/ 80.00</strong></div>
        <div class="vet-activity"><span class="activity-icon blue"><i class="fa-solid fa-calendar-plus"></i></span><div><b>Nueva cita agendada</b><p>Vacunación · Luna</p></div><time>Hace 25 min</time><strong>—</strong></div>
        <div class="vet-activity"><span class="activity-icon purple"><i class="fa-solid fa-user-plus"></i></span><div><b>Nuevo cliente</b><p>Andrea Torres</p></div><time>Hace 1 h</time><strong>—</strong></div>
      </article>

      <article class="vet-panel">
        <div class="vet-panel-head"><div><h2>Servicios populares</h2><p>Distribución de atenciones</p></div></div>
        <div class="service-progress"><div><span>Consulta general</span><b>42%</b></div><div class="progress-track"><i style="width:42%;background:#5278e8"></i></div></div>
        <div class="service-progress"><div><span>Vacunación</span><b>28%</b></div><div class="progress-track"><i style="width:28%;background:#38b99b"></i></div></div>
        <div class="service-progress"><div><span>Baño y grooming</span><b>18%</b></div><div class="progress-track"><i style="width:18%;background:#a18be8"></i></div></div>
        <div class="service-progress"><div><span>Otros servicios</span><b>12%</b></div><div class="progress-track"><i style="width:12%;background:#f0b34d"></i></div></div>
      </article>
    </div>
  </section>
</div>

<%@ include file="/WEB-INF/views/layout/scripts.jspf" %>
wleocminqawdqjhb d qbwdbqdunu