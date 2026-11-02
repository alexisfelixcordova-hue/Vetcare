
package com.example.plantillaweb.dao;

import com.example.plantillaweb.config.Conexion;
import com.example.plantillaweb.model.Producto;
import com.example.plantillaweb.model.ResumenInventario;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    private static final String COLUMNAS =
            "id, codigo, nombre, descripcion, unidad_medida, precio_compra, precio_venta, " +
                    "stock, stock_minimo, estado, fecha_creacion, fecha_actualizacion";

    public List<Producto> listar() throws SQLException {
        String sql = "SELECT " + COLUMNAS +
                " FROM dbo.producto WHERE estado = 1 ORDER BY nombre";

        List<Producto> productos = new ArrayList<>();

        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                productos.add(mapear(rs));
            }
        }

        return productos;
    }

    public int contar() throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.producto WHERE estado = 1";

        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public ResumenInventario resumen() throws SQLException {
        String sql = "SELECT COUNT(*), " +
                "SUM(CASE WHEN stock > 0 AND stock <= stock_minimo THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN stock <= 0 THEN 1 ELSE 0 END), " +
                "SUM(CAST(stock AS DECIMAL(18,2)) * precio_compra) " +
                "FROM dbo.producto WHERE estado = 1";

        ResumenInventario resumen = new ResumenInventario();

        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                resumen.setTotalProductos(rs.getInt(1));
                resumen.setStockBajo(rs.getInt(2));
                resumen.setAgotados(rs.getInt(3));

                BigDecimal valor = rs.getBigDecimal(4);
                if (valor != null) {
                    resumen.setValorInventario(valor);
                }
            }
        }

        return resumen;
    }

    public boolean existeCodigo(String codigo, int idExcluir) throws SQLException {
        String sql = "SELECT 1 FROM dbo.producto " +
                "WHERE estado = 1 AND codigo = ? AND id <> ?";

        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, codigo);
            ps.setInt(2, idExcluir);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void insertar(Producto p) throws SQLException {
        String sql = "INSERT INTO dbo.producto " +
                "(codigo, nombre, descripcion, unidad_medida, precio_compra, " +
                "precio_venta, stock, stock_minimo, estado, fecha_creacion) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, SYSDATETIME())";

        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            llenarDatos(ps, p);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    p.setId(rs.getInt(1));
                }
            }
        }
    }

    public void actualizar(Producto p) throws SQLException {
        String sql = "UPDATE dbo.producto SET " +
                "codigo = ?, nombre = ?, descripcion = ?, unidad_medida = ?, " +
                "precio_compra = ?, precio_venta = ?, stock = ?, stock_minimo = ?, " +
                "fecha_actualizacion = SYSDATETIME() " +
                "WHERE id = ? AND estado = 1";

        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            llenarDatos(ps, p);
            ps.setInt(9, p.getId());
            ps.executeUpdate();
        }
    }

    public void eliminar(int id) throws SQLException {
        String sql = "UPDATE dbo.producto SET estado = 0, " +
                "fecha_actualizacion = SYSDATETIME() WHERE id = ?";

        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private void llenarDatos(PreparedStatement ps, Producto p)
            throws SQLException {

        ps.setString(1, p.getCodigo());
        ps.setString(2, p.getNombre());

        String descripcion = p.getDescripcion();
        ps.setString(3,
                descripcion == null || descripcion.isBlank()
                        ? null
                        : descripcion);

        ps.setString(4, p.getUnidadMedida());
        ps.setBigDecimal(5, p.getPrecioCompra());
        ps.setBigDecimal(6, p.getPrecioVenta());
        ps.setInt(7, p.getStock());
        ps.setInt(8, p.getStockMinimo());
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto();

        p.setId(rs.getInt("id"));
        p.setCodigo(rs.getString("codigo"));
        p.setNombre(rs.getString("nombre"));

        String descripcion = rs.getString("descripcion");
        p.setDescripcion(descripcion == null ? "" : descripcion);

        p.setUnidadMedida(rs.getString("unidad_medida"));
        p.setPrecioCompra(rs.getBigDecimal("precio_compra"));
        p.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        p.setStock(rs.getInt("stock"));
        p.setStockMinimo(rs.getInt("stock_minimo"));
        p.setEstado(rs.getBoolean("estado"));

        Timestamp creacion = rs.getTimestamp("fecha_creacion");
        if (creacion != null) {
            p.setFechaCreacion(creacion.toLocalDateTime());
        }

        Timestamp actualizacion = rs.getTimestamp("fecha_actualizacion");
        if (actualizacion != null) {
            p.setFechaActualizacion(actualizacion.toLocalDateTime());
        }

        return p;
    }
}
