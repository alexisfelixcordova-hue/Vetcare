package com.example.plantillaweb.config;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Conexion {

    private static final String HOST = System.getenv("DB_HOST") != null
            ? System.getenv("DB_HOST")
            : (System.getProperty("os.name").toLowerCase().contains("win") ? "localhost" : "host.docker.internal");

    private static final String INSTANCE = "MSSQLSERVER01";
    private static final String DATABASE = "clinica_veterinaria";

    // Credenciales de SQL Server
    private static final String USER = "usr_una_bd";
    private static final String PASSWORD = "UnaBD#2026";

    private static final int TAMANO_POOL = 10;
    private static final int ESPERA_SEGUNDOS = 5;

    private static final BlockingQueue<Connection> libres = new ArrayBlockingQueue<>(TAMANO_POOL);
    private static final Semaphore permisos = new Semaphore(TAMANO_POOL, true);

    private static final SQLServerDataSource dataSource = crearDataSource();

    private Conexion() {
    }

    private static SQLServerDataSource crearDataSource() {
        SQLServerDataSource ds = new SQLServerDataSource();
        ds.setServerName(HOST);
        ds.setInstanceName(INSTANCE);
        ds.setDatabaseName(DATABASE);

        // Autenticación por usuario y contraseña (evita requerir el archivo .dll de Windows)
        ds.setUser(USER);
        ds.setPassword(PASSWORD);
        ds.setIntegratedSecurity(false);

        // Seguridad de red
        ds.setEncrypt("true");
        ds.setTrustServerCertificate(true);
        ds.setLoginTimeout(5);
        return ds;
    }

    public static Connection getConnection() throws SQLException {
        try {
            if (!permisos.tryAcquire(ESPERA_SEGUNDOS, TimeUnit.SECONDS)) {
                throw new SQLException("No hay conexiones libres en el pool (espera de "
                        + ESPERA_SEGUNDOS + " s agotada).");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrumpido esperando una conexión libre del pool", e);
        }

        try {
            Connection real = obtenerRealValida();
            return envolver(real);
        } catch (SQLException | RuntimeException e) {
            permisos.release();
            throw e;
        }
    }

    private static Connection obtenerRealValida() throws SQLException {
        Connection real;
        while ((real = libres.poll()) != null) {
            if (esValida(real)) {
                return real;
            }
            cerrarSilencioso(real);
        }
        return dataSource.getConnection();
    }

    private static boolean esValida(Connection c) {
        try {
            return !c.isClosed() && c.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    private static void devolver(Connection real) {
        try {
            if (!real.getAutoCommit()) {
                real.rollback();
                real.setAutoCommit(true);
            }
            if (esValida(real) && libres.offer(real)) {
                return;
            }
            cerrarSilencioso(real);
        } catch (SQLException e) {
            cerrarSilencioso(real);
        } finally {
            permisos.release();
        }
    }

    private static void cerrarSilencioso(Connection c) {
        try {
            c.close();
        } catch (SQLException ignorada) {
        }
    }

    private static Connection envolver(Connection real) {
        final AtomicBoolean cerrada = new AtomicBoolean(false);

        InvocationHandler manejador = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method metodo, Object[] argumentos) throws Throwable {
                String nombre = metodo.getName();

                if ("close".equals(nombre)) {
                    if (cerrada.compareAndSet(false, true)) {
                        devolver(real);
                    }
                    return null;
                }
                if ("isClosed".equals(nombre)) {
                    return cerrada.get();
                }
                if (cerrada.get()) {
                    throw new SQLException("La conexión ya fue cerrada");
                }

                try {
                    return metodo.invoke(real, argumentos);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        };

        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[] { Connection.class }, manejador);
    }
}