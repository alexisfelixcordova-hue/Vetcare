# Estructura del proyecto — Plantillaweb

Este documento explica **cómo está organizado el proyecto hoy** y **para qué sirve cada
parte**: no es una bitácora de cómo se construyó (eso está en [`MANUAL.md`](MANUAL.md), que
puede tener detalles desactualizados de versiones anteriores) ni una guía de arranque rápido
(eso está en [`README.md`](README.md)). Es el mapa de referencia: cuando no sepas dónde va algo
o qué hace un archivo, empieza por acá.

## Qué es esta plantilla

Una aplicación web Java tradicional (Servlets + JSP, sin frameworks tipo Spring, sin
build de frontend, sin React/Angular) pensada para servir de base a proyectos CRUD sencillos
(gestión de usuarios, catálogos, etc.) y para aprender el patrón MVC "a mano". Cada capa hace
una sola cosa y las páginas se sirven como HTML ya armado por el servidor (no es una SPA — hay
un script que simula navegación sin recarga completa, explicado más abajo, pero el servidor
sigue devolviendo HTML normal en cada petición).

## Stack técnico

| Pieza | Qué es |
|---|---|
| Java 21 (`--release 21` en `pom.xml`) | Lenguaje del backend |
| Jakarta Servlet 6.0 + JSP + JSTL 3.0 | Framework web (lo provee Tomcat 10.1.x, `scope=provided`) |
| SQL Server (driver `mssql-jdbc`) | Base de datos |
| BCrypt (`jbcrypt`) | Hash de contraseñas |
| Bootstrap 5 + Font Awesome + SweetAlert2 + DataTables + SimpleBar | Frontend, todo vendorizado en `assets/libs/` (sin CDN) |
| Maven (`mvnw.cmd`) | Build. Sin plugin de precompilación de JSP: Maven compila el `.java`, pero el `.jsp` recién se compila cuando Tomcat lo despliega — un error de JSP no lo detecta `mvn compile`, solo aparece al desplegar |

No hay pool de terceros (HikariCP, etc.) ni ORM (Hibernate/JPA): el acceso a datos es JDBC
directo con `PreparedStatement`, a propósito, para que se vea exactamente qué SQL corre cada
consulta.

## Mapa de carpetas

```
database/                              Scripts SQL
src/main/java/com/example/plantillaweb/
├── config/     Conexion.java          Único punto de acceso a la base de datos
├── model/                             POJOs (una clase por tabla, solo datos)
├── dao/                               Todo el SQL vive acá
├── controller/                        Un Servlet por módulo/ruta
├── filter/                            AuthFilter (sesión/permisos) + CsrfFilter (token anti-CSRF)
└── util/                              Password (BCrypt) y Flash (mensajes de sesión)
src/main/webapp/
├── assets/
│   ├── css/                           CSS propio, uno por componente (ver tabla más abajo)
│   ├── js/                            JS propio: app.js, navegacion-spa.js, datatables-es.js
│   ├── libs/                          Librerías de terceros vendorizadas (Bootstrap, FontAwesome, SweetAlert2, DataTables, SimpleBar, Inter, jQuery)
│   └── images/                        Avatares y fotos subidas de usuarios
├── index.jsp                          Redirige a /dashboard (no tiene HTML propio)
└── WEB-INF/
    ├── web.xml                        Encoding global, páginas de error, welcome-file, session-timeout
    └── views/
        ├── layout/                    head.jspf + menu.jspf + scripts.jspf (layout común)
        ├── auth/login.jsp
        ├── dashboard/index.jsp
        ├── usuarios/lista.jsp
        ├── personas/lista.jsp
        ├── productos/lista.jsp        (en construcción)
        ├── perfil/index.jsp
        └── error/404.jsp, 500.jsp
```

**Regla fija:** todo el HTML vive dentro de `WEB-INF/views` (nunca es accesible por URL directa;
solo un servlet puede hacer `forward` ahí). El único `.jsp` fuera de `WEB-INF` es `index.jsp`, y
ni siquiera tiene marcado, solo un `redirect`.

## El patrón Modelo-Vista-Controlador (MVC) en este proyecto

Todo el proyecto está organizado siguiendo MVC. No es una librería ni una anotación especial:
es simplemente la regla de **qué carpeta puede hacer qué cosa**, y se respeta a mano en cada
archivo nuevo que se agrega.

| Capa | Carpetas | Su único trabajo | Lo que tiene prohibido |
|---|---|---|---|
| **Modelo** | `model/` + `dao/` | Representar los datos (`model/`) y leerlos/guardarlos en SQL Server (`dao/`) | El DAO nunca genera HTML ni sabe qué servlet lo está llamando |
| **Vista** | `WEB-INF/views/**/*.jsp` | Mostrar los datos que le da el servlet (con `${...}`, `<c:forEach>`, etc.) | La vista nunca escribe SQL ni decide reglas de negocio (ej. "el DNI debe tener 8 dígitos" se valida en el servlet, no en el JSP) |
| **Controlador** | `controller/*Servlet.java` | Recibir la petición HTTP, pedirle los datos al Modelo, decidir qué Vista mostrar (o a dónde redirigir) | El servlet nunca escribe una línea de HTML ni una consulta SQL directamente |

En términos estrictos de MVC, el DAO es la parte del **Modelo** que sabe hablar con la base de
datos (a veces se lo llama aparte "capa de persistencia", pero acá vive junto al modelo porque
un proyecto de este tamaño no necesita más separación que esa).

### Un pedido de punta a punta: `GET /personas`

1. El navegador pide `/personas`. Pasa por `AuthFilter` (¿hay sesión?) y `CsrfFilter` (en un
   GET no hace nada, solo aplica a POST).
2. Llega a **`PersonaServlet.doGet()`** (el Controlador). No sabe nada de SQL ni de HTML: solo
   le pide la lista al **`PersonaDAO`** (el Modelo).
3. `PersonaDAO.listar(...)` arma un `PreparedStatement`, ejecuta el `SELECT` y devuelve una
   `List<Persona>` — objetos Java planos, nada de HTML todavía.
