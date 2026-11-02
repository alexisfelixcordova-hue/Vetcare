# Manual: cómo se construyó esta plantilla, paso a paso

Este documento explica **cómo se armó** Plantillaweb desde cero, en el orden en que se hizo,
y el **porqué** de cada decisión. Sirve para entender la plantilla a fondo y para repetir el
proceso si necesitas armar un proyecto nuevo desde el mismo punto de partida.

Si lo que necesitas es **usar** la plantilla (ejecutarla, ver la estructura, agregar un módulo
nuevo), esa guía rápida está en [`README.md`](README.md). Este archivo es el "detrás de cámaras".

---

## Paso 0: Crear el proyecto en IntelliJ

1. IntelliJ IDEA → **New Project** → **Jakarta EE** → **Web application** → build system **Maven**.
2. `groupId: com.example`, `artifactId: Plantillaweb`, paquete base `com.example.plantillaweb`.
3. IntelliJ genera un proyecto con un `HelloServlet.java` de ejemplo y un `index.jsp` con
   "Hello World". Esos dos archivos se borran apenas se empieza: son solo para probar que el
   asistente funcionó.
4. Se configura un servidor **Tomcat 10.1.x** en IntelliJ (Run/Debug Configurations → Tomcat
   Server), apuntando a una instalación de Tomcat 10.1 descargada aparte, y se asocia el
   artifact **"Plantillaweb:war exploded"** a esa configuración.

## Paso 1: `pom.xml`

El asistente genera un `pom.xml` para Java 8 con `servlet-api 6.1.0`. Se ajusta a lo que
realmente necesita Tomcat 10.1 (que implementa **Servlet 6.0**, no 6.1) y a la versión de JDK
instalada:

```xml
<maven.compiler.release>21</maven.compiler.release>
...
<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
    <version>6.0.0</version>
    <scope>provided</scope>  <!-- Tomcat ya la trae puesta, no va dentro del .war -->
</dependency>
```

Se agregan, en orden, las dependencias que se van a necesitar:

| Dependencia | Para qué |
|---|---|
| `jakarta.servlet.jsp.jstl-api` + `org.glassfish.web:jakarta.servlet.jsp.jstl` | Las etiquetas `<c:...>` y `<fn:...>` de las vistas JSP (API + implementación) |
| `com.microsoft.sqlserver:mssql-jdbc` | Driver JDBC para hablar con SQL Server |
| `org.mindrot:jbcrypt` | Hashear contraseñas (nunca se guardan en texto plano) |
| `org.junit.jupiter:*` (scope `test`) | Pruebas unitarias |

> Nota: en un momento se agregó también **HikariCP** (pool de conexiones) y **SLF4J**, pero se
> sacaron después al simplificar `Conexion.java` (ver Paso 4). El `pom.xml` final no las tiene.

Se agrega el plugin `maven-war-plugin` para que `mvnw.cmd clean package` genere el `.war`.

## Paso 2: Estructura de carpetas

Se crea de una vez toda la estructura de paquetes y carpetas, aunque todavía estén vacíos,
para tener el "mapa" completo desde el principio:

```
database/
src/main/java/com/example/plantillaweb/
├── config/       (conexión a la base de datos)
├── model/        (POJOs)
├── dao/          (acceso a datos, SQL)
├── controller/   (servlets)
├── filter/       (filtros de sesión y CSRF)
└── util/         (BCrypt, mensajes flash)
src/main/resources/
src/main/webapp/
├── assets/       (CSS, JS e imágenes propios y de terceros)
└── WEB-INF/views/
    ├── layout/   (cabecera, menú y pie comunes a toda página)
    ├── auth/, dashboard/, usuarios/, personas/, perfil/, error/
```

Regla de esta plantilla: **todo el HTML vive dentro de `WEB-INF/views`**. Nada de JSP suelto en
`webapp/` (salvo `index.jsp`, que ni siquiera tiene HTML: solo redirige).

