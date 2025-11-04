package pe.edu.upeu.sysventas;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pe.edu.upeu.sysventas.modelo.Venta;
import pe.edu.upeu.sysventas.servicio.ServicioVenta;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistorialVentas {

    private static final DecimalFormat PRICE_FMT = new DecimalFormat("0.00");

    public static void mostrarModal(Stage owner, ServicioVenta servicioVenta, String usuario) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Historial de Ventas");

        ListView<String> lv = new ListView<>();
        List<Venta> ventas;
        if (usuario == null || usuario.isEmpty()) ventas = servicioVenta.todos(); else ventas = servicioVenta.porUsuario(usuario);
        for (Venta v : ventas) lv.getItems().add(formatResumen(v));

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setOnAction(e -> stage.close());

        Button btnVer = new Button("Ver comprobante");
        btnVer.setOnAction(e -> {
            int idx = lv.getSelectionModel().getSelectedIndex();
            if (idx < 0) return;
            Venta sel = ventas.get(idx);
            mostrarComprobante(stage, sel);
        });

        VBox vbox = new VBox(8, lv, new HBox(8, btnVer, btnCerrar));
        vbox.setPadding(new Insets(10));
        Scene sc = new Scene(vbox, 820, 420);
        stage.setScene(sc);
        stage.showAndWait();
    }

    public static void mostrarComprobante(Stage owner, Venta v) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Comprobante - " + nullToDash(v != null ? v.getId() : null));

        TextArea ta = new TextArea();
        ta.setEditable(false);
        ta.setWrapText(true);
        StringBuilder sb = new StringBuilder();

        // intentar leer datos del archivo asociado a la venta
        Map<String, String> datosArchivo = readClienteFileForVenta(v != null ? v.getId() : null);

        sb.append("COMPROBANTE DE VENTA\n");
        sb.append("ID: ").append(nullToDash(v != null ? v.getId() : null)).append("\n");
        sb.append("Vendedor: ").append(nullToDash(v != null ? v.getUsername() : null)).append("\n");

        // prioridad: primero intentar leer desde el objeto Venta via reflection
        String nombreDesdeVenta = getClienteNombre(v);
        String dniDesdeVenta = getClienteDni(v);

        String nombre = "-";
        String dni = null;

        if (nombreDesdeVenta != null && !nombreDesdeVenta.equals("-")) nombre = nombreDesdeVenta;
        else if (datosArchivo.containsKey("nombre")) nombre = datosArchivo.get("nombre");

        if (dniDesdeVenta != null && !dniDesdeVenta.trim().isEmpty()) dni = dniDesdeVenta;
        else if (datosArchivo.containsKey("dni")) dni = datosArchivo.get("dni");

        sb.append("Cliente: ").append(nullToDash(nombre)).append("\n");
        if (dni != null && !dni.trim().isEmpty()) sb.append("DNI Cliente: ").append(dni).append("\n");

        sb.append("Fecha: ").append(v != null && v.getFechaHora() != null ? v.getFechaHora().toString() : "-").append("\n");
        sb.append("-------------------------------------\n");

        if (v != null && v.getItems() != null && !v.getItems().isEmpty()) {
            for (var it : v.getItems()) {
                String prodName = safeGetProductNameFromItem(it);
                int qty = safeGetQtyFromItem(it);
                double unit = safeGetUnitPriceFromItem(it);
                sb.append(prodName).append(" x ").append(qty).append("  S/").append(PRICE_FMT.format(unit)).append("\n");
            }
        } else {
            sb.append("(No hay items registrados)\n");
        }

        sb.append("-------------------------------------\n");
        sb.append("TOTAL: S/").append(PRICE_FMT.format(v != null ? v.getTotal() : 0.0)).append("\n");

        ta.setText(sb.toString());

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setOnAction(e -> stage.close());

        VBox vbox = new VBox(8, ta, btnCerrar);
        vbox.setPadding(new Insets(10));
        Scene sc = new Scene(vbox, 520, 420);
        stage.setScene(sc);
        stage.showAndWait();
    }

    // formato del resumen (muestra cliente también)
    private static String formatResumen(Venta v) {
        String cliente = getClienteNombre(v);
        if ((cliente == null || cliente.equals("-")) && v != null) {
            // intentar leer archivo si no hay cliente en venta
            Map<String, String> datos = readClienteFileForVenta(v.getId());
            if (datos.containsKey("nombre")) cliente = datos.get("nombre");
        }
        String fecha = v != null && v.getFechaHora() != null ? v.getFechaHora().toString() : "-";
        return String.format("%s  |  Vendedor: %s  |  Cliente: %s  |  %s  |  S/%s",
                nullToDash(v != null ? v.getId() : null),
                nullToDash(v != null ? v.getUsername() : null),
                nullToDash(cliente),
                fecha,
                PRICE_FMT.format(v != null ? v.getTotal() : 0.0));
    }

    // Intenta obtener nombre del cliente usando varios getters o campos (reflection)
    private static String getClienteNombre(Venta v) {
        if (v == null) return "-";
        String[] getters = {
                "getCliente", "getClienteNombre", "getNombreCliente", "getNombre",
                "getCustomerName", "getClientName", "getClienteName"
        };
        for (String g : getters) {
            try {
                Method m = v.getClass().getMethod(g);
                Object r = m.invoke(v);
                if (r != null) return r.toString();
            } catch (Exception ignored) {}
        }
        String[] fields = {"clienteNombre", "cliente", "nombreCliente", "clienteName", "nombre", "clientName"};
        for (String fName : fields) {
            try {
                Field f = v.getClass().getDeclaredField(fName);
                f.setAccessible(true);
                Object r = f.get(v);
                if (r != null) return r.toString();
            } catch (Exception ignored) {}
        }
        return "-";
    }

    // Intenta obtener DNI del cliente si existe en Venta
    private static String getClienteDni(Venta v) {
        if (v == null) return null;
        String[] getters = {"getDni", "getDNI", "getClienteDni", "getDocumento", "getDocumentoIdentidad", "getClienteDocumento"};
        for (String g : getters) {
            try {
                Method m = v.getClass().getMethod(g);
                Object r = m.invoke(v);
                if (r != null) return r.toString();
            } catch (Exception ignored) {}
        }
        String[] fields = {"clienteDni", "dni", "documentoIdentidad", "documento", "dniCliente"};
        for (String fName : fields) {
            try {
                Field f = v.getClass().getDeclaredField(fName);
                f.setAccessible(true);
                Object r = f.get(v);
                if (r != null) return r.toString();
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static String nullToDash(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s;
    }

    // lee archivo data/venta_<id>.txt con formato linea=valor (nombre=...,dni=...)
    private static Map<String, String> readClienteFileForVenta(String ventaId) {
        Map<String, String> map = new HashMap<>();
        if (ventaId == null || ventaId.trim().isEmpty()) return map;
        Path file = Paths.get("data", "venta_" + ventaId + ".txt");
        if (!Files.exists(file)) return map;
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (String ln : lines) {
                if (ln == null) continue;
                int ix = ln.indexOf('=');
                if (ix <= 0) continue;
                String k = ln.substring(0, ix).trim();
                String v = ln.substring(ix + 1).trim();
                map.put(k, v);
            }
        } catch (IOException ignored) {}
        return map;
    }

    // Métodos seguros para leer campos de los items (evitan NullPointer/NoSuchMethod)
    private static String safeGetProductNameFromItem(Object item) {
        if (item == null) return "-";
        try {
            Method m = item.getClass().getMethod("getProductName");
            Object r = m.invoke(item);
            if (r != null) return r.toString();
        } catch (Exception ignored) {}
        try {
            Method m = item.getClass().getMethod("getNombre");
            Object r = m.invoke(item);
            if (r != null) return r.toString();
        } catch (Exception ignored) {}
        try {
            Method m = item.getClass().getMethod("getName");
            Object r = m.invoke(item);
            if (r != null) return r.toString();
        } catch (Exception ignored) {}
        try {
            Field f = item.getClass().getDeclaredField("productName");
            f.setAccessible(true);
            Object r = f.get(item);
            if (r != null) return r.toString();
        } catch (Exception ignored) {}
        try {
            Field f = item.getClass().getDeclaredField("nombre");
            f.setAccessible(true);
            Object r = f.get(item);
            if (r != null) return r.toString();
        } catch (Exception ignored) {}
        return "-";
    }

    private static int safeGetQtyFromItem(Object item) {
        if (item == null) return 0;
        try {
            Method m = item.getClass().getMethod("getQty");
            Object r = m.invoke(item);
            if (r instanceof Number) return ((Number) r).intValue();
        } catch (Exception ignored) {}
        try {
            Method m = item.getClass().getMethod("getCantidad");
            Object r = m.invoke(item);
            if (r instanceof Number) return ((Number) r).intValue();
        } catch (Exception ignored) {}
        try {
            Field f = item.getClass().getDeclaredField("qty");
            f.setAccessible(true);
            Object r = f.get(item);
            if (r instanceof Number) return ((Number) r).intValue();
        } catch (Exception ignored) {}
        return 0;
    }

    private static double safeGetUnitPriceFromItem(Object item) {
        if (item == null) return 0.0;
        try {
            Method m = item.getClass().getMethod("getUnitPrice");
            Object r = m.invoke(item);
            if (r instanceof Number) return ((Number) r).doubleValue();
        } catch (Exception ignored) {}
        try {
            Method m = item.getClass().getMethod("getPrecio");
            Object r = m.invoke(item);
            if (r instanceof Number) return ((Number) r).doubleValue();
        } catch (Exception ignored) {}
        try {
            Field f = item.getClass().getDeclaredField("unitPrice");
            f.setAccessible(true);
            Object r = f.get(item);
            if (r instanceof Number) return ((Number) r).doubleValue();
        } catch (Exception ignored) {}
        return 0.0;
    }
}
