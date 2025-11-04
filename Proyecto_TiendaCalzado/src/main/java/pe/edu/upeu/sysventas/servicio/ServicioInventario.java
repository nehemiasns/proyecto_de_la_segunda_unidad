package pe.edu.upeu.sysventas.servicio;

import pe.edu.upeu.sysventas.modelo.Producto;
import pe.edu.upeu.sysventas.util.UtilArchivo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para manejo de inventario.
 * - Se apoya en ServicioProducto para leer/actualizar el stock real almacenado en datos/productos.csv
 * - Opcionalmente mantiene un archivo auxiliar datos/inventario.csv (productoId;stock) si quieres llevar una vista separada.
 */
public class ServicioInventario {

    private File archivoAux = new File("datos/inventario.csv");
    private ServicioProducto servicioProducto = new ServicioProducto();

    /**
     * Devuelve la lista de productos con su stock actual (tomado desde ServicioProducto).
     */
    public List<Producto> listarInventario() {
        return servicioProducto.todos();
    }

    /**
     * Obtiene el stock actual de un producto por su id.
     * @param productId id del producto
     * @return stock actual o -1 si no existe el producto
     */
    public int obtenerStock(String productId) {
        Producto p = servicioProducto.buscarPorId(productId);
        if (p == null) return -1;
        return p.getStock();
    }

    /**
     * Actualiza manualmente el stock de un producto.
     * Actualiza el registro en productos.csv a través de ServicioProducto.
     * También actualiza (o crea) la entrada en datos/inventario.csv (archivo auxiliar).
     *
     * @param productId id del producto
     * @param nuevoStock el stock a establecer (>=0)
     * @return true si la operación fue exitosa
     */
    public boolean actualizarStockManual(String productId, int nuevoStock) {
        if (nuevoStock < 0) return false;
        Producto p = servicioProducto.buscarPorId(productId);
        if (p == null) return false;

        p.setStock(nuevoStock);
        boolean ok = servicioProducto.actualizar(p);

        // actualizar archivo auxiliar (opcional)
        List<String> lines = UtilArchivo.leerLineas(archivoAux);
        List<String> out = new ArrayList<>();
        boolean found = false;
        for (String l : lines) {
            if (l.startsWith(productId + ";")) {
                out.add(productId + ";" + nuevoStock);
                found = true;
            } else out.add(l);
        }
        if (!found) out.add(productId + ";" + nuevoStock);
        UtilArchivo.escribirLineas(archivoAux, out);

        return ok;
    }

    /**
     * Reduce el stock de un producto en la cantidad indicada (uso típico después de una venta).
     * Asegura que el stock no baje de 0.
     *
     * @param productId id del producto
     * @param cantidad cantidad a reducir (>=1)
     */
    public void reducirStockPorVenta(String productId, int cantidad) {
        if (cantidad <= 0) return;
        Producto p = servicioProducto.buscarPorId(productId);
        if (p == null) return;
        int nuevo = Math.max(0, p.getStock() - cantidad);
        p.setStock(nuevo);
        servicioProducto.actualizar(p);

        // sincronizar archivo auxiliar (si existe)
        List<String> lines = UtilArchivo.leerLineas(archivoAux);
        List<String> out = new ArrayList<>();
        boolean found = false;
        for (String l : lines) {
            if (l.startsWith(productId + ";")) {
                out.add(productId + ";" + nuevo);
                found = true;
            } else out.add(l);
        }
        if (!found) out.add(productId + ";" + nuevo);
        UtilArchivo.escribirLineas(archivoAux, out);
    }

    /**
     * Devuelve una lista de productos cuyo stock es menor o igual al umbral (alerta de bajo stock).
     *
     * @param umbral valor entero que define el límite de alerta (ej. 5)
     * @return lista de productos con stock bajo o vacío si ninguno
     */
    public List<Producto> productosConStockBajo(int umbral) {
        List<Producto> out = new ArrayList<>();
        for (Producto p : servicioProducto.todos()) {
            if (p.getStock() <= umbral) out.add(p);
        }
        return out;
    }

    /**
     * Sincroniza el archivo auxiliar datos/inventario.csv con el stock actual de productos.csv.
     * Útil si quieres regenerar la vista auxiliar desde la fuente principal.
     */
    public void sincronizarArchivoAuxiliar() {
        List<String> lines = new ArrayList<>();
        for (Producto p : servicioProducto.todos()) {
            lines.add(p.getId() + ";" + p.getStock());
        }
        UtilArchivo.escribirLineas(archivoAux, lines);
    }
}
