package com.example.plantillaweb.model;

/** Representa una fila de la tabla rol. Solo datos, sin lógica. */
public class Rol {

    private int idRol;
    private String nombre;

    public Rol() {
    }

    public int getIdRol() {
        return idRol;
    }

    public void setIdRol(int idRol) {
        this.idRol = idRol;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