4. El servlet guarda esa lista en el `request` (`request.setAttribute("personas", ...)`) y hace
   `forward` a **`personas/lista.jsp`** (la Vista).
5. El JSP recorre `${personas}` con `<c:forEach>` y arma las filas `<tr>` — es la única capa que
   sabe que existe un `<table>`, un `<div class="modal">`, clases CSS, etc.
6. Tomcat manda el HTML final armado al navegador.

### Y un pedido que modifica datos: `POST /personas` (crear)

1. El formulario del modal manda `nombre`, `apellido`, `dni`, `telefono` + el token `_csrf`.
2. `CsrfFilter` valida el token. Si no coincide, corta acá con 403 — ni siquiera llega al
   servlet.
3. **`PersonaServlet.guardar()`** (Controlador) lee los parámetros, arma un objeto `Persona` y
   **valida las reglas de negocio ahí mismo** (campos obligatorios, formato del DNI, que no
   exista otro con el mismo DNI). La Vista no valida nada; el Modelo (DAO) tampoco — esa
   decisión es del Controlador.
4. Si la validación pasa, llama a `PersonaDAO.insertar(p)` (Modelo) y hace
   `response.sendRedirect("/personas")` — nunca un `forward` después de modificar datos
   (**Post/Redirect/Get**: evita que recargar la página F5 vuelva a mandar el mismo formulario).
5. Esa nueva petición GET arranca de nuevo el flujo de la sección anterior, y ahora la lista
   incluye a la persona recién creada.

### Por qué importa mantener esta separación

- **Cada bug tiene un lugar obvio donde buscarlo.** Si un dato sale mal formateado, el problema
  está en la Vista. Si un dato guardado está mal, está en el Modelo/DAO. Si la app deja pasar
  algo que no debería, está en el Controlador. Sin esta regla, un mismo archivo podría mezclar
  las tres cosas y cualquier bug obliga a leer todo de punta a punta para encontrarlo.
- **Se puede cambiar una capa sin tocar las otras dos.** Rediseñar `personas/lista.jsp` (como
  pasó varias veces en este proyecto: sidebar, colores de botones, columnas de la tabla) nunca
  requirió tocar `PersonaDAO` ni `PersonaServlet`. Cambiar una consulta SQL en el DAO no rompe
  la vista. Esto es lo que hace que la plantilla sea copiable: agregar "Productos" fue posible
  siguiendo la misma receta de 6 pasos exactamente porque Usuarios/Personas ya respetaban la
  separación — si no la respetaran, "copiar el patrón" no funcionaría.
- **Seguridad centralizada.** Como *todo* el SQL vive en el DAO y *siempre* usa
  `PreparedStatement`, hay un solo lugar donde revisar que no haya inyección SQL. Como *toda* la
  salida a HTML pasa por la Vista con `<c:out>`/`fn:escapeXml`, hay un solo lugar donde revisar
  que no haya XSS. Si el HTML se armara a mano dentro de los servlets (concatenando Strings),
  cada desarrollador tendría que acordarse de escapar cada dato, en cada lugar, cada vez.
- **Le da a la plantilla una estructura predecible.** Cualquiera que conozca MVC puede abrir
  este proyecto y saber de antemano dónde va cada cosa nueva, sin tener que preguntar — que es
  justamente el objetivo de una plantilla: minimizar las decisiones que hay que tomar para
  agregar un módulo más.

## Backend, capa por capa

### `config/Conexion.java`

El único lugar que sabe conectarse a SQL Server. Host, base, usuario y contraseña son
**constantes dentro de la clase** (no hay `.properties` ni variables de entorno) — simplifica la
lectura para un proyecto de aprendizaje, pero significa que la contraseña real queda en el
código fuente; antes de correr el proyecto hay que abrir este archivo y poner las credenciales
reales del `sa` (o del login que uses).

Mantiene un **pool casero de 10 conexiones** (sin librerías externas): al arrancar la app abre
10 conexiones reales y las guarda en una `BlockingQueue`. `getConnection()` presta una,
envuelta en un **proxy dinámico** (`java.lang.reflect.Proxy`) que intercepta `close()`: en vez
de cerrar la conexión de verdad, la devuelve a la cola. Así todo el resto del código sigue
escribiendo `try (Connection con = Conexion.getConnection()) { ... }` exactamente como si no
hubiera pool.

### `model/`

POJOs planos: campos privados + getters/setters, sin SQL ni lógica de negocio (salvo algún
método de conveniencia trivial, como `Usuario.getNombreCompleto()` o `Usuario.isAdministrador()`,
que solo combinan campos que ya están ahí). Uno por tabla: `Usuario`, `Rol`, `Persona`,
`Producto`.

### `dao/`

Un DAO por tabla. Reglas fijas en todos (`UsuarioDAO`, `PersonaDAO`, `RolDAO`, `ProductoDAO`):

- Todo el SQL vive acá — ni los servlets ni las vistas escriben una consulta.
- Siempre `PreparedStatement` con `?`, nunca se concatena un valor dentro del SQL.
- `try-with-resources` para `Connection`/`PreparedStatement`/`ResultSet`.
- Un método privado `mapear(ResultSet)` arma el objeto del modelo desde una fila.
- **Baja lógica**: `eliminar(id)` hace `UPDATE ... SET estado = 0`, nunca `DELETE` — la fila
  queda, solo se marca inactiva (excepto un caso puntual en `UsuarioDAO.eliminarDefinitivo()`,
  ver más abajo).
- Paginación con `OFFSET ? ROWS FETCH NEXT ? ROWS ONLY` (sintaxis SQL Server). En la práctica hoy
  se pide una sola "página" gigante (`porPagina = 5000`) y es **DataTables**, en el navegador,
  quien resuelve buscador/orden/paginado sobre esa lista completa — no hay ida y vuelta al
  servidor al cambiar de página o buscar.
- `listar()` y `contar()` **deben compartir el mismo `WHERE`**: si no, el total de páginas que
  calcula el servlet no coincide con lo que realmente devuelve `listar()`.

### `controller/` (servlets)

Un servlet por ruta/módulo, todos con el mismo patrón:

