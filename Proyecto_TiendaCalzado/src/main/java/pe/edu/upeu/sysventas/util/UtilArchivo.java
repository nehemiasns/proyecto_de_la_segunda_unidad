package pe.edu.upeu.sysventas.util;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class UtilArchivo {

    public static List<String> leerLineas(File f) {
        if (!f.exists()) return new ArrayList<>();
        try {
            return Files.readAllLines(f.toPath());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void escribirLineas(File f, List<String> lines) {
        try {
            File parent = f.getParentFile();
            if (parent != null) parent.mkdirs();
            Files.write(f.toPath(), lines);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String siguienteId(String prefijo) {
        return prefijo + System.currentTimeMillis();
    }
}
