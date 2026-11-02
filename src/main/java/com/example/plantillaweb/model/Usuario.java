package com.example.plantillaweb.model;

import java.time.LocalDateTime;

public class Usuario {

    private int idUsuario;
    private String nombre;
    private String rol;
    private String usuario;
    private String clave;

    public Usuario() {
    }

    public Usuario(int idUsuario, String nombre, String rol, String usuario, String clave) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.rol = rol;
        this.usuario = usuario;
        this.clave = clave;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    /** true si el usuario tiene el rol "Administrador". */
    public boolean isAdministrador() {
        return "Administrador".equalsIgnoreCase(rol);
    }

    // =========================================================================
    // MÉTODOS DE COMPATIBILIDAD (Para que UsuarioServlet y PerfilServlet no fallen)
    // =========================================================================

    public String getNombres() { return nombre; }
    public void setNombres(String nombres) { this.nombre = nombres; }

    public String getApellidos() { return ""; }
    public void setApellidos(String apellidos) { }

    public String getDni() { return ""; }
    public void setDni(String dni) { }

    public String getEmail() { return ""; }
    public void setEmail(String email) { }

    public String getPassword() { return clave; }
    public void setPassword(String password) { this.clave = password; }

    public int getIdRol() { return 1; }
    public void setIdRol(int idRol) { }

    public String getNombreRol() { return rol; }
    public void setNombreRol(String nombreRol) { this.rol = nombreRol; }

    public boolean isEstado() { return true; }
    public void setEstado(boolean estado) { }

    public String getFoto() { return null; }
    public void setFoto(String foto) { }

    public LocalDateTime getFechaRegistro() { return LocalDateTime.now(); }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { }

    public String getNombreCompleto() { return nombre; }
}