- `@WebServlet(name = "...", value = "/ruta")` — sin nada en `web.xml`, todo por anotación.
- `doGet` solo **muestra** pantallas (nunca modifica datos).
- `doPost` lee un parámetro `accion` (`"guardar"` / `"eliminar"`) y decide a qué método interno
  llamar. Nunca hay lógica de negocio en el `doPost` en sí, delega enseguida.
- Todo `SQLException` se atrapa, se loguea (`java.util.logging.Logger`), se manda un mensaje con
  `Flash.error(...)` y se redirige — el usuario nunca ve un stacktrace en pantalla (el 500
  genérico solo salta si la excepción **no** es `SQLException`, por ejemplo un error de sintaxis
  dentro del propio JSP al renderizar).
- **Post/Redirect/Get**: todo POST que modifica termina en `response.sendRedirect(...)`, nunca
  en `forward`, salvo un caso: si `guardar()` falla una validación, en vez de redirigir se
  vuelve a mostrar la lista (`forward`) con el objeto ya completado y `abrirModal = true`, para
  que el modal se reabra solo con lo que el usuario ya había escrito (ver más abajo, "Patrón
  CRUD con modal").

Servlets actuales:

| Servlet | Ruta | Qué hace |
|---|---|---|
| `LoginServlet` | `/login` | GET: formulario (o redirige a `/dashboard` si ya hay sesión). POST: valida usuario/clave, `changeSessionId()` (anti session-fixation), guarda `usuario` en sesión |
| `LogoutServlet` | `/logout` | Solo POST. Invalida la sesión y redirige a `/login` |
| `DashboardServlet` | `/dashboard` | Totales (usuarios activos, roles) en tarjetas |
| `UsuarioServlet` | `/usuarios` | CRUD completo. Solo para administradores (lo filtra `AuthFilter`) |
| `PersonaServlet` | `/personas` | CRUD completo, sin restricción de rol |
| `ProductoServlet` | `/productos` | CRUD completo (en construcción) |
| `PerfilServlet` | `/perfil` | Cada usuario ve sus propios datos y cambia su contraseña |

**Caso especial en `UsuarioServlet`**: a diferencia de Personas/Productos (baja lógica simple),
eliminar un usuario tiene dos pasos — si está activo, el botón "Desactivar" lo pone inactivo
(`estado = 0`); si ya estaba inactivo, el mismo tipo de botón ("Eliminar definitivamente") hace
un `DELETE` real (`UsuarioDAO.eliminarDefinitivo`). Los dos botones tienen colores distintos a
propósito (ver "Convención de colores" más abajo) para que no se confundan a simple vista.

### `filter/`

Dos filtros `@WebFilter("/*")`, pasan por **toda** petición:

- **`AuthFilter`**: si la ruta no es pública (`/login`, `/assets/*`) y no hay `usuario` en
  sesión, redirige a `/login`. Si la ruta es `/usuarios` y el usuario no es administrador,
  redirige al dashboard con un error. También pone cabeceras de caché distintas según el tipo de
  ruta: `Cache-Control: no-cache` (revalida, no descarga a ciegas) para `/assets/*`, y
  `no-cache, no-store, must-revalidate` para todo lo demás (para que el botón "atrás" del
  navegador nunca muestre una página privada después de cerrar sesión, ni un `/login` viejo con
  un token CSRF que ya no es válido).
- **`CsrfFilter`**: genera un token aleatorio por sesión (`SecureRandom`, guardado como
  `csrf`), excluyendo `/assets/*` (para no crear una `HttpSession` por cada CSS/JS/imagen que
  pide el navegador). En todo POST exige un campo oculto `_csrf` con ese mismo valor; si no
  coincide, responde 403. Todos los formularios llevan
  `<input type="hidden" name="_csrf" value="${sessionScope.csrf}">`.

### `util/`

- **`Password`**: `encriptar`/`verificar` con BCrypt. Tiene un `main()` que imprime el hash de
  `"admin123"`, para generar el valor que se pega a mano en el script SQL (nunca se escribe una
  contraseña real en texto plano, ni en el script).
- **`Flash`**: guarda un mensaje (`exito`/`error`/`advertencia`) en sesión antes de un redirect.
  La vista lo lee una sola vez (vía el `<div id="flash">` que arma `scripts.jspf`) y lo borra —
  así sobrevive exactamente una recarga, ni una más.

## Frontend: layout y vistas

### El esqueleto de cada página privada

Cada JSP de contenido repite siempre el mismo patrón de tres `include`:

```jsp
<%@ include file="/WEB-INF/views/layout/head.jspf" %>
<c:set var="activo" value="productos" />
<%@ include file="/WEB-INF/views/layout/menu.jspf" %>

... el contenido propio de la página ...

<%@ include file="/WEB-INF/views/layout/scripts.jspf" %>
```

- **`head.jspf`**: abre `<html><head>`, declara los taglibs `c`/`fn` (el `<c:set>` de arriba
  tiene que ir *después* de este include, porque es quien declara el prefijo `c`), carga el CSS
  y pinta el tema/colapso del sidebar guardado en `localStorage` **antes** de que el resto de la
  página se dibuje (para evitar el parpadeo claro/oscuro).
- **`menu.jspf`**: arma `.app-shell` completo — el sidebar (fijo, oscuro, con su propio scroll
  vía SimpleBar) y `.main-wrapper` (topbar sticky + `<main class="contenido"><div
  class="page-content">`, que queda **abierto**: lo cierra `scripts.jspf`). Recibe la variable
  `activo` para resaltar la opción correcta del menú. También trae el `<script>` que maneja
  tema/pantalla-completa/colapso del sidebar y el comportamiento del submenú flotante (ver
  abajo).
- **`scripts.jspf`**: cierra los `<div>` que dejó abiertos `menu.jspf`, pinta el footer, muestra
  el `<div id="flash">` con el mensaje pendiente (si hay uno) y carga los `<script src>` de las
  librerías + `app.js` + `navegacion-spa.js`.

`login.jsp` y las páginas de error (`404.jsp`/`500.jsp`) **no** usan `menu.jspf` (pueden
mostrarse sin sesión iniciada): solo incluyen `head.jspf`, pintan su contenido dentro de
`.pagina-login` y cierran `</body></html>` a mano.

### El sidebar

- Estructura: `.app-shell > (.sidebar + .main-wrapper)`. El sidebar es `position: fixed` a
  partir de 992px (siempre visible), y por debajo de eso es el offcanvas nativo de Bootstrap
  (overlay que se abre con el botón hamburguesa).
- Colapsado ("riel de íconos", botón `#btnSidebar` en la topbar): el sidebar no desaparece, baja
  de `260px` a `78px` (`--sidebar-collapsed-width`) y solo quedan los íconos. El estado se
  guarda en `localStorage` y se re-aplica en `head.jspf` antes de pintar, para que no parpadee.
- **Submenú de ejemplo ("Gestión")**: agrupa Usuarios/Personas/Productos bajo un ítem
  colapsable, usando el `Collapse` de Bootstrap (sin JS propio) cuando el sidebar está expandido.
  Cuando está colapsado (riel de íconos), el mismo submenú aparece como **panel flotante** junto
  al ícono al pasar el mouse — con `position: fixed` (no `absolute`), porque tanto `.sidebar`
  como su contenedor de scroll recortarían cualquier cosa posicionada relativa a ellos; el JS en
  `menu.jspf` calcula la posición real del ícono (`getBoundingClientRect`) para ubicarlo.
- El color de fondo del sidebar es fijo (oscuro) en los dos temas — es la franja de la marca
  (arriba del todo) la única parte que cambia con el tema claro/oscuro, porque está a la misma
  altura que la topbar y usa las mismas variables de "superficie" que ella.

### Patrón CRUD con modal (Usuarios, Personas, Productos)

No hay una página de formulario aparte para crear/editar. Cada `lista.jsp` tiene:

1. Un botón **"Nuevo"** que solo abre el modal vacío (no llama al servidor).
2. Un botón **"Editar"** por fila, con los datos del registro en atributos `data-*`
   (`data-nombre="..."`, `data-precio="..."`, etc.). Un manejador genérico en `app.js` (delegado
   en `document`, no hay que escribir JS por campo nuevo) copia esos `data-*` a los
   `<input name="...">` del modal que tengan el mismo nombre.
3. Si `guardar()` falla una validación de servidor, el servlet vuelve a mostrar la lista con
   `abrirModal = true`; un marcador (`<span data-modal-autoabrir="...">`) hace que `app.js`
   reabra el modal solo, con los datos que ya se habían escrito.
4. Cada fila de acción tiene "Editar" y una acción de baja (ver "Convención de colores").

### Tablas (DataTables)

Cada `lista.jsp` inicializa su tabla así, con un detalle importante:

```js
function inicializarTablaX() {
    $("#tablaX").DataTable({ ... });
}
if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", inicializarTablaX);
} else {
    inicializarTablaX();
}
```

El `if` existe por dos motivos que conviven: en la carga normal de la página, jQuery/DataTables
cargan *después* en el HTML (en `scripts.jspf`), así que hace falta esperar a
`DOMContentLoaded`. Pero si la página llega por navegación sin recarga (ver siguiente sección),
ese evento ya pasó hace rato y nunca volvería a disparar — por eso, si el documento ya está
"complete", se llama la función directo. `autoWidth: false` + `table.dataTable { width: 100%
!important; }` (en `datatables.css`) evitan que la tabla quede angosta hasta que algo dispare un
recálculo manual.

### Navegación sin recarga (`assets/js/navegacion-spa.js`)

Intercepta los clics en enlaces internos (sidebar, breadcrumb, "Mi perfil"...), trae la página
con `fetch()` y reemplaza solo `.page-content` — el sidebar y la topbar nunca se destruyen ni se
vuelven a dibujar, así que no hay parpadeo de recarga completa. El servidor no sabe que esto
existe: sigue devolviendo el mismo HTML de siempre.

Piezas clave:

- `esNavegable(enlace)` descarta lo que no es navegación real: enlaces con `data-bs-toggle`
  (submenú, dropdowns, modales), `target="_blank"`, `download`, otro dominio, o que solo cambian
  el `#hash`.
- Los `<script>` que trae el HTML nuevo no se ejecutan solos al hacer `innerHTML = ...` (el
  navegador lo bloquea a propósito); por eso se reemplaza cada uno por un `<script>` creado de
  cero, que sí se ejecuta.
- El sidebar se resalta comparando el `pathname` de cada enlace del menú contra la URL nueva —
  genérico, así que un módulo que agregues queda resaltado solo, sin tocar este archivo.
- Si el `fetch` falla, o la respuesta no tiene `.page-content` (ej. la sesión venció y el
  servidor mandó a `/login`), cae a una navegación real (`window.location.href = url`): nunca
  queda "roto".
- Barra de progreso (`.barra-carga`, fija arriba del todo): solo se muestra si la carga tarda
  más de 150ms, para que una respuesta rápida de localhost no la deje "cortada" a medio crecer.

### Convención de colores en botones de acción

Se armó pensando en qué tan grave es la acción, no solo en qué se ve bonito:

- **`btn-primary`** (Guardar, Nuevo): azul sólido. *Nota:* este kit define
  `--vz-btn-bg: #0c5ff6` para `.btn-primary` específicamente, distinto del `--primary` (`#405189`,
  indigo) que usa el resto de la app (link activo del sidebar, foco de teclado, SweetAlert2) — es
  una decisión de branding puntual para botones de acción, no un bug.
- **`btn-outline-secondary`** (Cancelar): el Bootstrap de este kit (Velzon) define
  `--vz-secondary` como **azul**, no gris — se sobreescribió en `buttons.css` para que sea el
  gris neutro que una acción secundaria necesita (si no, competía visualmente con "Guardar").
- **`btn-accion-danger`** (rojo): reservado para lo **irreversible** — borrado definitivo.
- **`btn-accion-warning`** (ámbar): acciones **reversibles** — desactivar (no borra nada, solo
  cambia `estado`).
- El diálogo de confirmación (`Alerta.confirmar(mensaje, tipo)` en `app.js`, disparado por
  `data-confirmar` + `data-confirmar-tipo="peligro"|"advertencia"` en el `<form>`) usa el mismo
  color que el botón que lo abrió, para reforzar — no contradecir — la señal visual que ya vio
  el usuario.

### Hoja de ruta del CSS (`assets/css/`, importado en orden desde `app.css`)

| Archivo | Qué estiliza |
|---|---|
| `variables.css` | Tokens de diseño (`--primary`, `--radius-*`, `--sidebar-width`...) y el tema oscuro (redefine las mismas variables bajo `body.tema-oscuro`) |
| `fonts.css` | `@font-face` de Inter (vendorizada) |
| `reset.css`, `base.css` | Reset mínimo + tipografía/colores base del `body` |
| `layout.css` | `.app-shell`, `.main-wrapper`, `.contenido`, `.footer` |
| `sidebar.css` | Todo el sidebar: marca, navegación, submenú, colapsado, riel de íconos |
| `navbar.css` | La topbar (sticky) y sus botones de ícono |
| `dashboard.css` | Tarjetas de estadísticas del dashboard |
| `buttons.css` | `.btn`, variantes de `.btn-accion-*`, override de `.btn-outline-secondary` |
| `cards.css`, `tables.css`, `datatables.css`, `forms.css`, `badges.css`, `alerts.css`, `modal.css`, `dropdown.css`, `pagination.css` | Un componente de Bootstrap/DataTables por archivo |
| `utilities.css` | Ayudas sueltas que no ameritan archivo propio |
| `responsive.css` | Ajustes puntuales por breakpoint, al final para poder sobreescribir todo lo anterior |
| `app.css` | Solo `@import` de todos los anteriores en orden, + un par de reglas sueltas (`.pagina-login`, `.barra-carga`) |

## Base de datos

`Conexion.java` apunta hoy a una base llamada **`prueba`** (no `PlantillaWeb`, que es lo que
crea `database/plantillaweb.sql` — si vas a usar ese script, actualiza `DATABASE` en
`Conexion.java`, o viceversa). Tablas: `rol`, `usuario` (con `id_rol` como FK), `persona`,
`producto`. Todas con `estado` (bit) para baja lógica en vez de `DELETE`.

## Cómo agregar un módulo nuevo (receta corta)

1. **Tabla** en SQL Server: `id INT IDENTITY PK`, tus columnas, `estado BIT`.
2. **Modelo**: `model/Producto.java` — copiar la forma de `Persona.java`.
3. **DAO**: `dao/ProductoDAO.java` — copiar `PersonaDAO.java` y adaptar el SQL
   (`listar`/`contar`/`obtenerPorId`/`insertar`/`actualizar`/`eliminar`). `listar()` y `contar()`
   con el mismo `WHERE`.
4. **Servlet**: `controller/ProductoServlet.java` — copiar `PersonaServlet.java`.
5. **Vista**: `WEB-INF/views/productos/lista.jsp` — copiar `personas/lista.jsp` (los 3 `include`,
   la tabla, el modal único, el init de DataTables con el patrón `readyState`).
6. **Menú**: agregar el `<li>` en `menu.jspf` (o dentro del submenú "Gestión"), comparando
   `${activo == 'productos'}`.
7. Si el módulo es solo para administradores, agregar su ruta al `if` de `AuthFilter.java`
   (igual que `/usuarios`).

## Ejemplo completo, paso a paso: agregar el módulo "Docentes"

La receta corta de arriba dice *qué* hacer. Acá está el *cómo*, con código real, tabla por
tabla, campo por campo — para poder explicarlo o seguirlo sin adivinar nada. Se arma un módulo
nuevo llamado **Docentes**, con esta forma:

| Campo | Tipo | Regla |
|---|---|---|
| `id_docente` | `INT IDENTITY` | Llave primaria |
| `nombres` | `VARCHAR(100)` | Obligatorio |
| `apellidos` | `VARCHAR(100)` | Obligatorio |
| `dni` | `VARCHAR(8)` | Obligatorio, único, 8 dígitos |
| `especialidad` | `VARCHAR(100)` | Obligatorio |
| `estado` | `BIT` | Baja lógica (1 = activo, 0 = inactivo) |

Es, a propósito, la misma forma que `persona` — así el ejemplo se puede seguir en paralelo con
un módulo que ya existe y funciona en el proyecto.

### Paso 1 — La tabla en SQL Server

Se agrega al script de base de datos (o se corre suelto), siguiendo el mismo estilo "si no
existe, créala" que ya usa el resto del script:

```sql
IF OBJECT_ID(N'dbo.docente', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.docente (
        id_docente    INT IDENTITY(1,1) PRIMARY KEY,
        nombres       VARCHAR(100) NOT NULL,
        apellidos     VARCHAR(100) NOT NULL,
        dni           VARCHAR(8)   NOT NULL UNIQUE,
        especialidad  VARCHAR(100) NULL,
        estado        BIT          NOT NULL DEFAULT 1
    );
END
GO
```

Por qué `estado BIT NOT NULL DEFAULT 1`: es la misma convención de baja lógica que usan
`persona`, `usuario` y `producto` — nunca se hace `DELETE`, "eliminar" en la interfaz solo pone
`estado = 0`, y las consultas de listado siempre filtran `WHERE estado = 1`.

### Paso 2 — El Modelo: `model/Docente.java`

Un POJO plano. Nombres de campos en **camelCase** (`idDocente`, no `id_docente`) aunque la
columna de SQL Server use snake_case — esa traducción la hace el DAO al leer/escribir, el
modelo no sabe nada de SQL:

```java
package com.example.plantillaweb.model;

/** Representa una fila de la tabla docente. Solo datos, sin lógica de negocio ni SQL. */
public class Docente {

    private int idDocente;
    private String nombres;
    private String apellidos;
    private String dni;
    private String especialidad;
    private boolean estado;

    public int getIdDocente() { return idDocente; }
    public void setIdDocente(int idDocente) { this.idDocente = idDocente; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }

    public boolean isEstado() { return estado; }
    public void setEstado(boolean estado) { this.estado = estado; }

    /** Nombres + apellidos juntos, para mostrar en pantalla (ej. en el mensaje de confirmación al eliminar). */
    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }
}
```

### Paso 3 — El Modelo (persistencia): `dao/DocenteDAO.java`

Los seis métodos de siempre. Fijate que `listar()` y `contar()` comparten **exactamente** el
mismo `WHERE` — si se escriben distinto, el total de páginas que calcula el servlet no va a
coincidir con lo que realmente se ve en la tabla:

```java
package com.example.plantillaweb.dao;

import com.example.plantillaweb.config.Conexion;
import com.example.plantillaweb.model.Docente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Todo el SQL de la tabla docente vive aquí. El servlet nunca escribe SQL directamente. */
public class DocenteDAO {

    /** Lista paginada de docentes activos, filtrando por nombres/apellidos/DNI. */
    public List<Docente> listar(String buscar, int pagina, int porPagina) throws SQLException {
        String sql = "SELECT id_docente, nombres, apellidos, dni, especialidad, estado " +
                "FROM dbo.docente " +
                "WHERE estado = 1 " +
                "  AND (nombres LIKE ? OR apellidos LIKE ? OR dni LIKE ?) " +
                "ORDER BY id_docente " +
                "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        String patron = "%" + (buscar == null ? "" : buscar.trim()) + "%";
        int desde = Math.max(pagina - 1, 0) * porPagina;

        List<Docente> docentes = new ArrayList<>();
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, patron);
            ps.setString(2, patron);
            ps.setString(3, patron);
            ps.setInt(4, desde);
            ps.setInt(5, porPagina);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    docentes.add(mapear(rs));
                }
            }
        }
        return docentes;
    }

    /** Cuenta cuántos docentes activos cumplen el mismo filtro que listar(), para la paginación. */
    public int contar(String buscar) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.docente " +
                "WHERE estado = 1 AND (nombres LIKE ? OR apellidos LIKE ? OR dni LIKE ?)";
        String patron = "%" + (buscar == null ? "" : buscar.trim()) + "%";

        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, patron);
            ps.setString(2, patron);
            ps.setString(3, patron);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Inserta un docente nuevo y deja el id generado en el propio objeto. */
    public void insertar(Docente d) throws SQLException {
        String sql = "INSERT INTO dbo.docente (nombres, apellidos, dni, especialidad, estado) " +
                "VALUES (?, ?, ?, ?, 1)";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getNombres());
            ps.setString(2, d.getApellidos());
            ps.setString(3, d.getDni());
            ps.setString(4, d.getEspecialidad());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    d.setIdDocente(rs.getInt(1));
                }
            }
        }
    }

    public void actualizar(Docente d) throws SQLException {
        String sql = "UPDATE dbo.docente SET nombres = ?, apellidos = ?, dni = ?, especialidad = ? " +
                "WHERE id_docente = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, d.getNombres());
            ps.setString(2, d.getApellidos());
            ps.setString(3, d.getDni());
            ps.setString(4, d.getEspecialidad());
            ps.setInt(5, d.getIdDocente());
            ps.executeUpdate();
        }
    }

    /** Baja lógica: nunca se borra la fila, solo se marca estado = 0. */
    public void eliminar(int id) throws SQLException {
        String sql = "UPDATE dbo.docente SET estado = 0 WHERE id_docente = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** true si el DNI ya está usado por OTRO docente (idExcluir = el propio id al editar, 0 al crear). */
    public boolean existeDni(String dni, int idExcluir) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.docente WHERE dni = ? AND id_docente <> ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, dni);
            ps.setInt(2, idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private Docente mapear(ResultSet rs) throws SQLException {
        Docente d = new Docente();
        d.setIdDocente(rs.getInt("id_docente"));
        d.setNombres(rs.getString("nombres"));
        d.setApellidos(rs.getString("apellidos"));
        d.setDni(rs.getString("dni"));
        d.setEspecialidad(rs.getString("especialidad"));
        d.setEstado(rs.getBoolean("estado"));
        return d;
    }
}
```

### Paso 4 — El Controlador: `controller/DocenteServlet.java`

Mismo esqueleto que `PersonaServlet`: `doGet` muestra, `doPost` decide entre `guardar`/`eliminar`
según el parámetro `accion`, y las validaciones de negocio (campos obligatorios, formato del
DNI, DNI único) viven acá — ni en el DAO ni en el JSP:

```java
package com.example.plantillaweb.controller;

import com.example.plantillaweb.dao.DocenteDAO;
import com.example.plantillaweb.model.Docente;
import com.example.plantillaweb.util.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** CRUD de docentes. GET solo muestra, POST solo modifica (ver "accion"). */
@WebServlet(name = "docenteServlet", value = "/docentes")
public class DocenteServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(DocenteServlet.class.getName());
    private static final int POR_PAGINA = 5000;

    private final DocenteDAO docenteDAO = new DocenteDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            mostrarLista(request, response, new Docente(), false);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error consultando docentes", e);
            Flash.error(request, "No se pudo cargar la información de docentes.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String accion = request.getParameter("accion");
        try {
            if ("guardar".equals(accion)) {
                guardar(request, response);
            } else if ("eliminar".equals(accion)) {
                eliminar(request, response);
            } else {
                response.sendRedirect(request.getContextPath() + "/docentes");
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error guardando el docente", e);
            Flash.error(request, "Ocurrió un error al guardar los datos.");
            response.sendRedirect(request.getContextPath() + "/docentes");
        }
    }

    private void mostrarLista(HttpServletRequest request, HttpServletResponse response, Docente docente,
            boolean abrirModal) throws SQLException, ServletException, IOException {
        String buscar = request.getParameter("buscar");
        int pagina = Math.max(parseEntero(request.getParameter("pagina"), 1), 1);

        int total = docenteDAO.contar(buscar);
        int totalPaginas = Math.max((int) Math.ceil(total / (double) POR_PAGINA), 1);
        pagina = Math.min(pagina, totalPaginas);

        request.setAttribute("docentes", docenteDAO.listar(buscar, pagina, POR_PAGINA));
        request.setAttribute("buscar", buscar == null ? "" : buscar);
        request.setAttribute("docente", docente);
        request.setAttribute("abrirModal", abrirModal);
        request.getRequestDispatcher("/WEB-INF/views/docentes/lista.jsp").forward(request, response);
    }

    private void guardar(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        int id = parseEntero(request.getParameter("idDocente"), 0);

        Docente d = new Docente();
        d.setIdDocente(id);
        d.setNombres(valor(request.getParameter("nombres")));
        d.setApellidos(valor(request.getParameter("apellidos")));
        d.setDni(valor(request.getParameter("dni")));
        d.setEspecialidad(valor(request.getParameter("especialidad")));

        String error = validar(d);
        if (error != null) {
            Flash.error(request, error);
            mostrarLista(request, response, d, true); // reabre el modal con lo ya escrito
            return;
        }

        if (id == 0) {
            docenteDAO.insertar(d);
            Flash.exito(request, "Docente creado correctamente.");
        } else {
            docenteDAO.actualizar(d);
            Flash.exito(request, "Docente actualizado correctamente.");
        }
        response.sendRedirect(request.getContextPath() + "/docentes"); // Post/Redirect/Get
    }

    private void eliminar(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        int id = parseEntero(request.getParameter("id"), 0);
        docenteDAO.eliminar(id);
        Flash.exito(request, "Docente eliminado correctamente.");
        response.sendRedirect(request.getContextPath() + "/docentes");
    }

    /** Validaciones de servidor. Devuelve el mensaje de error, o null si todo está bien. */
    private String validar(Docente d) throws SQLException {
        if (d.getNombres().isBlank() || d.getApellidos().isBlank() || d.getDni().isBlank()
                || d.getEspecialidad().isBlank()) {
            return "Todos los campos son obligatorios.";
        }
        if (!d.getDni().matches("\\d{8}")) {
            return "El DNI debe tener exactamente 8 dígitos.";
        }
        if (docenteDAO.existeDni(d.getDni(), d.getIdDocente())) {
            return "Ya existe otro docente con ese DNI.";
        }
        return null;
    }

    private String valor(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private int parseEntero(String texto, int porDefecto) {
        try {
            return texto == null ? porDefecto : Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }
}
```

### Paso 5 — La Vista: `WEB-INF/views/docentes/lista.jsp`

Todo en un solo archivo: los 3 `include` del layout, la tabla, el modal único de crear/editar y
el init de DataTables. Los nombres de `id`/clase (`modalDocente`, `tablaDocentes`,
`btnNuevoDocente`, `btn-editar-docente`) siguen el mismo patrón que en `usuarios`/`personas`
para que no se pisen entre módulos:

```jsp
<%@ include file="/WEB-INF/views/layout/head.jspf" %>
<c:set var="activo" value="docentes" />
<c:set var="esNuevo" value="${docente.idDocente == 0}" />
<%@ include file="/WEB-INF/views/layout/menu.jspf" %>

<div class="page-header">
    <div>
        <h3 class="page-title">Docentes</h3>
        <nav aria-label="breadcrumb">
            <ol class="breadcrumb">
                <li class="breadcrumb-item"><a href="${ctx}/dashboard">Inicio</a></li>
                <li class="breadcrumb-item active" aria-current="page">Docentes</li>
            </ol>
        </nav>
    </div>
</div>

<div class="table-card">
    <div class="table-header">
        <span class="card-header-tabla"><i class="fa-solid fa-list text-primary me-2"></i>Lista de docentes</span>
        <button type="button" class="btn btn-primary" id="btnNuevoDocente"
                data-bs-toggle="modal" data-bs-target="#modalDocente">
            <i class="fa-solid fa-plus me-1"></i>Nuevo
        </button>
    </div>
    <div class="table-responsive">
        <table class="table table-hover align-middle mb-0 tabla-plantilla" id="tablaDocentes">
            <thead>
            <tr>
                <th>N°</th>
                <th>Nombres</th>
                <th>Apellidos</th>
                <th>DNI</th>
                <th>Especialidad</th>
                <th>Estado</th>
                <th class="text-end">Acción</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="d" items="${docentes}" varStatus="fila">
                <tr>
                    <td><c:out value="${fila.index + 1}" /></td>
                    <td><c:out value="${d.nombres}" /></td>
                    <td><c:out value="${d.apellidos}" /></td>
                    <td><c:out value="${d.dni}" /></td>
                    <td><c:out value="${d.especialidad}" /></td>
                    <td>
                        <c:choose>
                            <c:when test="${d.estado}">
                                <span class="badge badge-success">Activo</span>
                            </c:when>
                            <c:otherwise>
                                <span class="badge badge-danger">Inactivo</span>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td class="text-end">
                        <button type="button" class="btn btn-accion btn-accion-primary btn-editar-docente"
                                data-bs-toggle="modal" data-bs-target="#modalDocente"
                                data-id-docente="${d.idDocente}"
                                data-nombres="${fn:escapeXml(d.nombres)}"
                                data-apellidos="${fn:escapeXml(d.apellidos)}"
                                data-dni="${d.dni}"
                                data-especialidad="${fn:escapeXml(d.especialidad)}">
                            <i class="fa-solid fa-pen-to-square"></i>
                        </button>
                        <form method="post" action="${ctx}/docentes" class="d-inline"
                              data-confirmar="¿Eliminar a ${fn:escapeXml(d.nombreCompleto)}?"
                              data-confirmar-tipo="peligro">
                            <input type="hidden" name="_csrf" value="${sessionScope.csrf}">
                            <input type="hidden" name="accion" value="eliminar">
                            <input type="hidden" name="id" value="${d.idDocente}">
                            <button type="submit" class="btn btn-accion btn-accion-danger">
                                <i class="fa-solid fa-trash"></i>
                            </button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty docentes}">
                <tr>
                    <td colspan="7">
                        <div class="empty-state">
                            <i class="fa-solid fa-chalkboard-user"></i>
                            <span class="empty-state-titulo">No hay registros</span>
                            <span class="empty-state-texto">Todavía no existen docentes registrados.</span>
                        </div>
                    </td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </div>
</div>

<%-- Un solo modal para crear y editar, igual que en personas/lista.jsp --%>
<div class="modal fade" id="modalDocente" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <%-- autocomplete="off": este modal se reusa para Nuevo y Editar; sin esto el
                 navegador reaparece con lo ultimo escrito en vez de dejarlo vacio. --%>
            <form method="post" action="${ctx}/docentes" autocomplete="off" data-cargando novalidate>
                <div class="modal-header">
                    <h5 class="modal-title" id="modalDocenteTitulo">${esNuevo ? 'Nuevo docente' : 'Editar docente'}</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrf}">
                    <input type="hidden" name="accion" value="guardar">
                    <input type="hidden" name="idDocente" value="${docente.idDocente}">

                    <div class="row g-3">
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="nombres">Nombres</label>
                            <input type="text" class="form-control" id="nombres" name="nombres"
                                   value="${fn:escapeXml(docente.nombres)}" required>
                        </div>
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="apellidos">Apellidos</label>
                            <input type="text" class="form-control" id="apellidos" name="apellidos"
                                   value="${fn:escapeXml(docente.apellidos)}" required>
                        </div>
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="dni">DNI</label>
                            <input type="text" class="form-control" id="dni" name="dni" maxlength="8" pattern="\d{8}"
                                   value="${fn:escapeXml(docente.dni)}" required>
                        </div>
                        <div class="col-12 col-md-6">
                            <label class="form-label" for="especialidad">Especialidad</label>
                            <input type="text" class="form-control" id="especialidad" name="especialidad"
                                   value="${fn:escapeXml(docente.especialidad)}" required>
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
    <span class="d-none" data-modal-autoabrir="modalDocente"></span>
</c:if>

<script>
    (function () {
        var tituloEl = document.getElementById("modalDocenteTitulo");

        document.getElementById("btnNuevoDocente").addEventListener("click", function () {
            tituloEl.textContent = "Nuevo docente";
        });

        document.querySelectorAll(".btn-editar-docente").forEach(function (boton) {
            boton.addEventListener("click", function () {
                tituloEl.textContent = "Editar docente";
            });
        });
    })();
</script>

<%-- DataTables: el servidor trae el listado completo (ver DocenteServlet), asi que el
     buscador, el orden y el paginado de esta tabla los resuelve DataTables en el navegador. --%>
<c:if test="${not empty docentes}">
    <script>
        // El "if" de mas abajo existe por dos motivos: en la carga normal de la pagina hay
        // que esperar a DOMContentLoaded porque jQuery/DataTables cargan mas abajo (en
        // scripts.jspf); pero si esta pagina llega por navegacion sin recarga
        // (assets/js/navegacion-spa.js), ese evento ya paso hace rato y nunca volveria a
        // disparar, asi que ahi se llama la funcion directo.
        function inicializarTablaDocentes() {
            $("#tablaDocentes").DataTable({
                language: DATATABLES_ES,
                pageLength: 10,
                lengthMenu: [10, 25, 50, 100],
                responsive: true,
                autoWidth: false,
                columnDefs: [
                    { orderable: false, targets: [0, 6] },
                    { responsivePriority: 1, targets: 1 },
                    { responsivePriority: 1, targets: 6 },
                    { responsivePriority: 2, targets: 5 },
                    { responsivePriority: 3, targets: 3 },
                    { responsivePriority: 4, targets: 2 },
                    { responsivePriority: 5, targets: 4 },
                    { responsivePriority: 6, targets: 0 }
                ]
            });
        }

        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", inicializarTablaDocentes);
        } else {
            inicializarTablaDocentes();
        }
    </script>
</c:if>

<%@ include file="/WEB-INF/views/layout/scripts.jspf" %>
```

### Paso 6 — Engancharlo al menú: `WEB-INF/views/layout/menu.jspf`

Se agrega un `<li>` más, dentro del mismo `<ul>` de "Navegación" (o dentro del submenú
"Gestión", si se lo quiere agrupar con Usuarios/Personas/Productos):

```jsp
<li class="nav-item">
    <a class="nav-link ${activo == 'docentes' ? 'active' : ''}" href="${ctx}/docentes" title="Docentes">
        <i class="fa-solid fa-chalkboard-user"></i><span>Docentes</span>
    </a>
</li>
```

El resaltado (`class="... active"`) funciona solo porque `docentes/lista.jsp` define
`<c:set var="activo" value="docentes" />` — es la misma variable que ya usan todas las demás
páginas.

### Paso 7 (opcional) — Restringir el acceso por rol

Si "Docentes" debe ser solo para administradores (como pasa con "Usuarios"), se agrega la ruta
al mismo `if` que ya existe en `AuthFilter.java`:

```java
if ((ruta.startsWith("/usuarios") || ruta.startsWith("/docentes")) && !usuario.isAdministrador()) {
    Flash.error(request, "No tienes permiso para acceder a esa sección.");
    response.sendRedirect(contexto + "/dashboard");
    return;
}
```

Si cualquier usuario logueado puede entrar (como pasa con "Personas"), este paso se salta
directamente — no hace falta tocar nada más, porque `AuthFilter` ya exige sesión iniciada por
defecto para cualquier ruta que no sea pública.

### Verificación final

Con los 6-7 pasos hechos: recompilar (`mvnw.cmd compile` detecta errores de Java, pero **no**
errores de JSP — esos recién aparecen al desplegar en Tomcat), volver a desplegar el `.war
exploded`, entrar a `/docentes` y probar: listar, crear uno nuevo, editarlo, buscar por nombre
en la tabla, y eliminarlo (confirmando que desaparece de la lista porque quedó con
`estado = 0`, no porque se borró la fila).

## Reglas de oro

- SQL solo en el DAO, siempre `PreparedStatement`.
- HTML solo dentro de `WEB-INF/views`; los servlets nunca escriben HTML.
- Toda salida se escapa con `<c:out>` o `${fn:escapeXml(...)}`.
- Post/Redirect/Get en todo lo que modifica datos.
- Baja lógica (`estado = 0`) en vez de `DELETE`, salvo el caso explícito de `UsuarioDAO.eliminarDefinitivo`.
