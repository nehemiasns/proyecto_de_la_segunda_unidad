package pe.edu.upeu.sysventas.servicio;

import pe.edu.upeu.sysventas.modelo.Venta;
import pe.edu.upeu.sysventas.modelo.VentaItem;
import pe.edu.upeu.sysventas.util.UtilArchivo;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServicioVenta {

    private File archivo = new File("datos/ventas.csv"); // id;username;fechaHora;total;items(serializado)

    public boolean registrar(Venta venta) {
        List<String> lines = UtilArchivo.leerLineas(archivo);
        StringBuilder sb = new StringBuilder();
        for (VentaItem it : venta.getItems()) {
            if (sb.length() > 0) sb.append("::");
            sb.append(it.getProductId()).append("|").append(it.getProductName()).append("|").append(it.getQty()).append("|").append(it.getUnitPrice());
        }
        lines.add(venta.getId() + ";" + venta.getUsername() + ";" + venta.getFechaHora().toString() + ";" + venta.getTotal() + ";" + sb.toString());
        UtilArchivo.escribirLineas(archivo, lines);
        return true;
    }

    public List<Venta> todos() {
        List<Venta> out = new ArrayList<>();
        for (String l : UtilArchivo.leerLineas(archivo)) {
            String[] p = l.split(";", 5);
            if (p.length >= 5) {
                String id = p[0], user = p[1], dt = p[2], tot = p[3], items = p[4];
                List<VentaItem> its = new ArrayList<>();
                if (!items.isEmpty()) {
                    for (String s : items.split("::")) {
                        String[] a = s.split("\\|");
                        its.add(new VentaItem(a[0], a[1], Integer.parseInt(a[2]), Double.parseDouble(a[3])));
                    }
                }
                Venta v = new Venta(id, user, LocalDateTime.parse(dt), its, Double.parseDouble(tot));
                out.add(v);
            }
        }
        return out;
    }

    public List<Venta> porUsuario(String username) {
        List<Venta> out = new ArrayList<>();
        for (Venta v : todos()) if (v.getUsername().equalsIgnoreCase(username)) out.add(v);
        return out;
    }
}
