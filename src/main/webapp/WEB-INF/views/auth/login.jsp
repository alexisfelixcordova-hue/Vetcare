
<%@ include file="/WEB-INF/views/layout/head.jspf" %>

<div class="vet-login-page">

  <div class="vet-login-art login-pet-background">

    <div class="login-brand">
      <span class="brand-mark">
        <i class="fa-solid fa-paw"></i>
      </span>
      <span>
        <b>VetCare</b>
        <small>GESTIÓN VETERINARIA</small>
      </span>
    </div>

    <div class="login-art-footer">
      © 2026 VetCare · Hecho para cuidar mejor
    </div>

  </div>

  <div class="vet-login-side">
    <div class="vet-login-card">

      <div class="mobile-login-brand">
        <span class="brand-mark">
          <i class="fa-solid fa-paw"></i>
        </span>
        <b>VetCare</b>
      </div>

      <span class="login-welcome">¡Qué bueno verte de nuevo!</span>
      <h2>Inicia sesión</h2>
      <p class="login-description">
        Ingresa tus datos para acceder al panel de tu clínica.
      </p>

      <form method="post" action="${ctx}/login" data-cargando>
        <input type="hidden" name="_csrf" value="${sessionScope.csrf}">

        <div class="login-field">
          <label for="usuario">Usuario</label>
          <div class="login-input-wrap">
            <i class="fa-regular fa-user"></i>
            <input type="text" id="usuario" name="usuario"
                   placeholder="Tu nombre de usuario"
                   required autofocus>
          </div>
        </div>

        <div class="login-field">
          <label for="password">Contraseña</label>
          <div class="login-input-wrap">
            <i class="fa-solid fa-lock"></i>
            <input type="password" id="password" name="password"
                   placeholder="Ingresa tu contraseña" required>
            <button type="button" id="togglePassword"
                    aria-label="Mostrar contraseña">
              <i class="fa-regular fa-eye"></i>
            </button>
          </div>
        </div>

        <div class="login-extra">
          <label><input type="checkbox"> <span>Recordarme</span></label>
          <span class="login-secure">
            <i class="fa-solid fa-shield-halved"></i> Acceso seguro
          </span>
        </div>

        <button type="submit" class="vet-login-submit">
          Ingresar al sistema <i class="fa-solid fa-arrow-right"></i>
        </button>
      </form>

      <div class="login-help">
        <i class="fa-regular fa-circle-question"></i>
        ¿Problemas para ingresar? Contacta al administrador.
      </div>

      <div class="login-legal">
        Al continuar, confirmas que tienes autorización para acceder a esta clínica.
      </div>

    </div>
  </div>

</div>

<c:if test="${not empty sessionScope.flashMsg}">
  <div id="flash"
       data-tipo="${sessionScope.flashTipo}"
       data-msg="${fn:escapeXml(sessionScope.flashMsg)}"></div>
  <c:remove var="flashMsg" scope="session" />
  <c:remove var="flashTipo" scope="session" />
</c:if>

<script src="${ctx}/assets/libs/bootstrap/js/bootstrap.bundle.min.js"></script>
<script src="${ctx}/assets/libs/sweetalert2/sweetalert2.all.min.js"></script>
<script src="${ctx}/assets/js/app.js"></script>

<script>
document.getElementById('togglePassword')?.addEventListener('click', function () {
    const input = document.getElementById('password');
    input.type = input.type === 'password' ? 'text' : 'password';
    this.innerHTML = input.type === 'password'
        ? '<i class="fa-regular fa-eye"></i>'
        : '<i class="fa-regular fa-eye-slash"></i>';
});
</script>

</body>
</html>