## Paso 3: Base de datos

Se escribe `database/plantillaweb.sql`, pensado para poder ejecutarse **muchas veces sin
romper nada** (todo valida "si no existe" antes de crear algo):

1. Crea la base de datos (`IF DB_ID(...) IS NULL`).
2. Crea un login propio de la aplicación con permisos acotados (no `sa`).
3. Crea la tabla `rol` y le inserta 'Administrador' y 'Usuario'.
4. Crea la tabla `usuario`, con `id_rol` como llave foránea a `rol` y `estado BIT` para poder
   "eliminar" usuarios sin borrar la fila (baja lógica).
5. Inserta el usuario `admin` con una contraseña ya encriptada con BCrypt (el hash se genera
   ejecutando `Password.main()`, ver Paso 6 — nunca se escribe la contraseña en texto plano ni
   siquiera en el script).

Se ejecuta con `sqlcmd -S localhost,1433 -E -C -i database\plantillaweb.sql` y se verifica el
resultado con un `SELECT` antes de seguir.

## Paso 4: Conexión a la base de datos

`config/Conexion.java` es el único lugar del proyecto que sabe cómo conectarse a SQL Server.
Esta clase pasó por tres versiones durante la construcción de la plantilla; la que quedó es la
**tercera**:

1. **Primera versión**: pool con la librería HikariCP, leyendo host/usuario/clave desde un
   `src/main/resources/db.properties`. Es el enfoque "de manual" (separa configuración de
   código), pero agrega una dependencia externa.
2. **Segunda versión**: se cambió a un estilo más directo y explícito —
   `SQLServerDataSource` de mssql-jdbc, sin pool, con host/base/usuario/clave como constantes
   dentro de la clase. Más fácil de leer de un vistazo, pero **cada consulta abre una conexión
   TCP+TLS nueva contra el servidor**, lo cual es notablemente más lento (se sintió al navegar
   entre pantallas).
3. **Versión final**: se mantiene el mismo estilo simple (mismas constantes, mismo
   `SQLServerDataSource`), pero se le agrega un **pool casero** de 10 conexiones hecho a mano
   (sin librerías nuevas), para no perder la simplicidad de lectura ni la velocidad:
   - Al arrancar la aplicación, se abren 10 conexiones reales y se guardan en una
     `BlockingQueue`.
   - `getConnection()` toma una conexión prestada de la cola.
   - Esa conexión se envuelve con un **proxy dinámico** (`java.lang.reflect.Proxy`) que
     intercepta el método `close()`: en vez de cerrar la conexión de verdad, la devuelve a la
     cola. Así, todo el resto del código (los DAO) sigue escribiendo
     `try (Connection con = Conexion.getConnection()) { ... }` exactamente igual que si no
     hubiera pool — no hubo que tocar ni una línea de los DAO.

```java
private static Connection envolver(Connection real) {
    InvocationHandler manejador = (proxy, metodo, argumentos) -> {
        if ("close".equals(metodo.getName())) {
            pool.put(real);       // no cierra: la devuelve al pool
            return null;
        }
        return metodo.invoke(real, argumentos); // todo lo demás va a la conexión real
    };
    return (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(), new Class<?>[] { Connection.class }, manejador);
}
```

**Importante:** con esta versión, host/base/usuario/contraseña quedan como constantes dentro
de `Conexion.java` (no en un `.properties` externo). Es una simplificación deliberada para que
sea fácil de leer en un contexto de aprendizaje: antes de ejecutar el proyecto hay que abrir
esta clase y poner la contraseña real.

## Paso 5: Modelos (`model/`)

POJOs simples: campos privados, constructor vacío, getters y setters, nombres en camelCase
(`idUsuario`, `fechaRegistro`). Nada de lógica de negocio ni de SQL acá. `Usuario` incluye
además `nombreRol` (viene del JOIN con `rol`, no es una columna propia) y un par de métodos de
conveniencia (`getNombreCompleto()`, `isAdministrador()`).

