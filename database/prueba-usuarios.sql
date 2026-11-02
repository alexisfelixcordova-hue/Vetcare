-- Agrega las tablas rol y usuario (login del sistema) a la base "prueba" existente,
-- sin tocar ninguna tabla que ya tengas ahí (como persona). Se puede ejecutar varias veces.

USE prueba;
GO

-- Roles
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

-- Usuarios del sistema (login)
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

-- Foto de perfil del usuario (nombre de archivo dentro de /assets/images/usuarios)
IF COL_LENGTH('dbo.usuario', 'foto') IS NULL
BEGIN
    ALTER TABLE dbo.usuario ADD foto VARCHAR(255) NULL;
END
GO

-- Usuario inicial: admin / admin123 (mismo hash BCrypt real que ya habíamos generado)
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


-- Tabla de productos para el inventario de VetCare
IF OBJECT_ID(N'dbo.producto', N'U') IS NULL
BEGIN
CREATE TABLE dbo.producto (
                              id INT IDENTITY(1,1) PRIMARY KEY,
                              codigo VARCHAR(30) NOT NULL UNIQUE,
                              nombre VARCHAR(150) NOT NULL,
                              descripcion VARCHAR(255) NULL,
                              unidad_medida VARCHAR(20) NOT NULL,
                              precio_compra DECIMAL(10,2) NOT NULL DEFAULT 0,
                              precio_venta DECIMAL(10,2) NOT NULL DEFAULT 0,
                              stock INT NOT NULL DEFAULT 0,
                              stock_minimo INT NOT NULL DEFAULT 0,
                              estado BIT NOT NULL DEFAULT 1,
                              fecha_creacion DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
                              fecha_actualizacion DATETIME2 NULL,

                              CONSTRAINT CK_producto_precio_compra
                                  CHECK (precio_compra >= 0),

                              CONSTRAINT CK_producto_precio_venta
                                  CHECK (precio_venta >= 0),

                              CONSTRAINT CK_producto_stock
                                  CHECK (stock >= 0),

                              CONSTRAINT CK_producto_stock_minimo
                                  CHECK (stock_minimo >= 0)
);
END
GO
