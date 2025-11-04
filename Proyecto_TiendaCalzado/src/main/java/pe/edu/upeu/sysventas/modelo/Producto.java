package pe.edu.upeu.sysventas.modelo;

import java.io.Serializable;

public class Producto implements Serializable {
    private String id;
    private String nombre;
    private String categoria;
    private String talla;
    private String color;
    private double precio;
    private int stock;
    private String imagen;

    public Producto() {}

    public Producto(String id, String nombre, String categoria, String talla, String color, double precio, int stock, String imagen) {
        this.id = id; this.nombre = nombre; this.categoria = categoria; this.talla = talla; this.color = color; this.precio = precio; this.stock = stock; this.imagen = imagen;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getTalla() { return talla; }
    public void setTalla(String talla) { this.talla = talla; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }
}
