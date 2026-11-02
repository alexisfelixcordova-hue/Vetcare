package com.example.plantillaweb.dao;

import com.example.plantillaweb.config.Conexion;
import com.example.plantillaweb.model.Rol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Todo el SQL de la tabla rol vive aquí. Los servlets nunca escriben SQL directamente. */
public class RolDAO {

    /** Lista todos los roles, para llenar el <select> del formulario de usuarios. */
    public List<Rol> listar() throws SQLException {
        String sql = "SELECT id_rol, nombre FROM dbo.rol ORDER BY nombre";
        List<Rol> roles = new ArrayList<>();
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                roles.add(mapear(rs));
            }
        }
        return roles;
    }

    /** Cuenta los roles existentes, para la tarjeta del dashboard. */
    public int contar() throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.rol";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Rol mapear(ResultSet rs) throws SQLException {
        Rol rol = new Rol();
        rol.setIdRol(rs.getInt("id_rol"));
        rol.setNombre(rs.getString("nombre"));
        return rol;
    }
}