## Paso 6: Utilidades (`util/`)

- **`Password.java`**: `encriptar(clave)` y `verificar(clave, hash)` con BCrypt.
  Tiene un `main()` que imprime el hash de una contraseña de prueba — así se genera el valor
  que se pega a mano en el script SQL del Paso 3, sin inventar un hash falso.
- **`Flash.java`**: guarda un mensaje (`exito`/`error`/`advertencia`) en la sesión antes de un
  `redirect`. La vista lo lee una sola vez y lo borra (ver Paso 9). Así se puede mostrar
  "Usuario creado correctamente" después de recargar la página sin perder el mensaje.

Se agrega una prueba JUnit (`PasswordTest`) para confirmar que `encriptar`/`verificar`
funcionan como se espera.

## Paso 7: DAO (`dao/`)

Un DAO por tabla (`UsuarioDAO`, `RolDAO`, `PersonaDAO`...). Reglas fijas en todos:

- **Todo el SQL vive acá.** Ningún servlet ni JSP escribe una consulta.
- **Siempre `PreparedStatement` con `?`**, nunca se concatena un valor dentro del SQL (evita
  inyección SQL).
- **`try-with-resources`** en `Connection`, `PreparedStatement` y `ResultSet`, para que se
  cierren solos incluso si algo falla.
- Un método privado `mapear(ResultSet)` que arma el objeto del modelo a partir de una fila.
- **Baja lógica**: `eliminar(id)` hace `UPDATE ... SET estado = 0`, nunca un `DELETE`.
- Paginación con `OFFSET ? ROWS FETCH NEXT ? ROWS ONLY` (sintaxis de SQL Server).

## Paso 8: Filtros (`filter/`)

Dos filtros mapeados a `/*` (pasan por TODAS las peticiones):

- **`AuthFilter`**: si la ruta no es pública (`/login`, `/assets/*`) y no hay `usuario` en la
  sesión, redirige a `/login`. Si la ruta es `/usuarios` y el rol no es Administrador, redirige
  al dashboard con un mensaje de error. También agrega cabeceras anti-caché
  (`Cache-Control: no-cache, no-store`) a **todo** menos a `/assets/*`, para que el navegador
  nunca guarde una copia vieja de una página privada ni de `/login` (esto evitó, por ejemplo,
  que quedara guardado un token CSRF viejo después de reiniciar el servidor).
- **`CsrfFilter`**: genera un token aleatorio por sesión (`SecureRandom`) y lo guarda como
  `csrf`. En **todo POST** exige que venga un campo oculto `_csrf` con ese mismo valor; si no
  coincide, responde 403. Todos los formularios de la plantilla llevan
  `<input type="hidden" name="_csrf" value="${sessionScope.csrf}">`.

## Paso 9: Layout común (`views/layout/`)

Tres fragmentos que se incluyen en cada página con `<%@ include %>`:

- **`head.jspf`**: abre `<html><head>...`, declara los taglibs (`c`, `fn`) y carga el CSS.
  Es el único lugar donde se declara `<%@ page contentType="text/html;charset=UTF-8" ... %>`.
- **`menu.jspf`**: la topbar (logo + usuario logueado) y la sidebar (íconos + texto, angosta en
  escritorio, un drawer normal en mobile). Recibe una variable `activo` para marcar la opción
  actual del menú.
- **`scripts.jspf`**: cierra los `<div>` que abrió `menu.jspf`, muestra el mensaje flash (si
  hay uno) y carga los `<script>` (Bootstrap, SweetAlert2, `app.js`), y cierra `</body></html>`.

Cada página de contenido sigue siempre el mismo esqueleto:

