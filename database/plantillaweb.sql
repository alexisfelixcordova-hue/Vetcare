-- Script de creación de la base de datos PlantillaWeb.
-- Pensado para poder ejecutarse varias veces sin fallar (todo valida "si no existe" antes de crear).

-- 1) Base de datos
IF DB_ID(N'PlantillaWeb') IS NULL
BEGIN
    CREATE DATABASE PlantillaWeb;
END
GO

USE PlantillaWeb;
GO

-- 2) Login propio de la aplicación (nunca se usa "sa" desde el código)
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'plantilla_app')
BEGIN
    CREATE LOGIN plantilla_app WITH PASSWORD = N'Plantilla#2025', CHECK_POLICY = OFF;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'plantilla_app')
BEGIN
    CREATE USER plantilla_app FOR LOGIN plantilla_app;
    ALTER ROLE db_datareader ADD MEMBER plantilla_app;
    ALTER ROLE db_datawriter ADD MEMBER plantilla_app;
END
GO

-- 3) Tabla de roles
IF OBJECT_ID(N'dbo.rol', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.rol (
        id_rol INT IDENTITY(1,1) PRIMARY KEY,
        nombre VARCHAR(50) NOT NULL UNIQUE
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM dbo.rol WHERE nombre = 'Administrador')
    INSERT INTO dbo.rol (nombre) VALUES ('Administrador');
IF NOT EXISTS (SELECT 1 FROM dbo.rol WHERE nombre = 'Usuario')
    INSERT INTO dbo.rol (nombre) VALUES ('Usuario');
GO

-- 4) Tabla de usuarios
IF OBJECT_ID(N'dbo.usuario', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.usuario (
        id_usuario     INT IDENTITY(1,1) PRIMARY KEY,
        nombres        VARCHAR(100)  NOT NULL,
        apellidos      VARCHAR(100)  NOT NULL,
        dni            CHAR(8)       NOT NULL UNIQUE,
        email          VARCHAR(150)  NULL,
        usuario        VARCHAR(50)   NOT NULL UNIQUE,
        password       VARCHAR(100)  NOT NULL,
        id_rol         INT           NOT NULL,
        estado         BIT           NOT NULL DEFAULT 1,
        fecha_registro DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_usuario_rol FOREIGN KEY (id_rol) REFERENCES dbo.rol(id_rol)
    );
END
GO

-- 4.1) Foto de perfil del usuario (nombre de archivo dentro de /assets/images/usuarios). Se agrega
--      con ALTER porque la tabla usuario ya pudo haberse creado antes sin esta columna.
IF COL_LENGTH('dbo.usuario', 'foto') IS NULL
BEGIN
    ALTER TABLE dbo.usuario ADD foto VARCHAR(255) NULL;
END
GO

-- 5) Usuario inicial: admin / admin123
--    El hash de abajo es BCrypt real, generado ejecutando Password.main() (no es un valor inventado).
IF NOT EXISTS (SELECT 1 FROM dbo.usuario WHERE usuario = 'admin')
BEGIN
    INSERT INTO dbo.usuario (nombres, apellidos, dni, email, usuario, password, id_rol, estado)
    SELECT 'Administrador', 'General', '00000000', 'admin@plantillaweb.local', 'admin',
           '$2a$10$LyuFEyOT.8ydZXCQwBI7qu/Tp0/j2t1/fQtL3mZIjmLAZ/adfK36m',
           id_rol, 1
    FROM dbo.rol
    WHERE nombre = 'Administrador';
END
GO
