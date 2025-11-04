package pe.edu.upeu.sysventas.servicio;

import pe.edu.upeu.sysventas.modelo.Usuario;
import pe.edu.upeu.sysventas.util.UtilArchivo;
import pe.edu.upeu.sysventas.util.UtilHash;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ServicioUsuario {

    private File archivo = new File("datos/usuarios.csv"); // username;hash;admin(1/0)

    public boolean registrar(String username, String password, boolean admin) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) return false;
        List<String> lines = UtilArchivo.leerLineas(archivo);
        for (String l : lines) {
            if (l.split(";")[0].equalsIgnoreCase(username)) return false;
        }
        String h = UtilHash.sha256(password);
        lines.add(username + ";" + h + ";" + (admin ? "1" : "0"));
        UtilArchivo.escribirLineas(archivo, lines);
        return true;
    }

    public Usuario autenticar(String username, String password) {
        List<String> lines = UtilArchivo.leerLineas(archivo);
        String h = UtilHash.sha256(password);
        for (String l : lines) {
            String[] p = l.split(";");
            if (p.length >= 3 && p[0].equalsIgnoreCase(username) && p[1].equals(h)) {
                return new Usuario(p[0], p[1], p[2].equals("1"));
            }
        }
        return null;
    }

    public void asegurarAdminPorDefecto() {
        List<String> lines = UtilArchivo.leerLineas(archivo);
        boolean encontrado = false;
        for (String l : lines) {
            if (l.split(";")[0].equalsIgnoreCase("nehemias")) { encontrado = true; break; }
        }
        if (!encontrado) registrar("nehemias", "123456", true);
    }

    public List<Usuario> todos() {
        List<Usuario> out = new ArrayList<>();
        for (String l : UtilArchivo.leerLineas(archivo)) {
            String[] p = l.split(";");
            if (p.length >= 3) out.add(new Usuario(p[0], p[1], p[2].equals("1")));
        }
        return out;
    }
}
