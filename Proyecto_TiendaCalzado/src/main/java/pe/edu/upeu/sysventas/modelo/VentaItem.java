package pe.edu.upeu.sysventas.modelo;

import java.io.Serializable;

public class VentaItem implements Serializable {
    private String productId;
    private String productName;
    private int qty;
    private double unitPrice;

    public VentaItem() {}

    public VentaItem(String productId, String productName, int qty, double unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.qty = qty;
        this.unitPrice = unitPrice;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
}