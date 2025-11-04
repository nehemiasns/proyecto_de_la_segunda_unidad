package pe.edu.upeu.sysventas.modelo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class Venta implements Serializable {
    private String id;
    private String username;
    private LocalDateTime fechaHora;
    private List<VentaItem> items;
    private double total;

    public Venta() {}

    public Venta(String id, String username, LocalDateTime fechaHora, List<VentaItem> items, double total) {
        this.id = id; this.username = username; this.fechaHora = fechaHora; this.items = items; this.total = total;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public List<VentaItem> getItems() { return items; }
    public void setItems(List<VentaItem> items) { this.items = items; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
}
