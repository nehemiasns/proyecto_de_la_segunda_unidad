package pe.edu.upeu.sysventas.servicio;

import pe.edu.upeu.sysventas.modelo.Categoria;
import pe.edu.upeu.sysventas.util.UtilArchivo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ServicioCategoria {

    private File archivo = new File("datos/categorias.csv"); // id;nombre

    public List<Categoria> todos() {
        List<Categoria> out = new ArrayList<>();
        for (String l : UtilArchivo.leerLineas(archivo)) {
            String[] p = l.split(";", 2);
            if (p.length >= 2) out.add(new Categoria(p[0], p[1]));
        }
        return out;
    }

    public boolean crear(String nombre) {
        if (nombre == null || nombre.isEmpty()) return false;
        List<String> lines = UtilArchivo.leerLineas(archivo);
        String id = UtilArchivo.siguienteId("CAT");
        lines.add(id + ";" + nombre);
        UtilArchivo.escribirLineas(archivo, lines);
        return true;
    }

    public boolean actualizar(String id, String nombre) {
        if (id == null || id.isEmpty() || nombre == null || nombre.isEmpty()) return false;
        List<String> lines = UtilArchivo.leerLineas(archivo);
        List<String> out = new ArrayList<>();
        for (String l : lines) {
            if (l.startsWith(id + ";")) out.add(id + ";" + nombre);
            else out.add(l);
        }
        UtilArchivo.escribirLineas(archivo, out);
        return true;
    }

    public boolean eliminar(String id) {
        List<String> lines = UtilArchivo.leerLineas(archivo);
        List<String> out = new ArrayList<>();
        for (String l : lines) {
            if (!l.startsWith(id + ";")) out.add(l);
        }
        UtilArchivo.escribirLineas(archivo, out);
        return true;
    }
}