```jsp
<%@ include file="/WEB-INF/views/layout/head.jspf" %>
<c:set var="activo" value="dashboard" />
<%@ include file="/WEB-INF/views/layout/menu.jspf" %>

... contenido de la página ...

<%@ include file="/WEB-INF/views/layout/scripts.jspf" %>
```

**Gotcha importante que se encontró y su arreglo:** el `<c:set>` tiene que ir **después** del
`include` de `head.jspf`, no antes. `head.jspf` es quien declara el taglib `prefix="c"`; si se
usa `<c:set>` antes de que ese `include` se procese, Tomcat tira
`JasperException: El prefijo [c] ... ha sido usado previamente`.

**Otro gotcha de encoding:** poner `pageEncoding="UTF-8"` en `head.jspf` solo arregla la
lectura de *ese* archivo, no la del archivo que lo incluye. Como el texto con tildes vive en
cada vista (no en `head.jspf`), cada archivo nuevo repetía el bug (las tildes salían como
`Ã³`). La solución de raíz fue declarar el encoding **una sola vez, para todo el proyecto**, en
`web.xml`:

```xml
<jsp-config>
    <jsp-property-group>
        <url-pattern>*.jsp</url-pattern>
        <url-pattern>*.jspf</url-pattern>
        <page-encoding>UTF-8</page-encoding>
    </jsp-property-group>
</jsp-config>
```

## Paso 10: Assets (`webapp/assets/`)

Se decidió no usar CDNs: todo lo que carga el navegador vive dentro del proyecto.

- **Bootstrap 5.3** (CSS + `bootstrap.bundle.min.js`) y los íconos: primero se probó con
  **Boxicons** (tomados de un tema llamado Velzon), y más adelante se migraron a
  **Font Awesome Free 7**, descargado una vez desde su release oficial y copiado a
  `assets/libs/fontawesome/` (solo `all.min.css` + las 3 fuentes que realmente se usan:
  solid, regular y brands).
- **SweetAlert2** (`sweetalert2.all.min.js`), para todos los mensajes y confirmaciones.
- **simplebar**, para la barra de scroll del sidebar.
- **`custom.css`**: los ajustes propios encima de Bootstrap (topbar, sidebar, tabla estándar,
  página de login).

**Gotcha grande que se encontró:** el `bootstrap.min.css` que se copió del tema Velzon **no es
Bootstrap puro** — es una build personalizada que usa variables `--vz-*` en vez de las `--bs-*`
estándar, y varias de esas variables (la tipografía y el borde de los `<input>`) están pensadas
para definirse en el `app.min.css` propio de Velzon, que **no se copió**. Sin esas variables:
- Los inputs (`.form-control`) quedaban sin borde visible (`border-color` inválido).
- El texto de toda la página caía a la fuente serif por defecto del navegador.

Se arregló definiendo esas variables faltantes a mano en `custom.css`, con los mismos valores
por defecto que usa Velzon:

```css
:root {
    --vz-input-bg-custom: #fff;
    --vz-input-border-custom: var(--vz-border-color);
}
body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}
```

## Paso 11: Login, logout y dashboard

- **`LoginServlet`** (`/login`): GET muestra el formulario (o redirige a `/dashboard` si ya hay
  sesión). POST busca el usuario, verifica la contraseña con BCrypt, cambia el id de sesión
  (`request.changeSessionId()`, para evitar *session fixation*) y guarda el usuario en sesión.
- **`LogoutServlet`** (`/logout`, solo POST): invalida la sesión y redirige a `/login`.
- **`DashboardServlet`** (`/dashboard`): consulta un par de totales (usuarios activos, roles) y
  los muestra en tarjetas con un ícono dentro de un círculo de color.

## Paso 12: CRUD de Usuarios (y de Personas) con modal

La primera versión de "crear/editar usuario" usaba una página de formulario aparte
(`form.jsp`) a la que se llegaba con `?accion=nuevo` o `?accion=editar&id=`. Se rehizo para que
crear y editar pasen **dentro de un modal** de Bootstrap, sobre la misma lista, sin recargar la
página:

