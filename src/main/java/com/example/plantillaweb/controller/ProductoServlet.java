package com.example.plantillaweb.controller;

import com.example.plantillaweb.dao.ProductoDAO;
import com.example.plantillaweb.model.Producto;
import com.example.plantillaweb.util.Flash;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "productoServlet", value = "/productos")
public class ProductoServlet extends HttpServlet {

    private static final Logger LOG =
            Logger.getLogger(ProductoServlet.class.getName());

    public static final List<String> UNIDADES = List.of(
            "Unidad",
            "Kilogramo (kg)",
            "Gramo (g)",
            "Litro (L)",
            "Mililitro (ml)",
            "Tableta",
            "Cápsula",
            "Frasco",
            "Ampolla",
            "Vial",
            "Dosis",
            "Jeringa prellenada"
    );

    private static final BigDecimal PRECIO_MAXIMO =
            new BigDecimal("99999999.99");

    private final ProductoDAO productoDAO = new ProductoDAO();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        try {
            mostrarLista(request, response, new Producto(), false);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error consultando productos", e);
            Flash.error(request,
                    "No se pudo cargar la información de productos.");
            response.sendRedirect(
                    request.getContextPath() + "/dashboard");
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        String accion = request.getParameter("accion");

        try {
            if ("guardar".equals(accion)) {
                guardar(request, response);
            } else if ("eliminar".equals(accion)) {
                eliminar(request, response);
            } else {
                response.sendRedirect(
                        request.getContextPath() + "/productos");
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error guardando el producto", e);
            Flash.error(request,
                    "Ocurrió un error al guardar los datos.");
            response.sendRedirect(
                    request.getContextPath() + "/productos");
        }
    }

    private void mostrarLista(
            HttpServletRequest request,
            HttpServletResponse response,
            Producto producto,
            boolean abrirModal
    ) throws SQLException, ServletException, IOException {

        request.setAttribute("productos", productoDAO.listar());
        request.setAttribute("resumen", productoDAO.resumen());
        request.setAttribute("unidades", UNIDADES);
        request.setAttribute("producto", producto);
        request.setAttribute("abrirModal", abrirModal);

        request.getRequestDispatcher(
                "/WEB-INF/views/productos/lista.jsp"
        ).forward(request, response);
    }

    private void guardar(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws SQLException, ServletException, IOException {

        Producto p = new Producto();

        p.setId(parseEntero(request.getParameter("id"), 0));

        p.setCodigo(
                valor(request.getParameter("codigo"))
                        .toUpperCase(Locale.ROOT)
        );

        p.setNombre(valor(request.getParameter("nombre")));
        p.setDescripcion(valor(request.getParameter("descripcion")));
        p.setUnidadMedida(valor(request.getParameter("unidadMedida")));

        p.setPrecioCompra(
                parseDecimal(request.getParameter("precioCompra"))
        );

        p.setPrecioVenta(
                parseDecimal(request.getParameter("precioVenta"))
        );

        p.setStock(
                parseEnteroONull(request.getParameter("stock"))
        );

        p.setStockMinimo(
                parseEnteroONull(request.getParameter("stockMinimo"))
        );

        String error = validar(p);

        if (error == null
                && productoDAO.existeCodigo(p.getCodigo(), p.getId())) {
            error = "Ya existe un producto con el código "
                    + p.getCodigo() + ".";
        }

        if (error != null) {
            Flash.error(request, error);
            mostrarLista(request, response, p, true);
            return;
        }

        if (p.getId() == 0) {
            productoDAO.insertar(p);
            Flash.exito(request,
                    "Producto registrado correctamente.");
        } else {
            productoDAO.actualizar(p);
            Flash.exito(request,
                    "Producto actualizado correctamente.");
        }

        response.sendRedirect(
                request.getContextPath() + "/productos");
    }

    private void eliminar(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws SQLException, IOException {

        int id = parseEntero(request.getParameter("id"), 0);

        productoDAO.eliminar(id);

        Flash.exito(request, "Producto eliminado correctamente.");

        response.sendRedirect(
                request.getContextPath() + "/productos");
    }

    /** Valida los datos recibidos desde el formulario. */
    private String validar(Producto p) {

        if (p.getCodigo().isEmpty() || p.getNombre().isEmpty()) {
            return "El código y el nombre son obligatorios.";
        }

        if (!p.getCodigo().matches("[A-Z0-9_-]{1,30}")) {
            return "El código solo puede tener letras, números, "
                    + "guiones y hasta 30 caracteres.";
        }

        if (p.getNombre().length() > 150) {
            return "El nombre no puede superar los 150 caracteres.";
        }

        if (p.getDescripcion().length() > 255) {
            return "La descripción no puede superar los 255 caracteres.";
        }

        if (!UNIDADES.contains(p.getUnidadMedida())) {
            return "Selecciona una unidad de medida válida.";
        }

        String errorPrecio =
                validarPrecio(p.getPrecioCompra(), "precio de compra");

        if (errorPrecio == null) {
            errorPrecio =
                    validarPrecio(p.getPrecioVenta(), "precio de venta");
        }

        if (errorPrecio != null) {
            return errorPrecio;
        }

        if (p.getStock() == null || p.getStock() < 0) {
            return "El stock debe ser un número entero mayor o igual a 0.";
        }

        if (p.getStockMinimo() == null || p.getStockMinimo() < 0) {
            return "El stock mínimo debe ser un número entero "
                    + "mayor o igual a 0.";
        }

        return null;
    }

    private String validarPrecio(BigDecimal precio, String nombre) {

        if (precio == null
                || precio.signum() < 0
                || precio.compareTo(PRECIO_MAXIMO) > 0
                || precio.stripTrailingZeros().scale() > 2) {

            return "El " + nombre
                    + " debe ser un monto válido (ej. 19.90).";
        }

        return null;
    }

    private String valor(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private int parseEntero(String texto, int porDefecto) {
        Integer numero = parseEnteroONull(texto);
        return numero == null ? porDefecto : numero;
    }

    private Integer parseEnteroONull(String texto) {
        try {
            return texto == null || texto.isBlank()
                    ? null
                    : Integer.valueOf(texto.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Acepta precios como 19.90 o 19,90. */
    private BigDecimal parseDecimal(String texto) {
        try {
            return texto == null || texto.isBlank()
                    ? null
                    : new BigDecimal(texto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}