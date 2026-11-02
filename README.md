# VetCare — interfaz visual sobre Plantillaweb

Este proyecto conserva la base Java/JSP, Maven, Docker y SQL Server de Plantillaweb, con una nueva interfaz visual de gestión veterinaria inspirada en el proyecto VetCare de referencia.

## Ejecutar con IntelliJ IDEA y Docker

1. Abre la carpeta `Plantillaweb` (la que contiene `pom.xml` y `docker-compose.yml`) en IntelliJ IDEA.
2. Revisa `.env` y configura una contraseña segura para SQL Server en `DB_SA_PASSWORD`.
3. Inicia Docker Desktop.
4. En la terminal de IntelliJ ejecuta: `docker compose up --build`
5. Abre `http://localhost:8080`.

Si la primera compilación tarda, espera a que Maven termine de descargar las dependencias. Para detener: `docker compose down`. Para detener y borrar también los datos persistidos de SQL Server: `docker compose down -v` (esto elimina el volumen de la base de datos).

## Estado actual

- La pantalla de acceso conserva la autenticación de la plantilla.
- El dashboard muestra el diseño VetCare y las vistas visuales de Dashboard, Citas, Pacientes, Clientes, Servicios, Pagos, Historial médico, Reportes y Configuración.
- Los datos de estas vistas son de demostración y están escritos como maqueta de interfaz. Los formularios muestran una nota de que aún no guardan en SQL Server.
- El backend existente de usuarios, roles, sesión y conexión a SQL Server se conserva.
- En la siguiente etapa se implementará el esquema SQL de veterinaria y se conectará cada módulo con sus Servlet/DAO. No se deben tratar los datos de muestra como registros reales.

## Estructura técnica

- Java + Jakarta Servlet + JSP
- Maven / Tomcat
- Microsoft SQL Server 2022
- Docker Compose
- Bootstrap y Font Awesome locales
