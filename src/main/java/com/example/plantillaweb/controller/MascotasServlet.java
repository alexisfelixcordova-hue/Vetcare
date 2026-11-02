package com.example.plantillaweb.controller;

import com.example.plantillaweb.dao.MascotaDAO;
import com.example.plantillaweb.model.Mascota;
import com.example.plantillaweb.util.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "mascotasServlet", value = "/mascota")
public class MascotasServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(MascotasServlet.class.getName());
    private final MascotaDAO mascotaDAO = new MascotaDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            // Obtenemos la lista de mascotas unidas con sus dueños (clientes)
            List<Mascota> listaMascotas = mascotaDAO.listarConCliente();
            request.setAttribute("listaMascotas", listaMascotas);

            // Redirigimos a la vista JSP exacta dentro de la carpeta Mascota
            request.getRequestDispatcher("/WEB-INF/views/Mascota/mascota.jsp").forward(request, response);
        } catch (SQLException e) {
            e.printStackTrace();
            LOG.log(Level.SEVERE, "Error al listar mascotas", e);

            Flash.error(request, "Error al cargar la lista de mascotas: " + e.getMessage());

            request.setAttribute("listaMascotas", null);
            request.getRequestDispatcher("/WEB-INF/views/Mascota/mascota.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            int idCliente = Integer.parseInt(request.getParameter("idCliente"));
            String nombre = request.getParameter("nombre");
            String especie = request.getParameter("especie");
            String raza = request.getParameter("raza");
            String sexo = request.getParameter("sexo");
            String edad = request.getParameter("edad");
            double peso = Double.parseDouble(request.getParameter("peso"));

            Mascota m = new Mascota();
            m.setIdCliente(idCliente);
            m.setNombre(nombre);
            m.setEspecie(especie);
            m.setRaza(raza);
            m.setSexo(sexo);
            m.setEdad(edad);
            m.setPeso(peso);
            m.setEstado("Activo");

            // Guardamos en la base de datos
            mascotaDAO.insertar(m);

            // Mensaje de éxito
            Flash.exito(request, "Mascota registrada correctamente");
        } catch (Exception e) {
            e.printStackTrace();
            LOG.log(Level.SEVERE, "Error al registrar mascota", e);
            Flash.error(request, "Error al registrar: " + e.getMessage());
        }

        // Redirigimos de vuelta a la ruta /mascota
        response.sendRedirect(request.getContextPath() + "/mascota");
    }
}