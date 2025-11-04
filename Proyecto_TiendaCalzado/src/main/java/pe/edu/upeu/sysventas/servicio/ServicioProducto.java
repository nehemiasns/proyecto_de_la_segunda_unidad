package pe.edu.upeu.sysventas.servicio;

import pe.edu.upeu.sysventas.modelo.Producto;
import pe.edu.upeu.sysventas.util.UtilArchivo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ServicioProducto {

    private File archivo = new File("datos/productos.csv"); // id;nombre;categoria;talla;color;precio;stock;imagen

    public List<Producto> todos() {
        List<Producto> out = new ArrayList<>();
        for (String l : UtilArchivo.leerLineas(archivo)) {
            String[] p = l.split(";", -1);
            if (p.length >= 8) {
                Producto prod = new Producto(p[0], p[1], p[2], p[3], p[4], Double.parseDouble(p[5]), Integer.parseInt(p[6]), p[7]);
                out.add(prod);
            }
        }
        return out;
    }

    public boolean crear(Producto p) {
        List<String> lines = UtilArchivo.leerLineas(archivo);
        String id = UtilArchivo.siguienteId("PRD");
        // asignar id al producto y persistir
        p.setId(id);
        lines.add(id + ";" + p.getNombre() + ";" + p.getCategoria() + ";" + p.getTalla() + ";" + p.getColor() + ";" + p.getPrecio() + ";" + p.getStock() + ";" + (p.getImagen() == null ? "" : p.getImagen()));
        UtilArchivo.escribirLineas(archivo, lines);
        return true;
    }

    public boolean actualizar(Producto p) {
        List<String> lines = UtilArchivo.leerLineas(archivo);
        List<String> out = new ArrayList<>();
        for (String l : lines) {
            if (l.startsWith(p.getId() + ";")) {
                out.add(p.getId() + ";" + p.getNombre() + ";" + p.getCategoria() + ";" + p.getTalla() + ";" + p.getColor() + ";" + p.getPrecio() + ";" + p.getStock() + ";" + (p.getImagen() == null ? "" : p.getImagen()));
            } else out.add(l);
        }
        UtilArchivo.escribirLineas(archivo, out);
        return true;
    }

    public boolean eliminar(String id) {
        List<String> lines = UtilArchivo.leerLineas(archivo);
        List<String> out = new ArrayList<>();
        for (String l : lines) if (!l.startsWith(id + ";")) out.add(l);
        UtilArchivo.escribirLineas(archivo, out);
        return true;
    }

    public Producto buscarPorId(String id) {
        for (Producto p : todos()) if (p.getId().equals(id)) return p;
        return null;
    }

    public List<Producto> buscar(String q) {
        q = q == null ? "" : q.toLowerCase();
        List<Producto> out = new ArrayList<>();
        for (Producto p : todos()) {
            if (p.getNombre().toLowerCase().contains(q) || p.getColor().toLowerCase().contains(q) || p.getCategoria().toLowerCase().contains(q) || p.getTalla().toLowerCase().contains(q))
                out.add(p);
        }
        return out;
    }

    public void reducirStock(String id, int qty) {
        Producto p = buscarPorId(id);
        if (p != null) {
            p.setStock(Math.max(0, p.getStock() - qty));
            actualizar(p);
        }
    }
}
