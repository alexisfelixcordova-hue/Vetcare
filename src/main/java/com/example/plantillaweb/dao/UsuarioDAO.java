package com.example.plantillaweb.dao;

import com.example.plantillaweb.config.Conexion;
import com.example.plantillaweb.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Todo el SQL de la tabla usuario vive aquí. Los servlets solo llaman a estos métodos.
 * Regla de la casa: siempre PreparedStatement con "?", nunca concatenar valores en el SQL,
 * y siempre try-with-resources para que Connection/PreparedStatement/ResultSet se cierren solos.
 */
public class UsuarioDAO {

    private static final String SELECT_BASE =
            "SELECT u.id_usuario, u.nombre, u.rol, u.usuario, u.clave " +
                    "FROM usuario u ";

    /** Lista paginada de usuarios, filtrando por nombre o usuario. */
    public List<Usuario> listar(String buscar, int pagina, int porPagina) throws SQLException {
        String sql = SELECT_BASE +
                "WHERE (u.nombre LIKE ? OR u.usuario LIKE ? OR u.rol LIKE ?) " +
                "ORDER BY u.id_usuario " +
                "LIMIT ? OFFSET ?";
        String patron = "%" + (buscar == null ? "" : buscar.trim()) + "%";
        int desde = Math.max(pagina - 1, 0) * porPagina;

        List<Usuario> usuarios = new ArrayList<>();
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, patron);
            ps.setString(2, patron);
            ps.setString(3, patron);
            ps.setInt(4, porPagina);
            ps.setInt(5, desde);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    usuarios.add(mapear(rs));
                }
            }
        }
        return usuarios;
    }

    /** Cuenta cuántos usuarios cumplen el mismo filtro que listar(), para la paginación. */
    public int contar(String buscar) throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuario u WHERE (u.nombre LIKE ? OR u.usuario LIKE ? OR u.rol LIKE ?)";
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

    /** Busca un usuario por ID. */
    public Usuario obtenerPorId(int idUsuario) throws SQLException {
        String sql = SELECT_BASE + "WHERE u.id_usuario = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /** Busca por nombre de usuario. Lo usa el login. */
    public Usuario buscarPorUsuario(String usuario) throws SQLException {
        String sql = SELECT_BASE + "WHERE u.usuario = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /** Inserta un usuario nuevo y deja el ID generado en el propio objeto. */
    public void insertar(Usuario u) throws SQLException {
        String sql = "INSERT INTO usuario (nombre, rol, usuario, clave) VALUES (?, ?, ?, ?)";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getRol());
            ps.setString(3, u.getUsuario());
            ps.setString(4, u.getClave());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    u.setIdUsuario(rs.getInt(1));
                }
            }
        }
    }

    /** Actualiza los datos del usuario. Si clave viene vacía, NO se toca la contraseña actual. */
    public void actualizar(Usuario u) throws SQLException {
        boolean cambiaClave = u.getClave() != null && !u.getClave().isBlank();
        String sql = "UPDATE usuario SET nombre = ?, rol = ?, usuario = ?" +
                (cambiaClave ? ", clave = ?" : "") +
                " WHERE id_usuario = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, u.getNombre());
            ps.setString(i++, u.getRol());
            ps.setString(i++, u.getUsuario());
            if (cambiaClave) {
                ps.setString(i++, u.getClave());
            }
            ps.setInt(i, u.getIdUsuario());
            ps.executeUpdate();
        }
    }

    /** Cambia solo la contraseña de un usuario. Lo usa "Mi perfil". */
    public void cambiarPassword(int idUsuario, String nuevaClave) throws SQLException {
        String sql = "UPDATE usuario SET clave = ? WHERE id_usuario = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevaClave);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        }
    }

    /** Borra un usuario de forma definitiva. */
    public void eliminarDefinitivo(int idUsuario) throws SQLException {
        String sql = "DELETE FROM usuario WHERE id_usuario = ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
        }
    }

    /** Método de compatibilidad para el servlet (llama a eliminarDefinitivo). */
    public void eliminar(int idUsuario) throws SQLException {
        eliminarDefinitivo(idUsuario);
    }

    /** true si el nombre de usuario ya está usado por OTRO usuario. */
    public boolean existeUsuario(String usuario, int idExcluir) throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuario WHERE usuario = ? AND id_usuario <> ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, usuario);
            ps.setInt(2, idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /** Método de compatibilidad para verificar DNI (retorna falso al no manejarse en la nueva tabla). */
    public boolean existeDni(String dni, int idExcluir) throws SQLException {
        return false;
    }

    /** Cuenta el total de usuarios registrados, para la tarjeta del dashboard. */
    public int contarActivos() throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuario";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setNombre(rs.getString("nombre"));
        u.setRol(rs.getString("rol"));
        u.setUsuario(rs.getString("usuario"));
        u.setClave(rs.getString("clave"));
        return u;
    }
}