- El modal vive una sola vez dentro de `lista.jsp`, junto con la tabla.
- El botón "Nuevo" solo abre el modal vacío (no llama al servidor).
- Cada botón "Editar" de la tabla trae sus datos en atributos `data-*`
  (`data-nombres="..."`, `data-dni="..."`, etc.) — un **manejador genérico en `app.js`** copia
  esos atributos a los `<input name="...">` del modal que tengan el mismo nombre, así que no
  hace falta escribir JavaScript para cada campo nuevo.
- Si la validación del servidor falla al guardar, en vez de redirigir se vuelve a mostrar la
  lista con un `abrirModal = true`; un pequeño marcador (`data-modal-autoabrir`) hace que
  `app.js` reabra el modal solo, ya con los datos que se habían escrito.

Este mismo patrón se repitió, sin cambios de fondo, para armar el módulo de ejemplo
**Personas** (ver la receta completa en el README).

## Paso 13: Validaciones con SweetAlert2

Se reemplazó el globo nativo del navegador ("Por favor, completa este campo") por SweetAlert2
en todos lados:

- Los formularios con validación HTML5 (`required`, `pattern`, etc.) llevan `novalidate`;
  `app.js` intercepta el `submit`, revisa el primer campo inválido con la Constraint
  Validation API del navegador y muestra el error con SweetAlert2, usando el texto del
  `<label>` para nombrar el campo.
- Los mensajes que vienen del servidor (`Flash.exito/error/advertencia`) se muestran distinto
  según el tipo: el éxito como un toast chico que desaparece solo; el error/advertencia como un
  modal que hay que cerrar a propósito, para que una validación fallida no pase desapercibida.

## Paso 14: Perfil

`PerfilServlet` (`/perfil`) muestra los datos del usuario logueado y permite cambiar su propia
contraseña (pide la actual, la nueva y su confirmación; valida todo en el servidor antes de
guardar el nuevo hash).

## Paso 15: Páginas de error y `web.xml` final

`404.jsp` y `500.jsp` son páginas simples (sin sidebar, porque pueden dispararse sin sesión
iniciada) con un botón para volver al inicio. `web.xml` las registra como `<error-page>`, fija
el `welcome-file`, el `session-timeout` y el `jsp-config` del Paso 9.

## Paso 16: README y receta para módulos nuevos

Por último, se escribió `README.md` con los pasos para ejecutar el proyecto y una receta de
6 pasos para copiar el patrón de "Usuarios" en un módulo nuevo (tabla → model → DAO → servlet →
vista con modal → línea en el menú). Ese README es la referencia rápida del día a día; este
`MANUAL.md` es la explicación larga de cómo se llegó hasta ahí.

---

## Resumen de los "gotchas" encontrados (y por qué importan)

| Problema | Causa | Arreglo |
|---|---|---|
| `JasperException: prefijo [c] ya usado` | `<c:set>` antes del `include` que declara el taglib | Mover el `<c:set>` después de `head.jspf` |
| Tildes rotas (`Ã³` en vez de `ó`) | `pageEncoding` declarado solo en el archivo incluido, no en cada vista | `<jsp-config>` global en `web.xml` |
| Inputs sin borde visible | Variables `--vz-input-*-custom` de Velzon sin definir (solo existen en su `app.min.css`, que no se copió) | Definirlas a mano en `custom.css` |
| Tipografía serif por defecto | Lo mismo: `--vz-font-sans-serif` sin definir | `font-family` explícito en `custom.css` |
| `403 Token CSRF inválido` tras reiniciar el servidor | El navegador mostraba `/login` cacheado con un token de una sesión que ya no existe | Cabeceras anti-caché también en `/login` |
| Navegación lenta entre pantallas | `Conexion.java` abría una conexión TCP+TLS nueva por cada consulta | Pool casero de conexiones con un proxy que intercepta `close()` |
