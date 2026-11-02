package com.example.plantillaweb.dao;

import com.example.plantillaweb.config.Conexion;
import com.example.plantillaweb.model.Mascota;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MascotaDAO {

    public List<Mascota> listarConCliente() throws SQLException {
        List<Mascota> lista = new ArrayList<>();
        // Incluimos la columna 'edad' tal como está en tu base de datos
        String sql = "SELECT m.id_mascota, m.id_cliente, m.nombre, m.especie, m.raza, m.sexo, m.edad, m.peso, m.estado, c.nombre AS cliente_nombre " +
                "FROM mascota m INNER JOIN cliente c ON m.id_cliente = c.id_cliente";

        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Mascota m = new Mascota();
                m.setIdMascota(rs.getInt("id_mascota"));
                m.setIdCliente(rs.getInt("id_cliente"));
                m.setNombre(rs.getString("nombre"));
                m.setEspecie(rs.getString("especie"));
                m.setRaza(rs.getString("raza"));
                m.setSexo(rs.getString("sexo"));

                // Leemos la edad directamente de la base de datos (manejando nulos si los hubiera)
                String edadDb = rs.getString("edad");
                m.setEdad(edadDb != null ? edadDb : "N/D");

                m.setPeso(rs.getDouble("peso"));
                m.setEstado(rs.getString("estado"));
                m.setNombreCliente(rs.getString("cliente_nombre"));
                lista.add(m);
            }
        }
        return lista;
    }

    public void insertar(Mascota m) throws SQLException {
        String sql = "INSERT INTO mascota (id_cliente, nombre, especie, raza, sexo, edad, peso, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, m.getIdCliente());
            ps.setString(2, m.getNombre());
            ps.setString(3, m.getEspecie());
            ps.setString(4, m.getRaza());
            ps.setString(5, m.getSexo());
            ps.setString(6, m.getEdad());
            ps.setDouble(7, m.getPeso());
            ps.setString(8, m.getEstado() != null ? m.getEstado() : "Activo");
            ps.executeUpdate();
        }
    }
}