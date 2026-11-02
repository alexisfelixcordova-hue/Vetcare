
package com.example.plantillaweb.model;

import java.math.BigDecimal;

public class ResumenInventario {

    private int totalProductos;
    private int stockBajo;
    private int agotados;
    private BigDecimal valorInventario = BigDecimal.ZERO;

    public int getTotalProductos() {
        return totalProductos;
    }

    public void setTotalProductos(int totalProductos) {
        this.totalProductos = totalProductos;
    }

    public int getStockBajo() {
        return stockBajo;
    }

    public void setStockBajo(int stockBajo) {
        this.stockBajo = stockBajo;
    }

    public int getAgotados() {
        return agotados;
    }

    public void setAgotados(int agotados) {
        this.agotados = agotados;
    }

    public BigDecimal getValorInventario() {
        return valorInventario;
    }

    public void setValorInventario(BigDecimal valorInventario) {
        this.valorInventario = valorInventario;
    }
}
