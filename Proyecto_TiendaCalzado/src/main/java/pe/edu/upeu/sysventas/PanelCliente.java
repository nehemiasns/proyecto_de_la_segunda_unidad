package pe.edu.upeu.sysventas;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Pair;
import pe.edu.upeu.sysventas.modelo.Producto;
import pe.edu.upeu.sysventas.modelo.Usuario;
import pe.edu.upeu.sysventas.modelo.Venta;
import pe.edu.upeu.sysventas.modelo.VentaItem;
import pe.edu.upeu.sysventas.servicio.ServicioCategoria;
import pe.edu.upeu.sysventas.servicio.ServicioProducto;
import pe.edu.upeu.sysventas.servicio.ServicioVenta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * PanelCliente con persistencia de cliente a archivo por venta.
 */
public class PanelCliente {

    private static final DecimalFormat PRICE_FMT = new DecimalFormat("0.00");

    public static Pane crear(ServicioProducto servicioProducto,
                             ServicioCategoria servicioCategoria,
                             ServicioVenta servicioVenta,
                             Usuario usuario,
                             ObservableList<Producto> sharedProducts) {

        Objects.requireNonNull(servicioProducto, "servicioProducto null");
        Objects.requireNonNull(servicioVenta, "servicioVenta null");
        Objects.requireNonNull(sharedProducts, "sharedProducts null");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // ---------------- RIGHT: carrito ----------------
        VBox right = new VBox(8);
        right.setPrefWidth(360);

        Label lblCarrito = new Label("Carrito (0)");
        lblCarrito.setStyle("-fx-font-weight:bold; -fx-font-size:14px;");

        ObservableList<VentaItem> carrito = FXCollections.observableArrayList();
        TableView<VentaItem> tvCarrito = new TableView<>(carrito);
        tvCarrito.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tvCarrito.setPrefHeight(360);

        TableColumn<VentaItem, String> colNombre = new TableColumn<>("Producto");
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(ventaItemNombre(d.getValue())));
        colNombre.setMinWidth(140);

        TableColumn<VentaItem, Integer> colQty = new TableColumn<>("Cantidad");
        colQty.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getQty()).asObject());
        colQty.setCellFactory(col -> new SpinnerCell(tvCarrito)); // pasa la tabla para refresco

        TableColumn<VentaItem, String> colPrecio = new TableColumn<>("Precio U.");
        colPrecio.setCellValueFactory(d -> new SimpleStringProperty("S/" + PRICE_FMT.format(d.getValue().getUnitPrice())));

        TableColumn<VentaItem, String> colSub = new TableColumn<>("Subtotal");
        colSub.setCellValueFactory(d -> new SimpleStringProperty("S/" + PRICE_FMT.format(d.getValue().getQty() * d.getValue().getUnitPrice())));

        TableColumn<VentaItem, Void> colAcc = new TableColumn<>(" ");
        colAcc.setCellFactory(c -> new TableCell<>() {
            private final Button btnDel = new Button("Eliminar");
            {
                btnDel.setOnAction(e -> {
                    VentaItem it = getTableView().getItems().get(getIndex());
                    if (it != null) {
                        Platform.runLater(() -> carrito.remove(it));
                    }
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btnDel);
            }
        });

        tvCarrito.getColumns().addAll(colNombre, colQty, colPrecio, colSub, colAcc);

        Label lblTotal = new Label("Total: S/" + PRICE_FMT.format(0));
        lblTotal.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        lblTotal.setId("lblTotal");

        Button btnPagar = new Button("Pagar");
        btnPagar.setMaxWidth(Double.MAX_VALUE);
        Button btnVaciar = new Button("Vaciar carrito");
        btnVaciar.setMaxWidth(Double.MAX_VALUE);

        right.getChildren().addAll(lblCarrito, tvCarrito, lblTotal, btnPagar, btnVaciar);

        // listener para actualizar contador y total cuando cambia carrito
        carrito.addListener((ListChangeListener<VentaItem>) c -> {
            Platform.runLater(() -> {
                int unidades = carrito.stream().mapToInt(VentaItem::getQty).sum(); // total unidades
                lblCarrito.setText("Carrito (" + unidades + ")");
                double total = carrito.stream().mapToDouble(i -> i.getQty() * i.getUnitPrice()).sum();
                lblTotal.setText("Total: S/" + PRICE_FMT.format(total));
            });
        });

        // ---------------- LEFT: productos ----------------
        VBox left = new VBox(8);
        TextField txtBuscar = new TextField();
        txtBuscar.setPromptText("Buscar por nombre, color, talla o categoría");
        txtBuscar.setPrefWidth(520);

        FilteredList<Producto> filtered = new FilteredList<>(sharedProducts, p -> true);

        ListView<Producto> lvProductos = new ListView<>(filtered);
        lvProductos.setPrefWidth(560);
        // no fijar color en ListView — usar estilo de celdas
        lvProductos.setStyle("-fx-control-inner-background: transparent;");

        // ---------------- Helper: agregar al carrito (closure) ----------------
        BiConsumer<Producto, Integer> agregarAlCarrito = (producto, qty) -> {
            if (producto == null || qty <= 0) return;
            if (producto.getStock() < qty) {
                showAlert(Alert.AlertType.WARNING, "Stock insuficiente", "Stock disponible: " + producto.getStock());
                return;
            }
            Platform.runLater(() -> {
                Optional<VentaItem> existe = carrito.stream().filter(i -> Objects.equals(i.getProductId(), producto.getId())).findFirst();
                if (existe.isPresent()) {
                    VentaItem it = existe.get();
                    it.setQty(it.getQty() + qty);
                    tvCarrito.refresh();
                } else {
                    carrito.add(new VentaItem(producto.getId(), safeGetProductName(producto), qty, producto.getPrecio()));
                }
            });
        };

        root.getProperties().put("agregarAlCarrito", agregarAlCarrito);

        // cell factory captura agregarAlCarrito
        lvProductos.setCellFactory(list -> new ListCell<>() {
            private final HBox rootBox = new HBox(12);
            private final VBox info = new VBox(6);
            private final Label lblName = new Label();
            private final Label lblDetails = new Label();
            private final Spinner<Integer> spQty = new Spinner<>(1, 99, 1);
            private final Button btnAdd = new Button("Agregar");

            {
                spQty.setPrefWidth(70);
                info.getChildren().addAll(lblName, lblDetails);
                rootBox.getChildren().addAll(info, spQty, btnAdd);
                HBox.setHgrow(info, Priority.ALWAYS);
                rootBox.setAlignment(Pos.CENTER_LEFT);
                rootBox.setPadding(new Insets(6));
                // tarjeta ligera para distinguir
                rootBox.setStyle("-fx-background-color: rgba(240,240,240,0.6); -fx-background-radius:6;");

                // estilos seguros (no blanco)
                lblName.setStyle("-fx-font-weight:bold; -fx-text-fill: -fx-text-base-color; -fx-font-size:13px;");
                lblDetails.setStyle("-fx-text-fill: #444444; -fx-font-size:11px;");

                // botón usa la lambda capturada
                btnAdd.setOnAction(e -> {
                    Producto p = getItem();
                    if (p != null) agregarAlCarrito.accept(p, spQty.getValue());
                });
            }

            @Override protected void updateItem(Producto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    // Nombre y detalles claros y legibles en cualquier tema
                    String nombre = item.getNombre() == null ? "Sin nombre" : item.getNombre();
                    lblName.setText(nombre + "  —  S/" + PRICE_FMT.format(item.getPrecio()));

                    String cat = item.getCategoria() == null ? "-" : item.getCategoria();
                    String talla = item.getTalla() == null ? "-" : item.getTalla();
                    lblDetails.setText(cat + " • Talla: " + talla + " • Stock: " + item.getStock());

                    // si stock bajo, resaltamos pero con color legible
                    if (item.getStock() <= 5) lblDetails.setStyle("-fx-text-fill: #b30059; -fx-font-size:11px;");
                    else lblDetails.setStyle("-fx-text-fill: #444444; -fx-font-size:11px;");

                    // asegurar altura de celda para que se vean todos los campos
                    setPrefHeight(84);
                    setGraphic(rootBox);
                }
            }
        });

        // doble clic añade 1
        lvProductos.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                Producto p = lvProductos.getSelectionModel().getSelectedItem();
                if (p != null) agregarAlCarrito.accept(p, 1);
            }
        });

        // filtrado de búsqueda
        txtBuscar.textProperty().addListener((obs, ov, nv) -> {
            String q = nv == null ? "" : nv.toLowerCase().trim();
            filtered.setPredicate(prod -> {
                if (prod == null) return false;
                if (q.isEmpty()) return true;
                return safeContains(prod.getNombre(), q)
                        || safeContains(prod.getColor(), q)
                        || safeContains(prod.getCategoria(), q)
                        || safeContains(prod.getTalla(), q);
            });
            lvProductos.refresh();
        });

        left.getChildren().addAll(txtBuscar, lvProductos);

        // ---------------- acciones: btnVaciar / btnPagar / historial ----------------
        btnVaciar.setOnAction(e -> Platform.runLater(carrito::clear));

        btnPagar.setOnAction(e -> {
            if (carrito.isEmpty()) { showAlert(Alert.AlertType.WARNING, "Carrito vacío", "Agrega productos antes de pagar."); return; }

            // Dialog para pedir nombre y DNI
            Dialog<Pair<String, String>> dialog = new Dialog<>();
            dialog.setTitle("Datos del cliente");
            dialog.setHeaderText("Ingrese el nombre y DNI del cliente");

            ButtonType okType = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            TextField tfNombre = new TextField();
            tfNombre.setPromptText("Nombre completo");
            TextField tfDni = new TextField();
            tfDni.setPromptText("DNI (solo números)");

            // Nuevo: permitir sólo números en el campo DNI pero sin límite de dígitos
            tfDni.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null) return;
                if (!newVal.matches("\\d*")) {
                    tfDni.setText(newVal.replaceAll("[^\\d]", ""));
                }
            });

            grid.add(new Label("Nombre:"), 0, 0);
            grid.add(tfNombre, 1, 0);
            grid.add(new Label("DNI:"), 0, 1);
            grid.add(tfDni, 1, 1);

            Node okButton = dialog.getDialogPane().lookupButton(okType);
            okButton.setDisable(true);

            // habilitar cuando ambos campos tengan algo razonable
            tfNombre.textProperty().addListener((o, ov, nv) -> okButton.setDisable(nv == null || nv.trim().isEmpty() || tfDni.getText().trim().isEmpty()));
            tfDni.textProperty().addListener((o, ov, nv) -> okButton.setDisable(nv == null || nv.trim().isEmpty() || tfNombre.getText().trim().isEmpty()));

            dialog.getDialogPane().setContent(grid);
            Platform.runLater(tfNombre::requestFocus);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == okType) return new Pair<>(tfNombre.getText().trim(), tfDni.getText().trim());
                return null;
            });

            Optional<Pair<String, String>> result = dialog.showAndWait();
            if (!result.isPresent()) return; // cancelado

            String clienteNombre = result.get().getKey();
            String clienteDni = result.get().getValue();

            // Ahora aceptamos cualquier cantidad de dígitos; sólo verificamos que no esté vacío
            if (clienteDni == null || clienteDni.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "DNI inválido", "Debe ingresar el DNI (sólo números).");
                return;
            }

            double total = carrito.stream().mapToDouble(it -> it.getQty() * it.getUnitPrice()).sum();
            String id = "VEN" + System.currentTimeMillis();
            Venta venta = new Venta(id, usuario.getUsername(), LocalDateTime.now(), new ArrayList<>(carrito), total);

            // Intentar asignar nombre y dni a Venta vía setters comunes o campos (si existen)
            tryAssignClienteToVenta(venta, clienteNombre, clienteDni);

            // ADICIONAL: persistir nombre/dni en archivo (data/venta_<id>.txt)
            persistClienteToFile(id, clienteNombre, clienteDni);

            try {
                servicioVenta.registrar(venta);
                // reducir stock en servicio (intenta)
                carrito.forEach(it -> {
                    try { servicioProducto.reducirStock(it.getProductId(), it.getQty()); } catch (Exception ignored) {}
                });
                try { sharedProducts.setAll(servicioProducto.todos()); } catch (Exception ignored) {}
                Platform.runLater(carrito::clear);
                showAlert(Alert.AlertType.INFORMATION, "Compra exitosa", "Cliente: " + clienteNombre + "\nDNI: " + clienteDni + "\nTotal S/" + PRICE_FMT.format(total));
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Error", "No se pudo registrar la venta: " + ex.getMessage());
            }
        });

        Button btnVerHistorial = new Button("Ver historial");
        btnVerHistorial.setOnAction(e -> {
            try {
                Stage owner = (Stage) lvProductos.getScene().getWindow();
                HistorialVentas.mostrarModal(owner, servicioVenta, usuario.getUsername());
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Error", "No se pudo abrir historial de ventas.");
            }
        });

        HBox bottom = new HBox(8, btnVerHistorial);

        // ---------------- layout final ----------------
        root.setLeft(left);
        root.setRight(right);
        root.setBottom(bottom);

        // atajos
        root.setOnKeyPressed(k -> {
            if (k.getCode() == KeyCode.ESCAPE) txtBuscar.clear();
        });

        // cuando cambien sharedProducts, refrescar listview
        sharedProducts.addListener((ListChangeListener<Producto>) c -> Platform.runLater(() -> filtered.setPredicate(p -> true)));

        // asegurar vista inicial
        Platform.runLater(() -> {
            filtered.setPredicate(p -> true);
            lvProductos.refresh();
        });

        return root;
    }

    // guarda cliente en data/venta_<id>.txt (formato simple: nombre=...,dni=...)
    private static void persistClienteToFile(String ventaId, String nombre, String dni) {
        try {
            Path dir = Paths.get("data");
            if (!Files.exists(dir)) Files.createDirectories(dir);
            String fileName = "venta_" + ventaId + ".txt";
            Path file = dir.resolve(fileName);
            List<String> lines = Arrays.asList("nombre=" + (nombre == null ? "" : nombre),
                                               "dni=" + (dni == null ? "" : dni));
            Files.write(file, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ex) {
            // no interrumpir flujo por error en IO; opcional: loggear
            System.err.println("No se pudo persistir cliente a archivo: " + ex.getMessage());
        }
    }

    // intenta obtener nombre de VentaItem por varios getters comunes
    private static String ventaItemNombre(VentaItem item) {
        if (item == null) return "";
        try { Method m = item.getClass().getMethod("getName"); Object r = m.invoke(item); if (r != null) return r.toString(); } catch (Exception ignored) {}
        try { Method m = item.getClass().getMethod("getNombre"); Object r = m.invoke(item); if (r != null) return r.toString(); } catch (Exception ignored) {}
        try { Method m = item.getClass().getMethod("getProductName"); Object r = m.invoke(item); if (r != null) return r.toString(); } catch (Exception ignored) {}
        return item.getProductId() == null ? "" : item.getProductId().toString();
    }

    private static String safeGetProductName(Producto p) {
        if (p == null) return "";
        try { java.lang.reflect.Method m = p.getClass().getMethod("getNombre"); Object r = m.invoke(p); if (r != null) return r.toString(); } catch (Exception ignored) {}
        try { java.lang.reflect.Method m = p.getClass().getMethod("getName"); Object r = m.invoke(p); if (r != null) return r.toString(); } catch (Exception ignored) {}
        return p.getId() == null ? "" : p.getId().toString();
    }

    private static void tryAssignClienteToVenta(Venta venta, String nombre, String dni) {
        if (venta == null) return;
        try {
            // probar setters comunes para nombre
            List<String> nameSetters = Arrays.asList("setCliente", "setClienteNombre", "setNombreCliente", "setNombre", "setCustomerName", "setClientName");
            List<String> dniSetters = Arrays.asList("setDni", "setDNI", "setClienteDni", "setDocumento", "setDocumentoIdentidad", "setClienteDocumento");
            for (String s : nameSetters) {
                try { Method m = venta.getClass().getMethod(s, String.class); m.invoke(venta, nombre); break; } catch (Exception ignored) {}
            }
            for (String s : dniSetters) {
                try { Method m = venta.getClass().getMethod(s, String.class); m.invoke(venta, dni); break; } catch (Exception ignored) {}
            }
            // si no hay setters, intentar campos directos
            tryAssignField(venta, "clienteNombre", nombre);
            tryAssignField(venta, "cliente", nombre);
            tryAssignField(venta, "nombreCliente", nombre);
            tryAssignField(venta, "clienteName", nombre);
            tryAssignField(venta, "nombre", nombre);

            tryAssignField(venta, "clienteDni", dni);
            tryAssignField(venta, "dni", dni);
            tryAssignField(venta, "documentoIdentidad", dni);
            tryAssignField(venta, "documento", dni);
            tryAssignField(venta, "dniCliente", dni);
        } catch (Exception ignored) {}
    }

    private static void tryAssignField(Object obj, String fieldName, String value) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            if (f.getType().isAssignableFrom(String.class)) f.set(obj, value);
        } catch (Exception ignored) {}
    }

    private static void actualizarTotalLookup(Node any, TableView<VentaItem> tv) {
        try {
            if (any == null || tv == null) return;
            double total = tv.getItems().stream().mapToDouble(i -> i.getQty() * i.getUnitPrice()).sum();
            Node root = any.getScene().getRoot();
            if (root != null) {
                Node found = root.lookup("#lblTotal");
                if (found instanceof Label) ((Label) found).setText("Total: S/" + PRICE_FMT.format(total));
            }
        } catch (Exception ignored) {}
    }

    private static boolean safeContains(String src, String q) {
        if (src == null || q == null) return false;
        return src.toLowerCase().contains(q);
    }

    private static void showAlert(Alert.AlertType tipo, String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert a = new Alert(tipo, mensaje, ButtonType.OK);
            a.setTitle(titulo);
            a.setHeaderText(null);
            a.showAndWait();
        });
    }

    // SpinnerCell que actualiza cantidad y recalcula total (usa owner para refresco)
    private static class SpinnerCell extends TableCell<VentaItem, Integer> {
        private final Spinner<Integer> spinner;

        SpinnerCell(TableView<VentaItem> owner) {
            this.spinner = new Spinner<>(1, 999, 1);
            spinner.setEditable(true);
            spinner.valueProperty().addListener((obs, ov, nv) -> {
                if (getTableRow() == null) return;
                VentaItem item = (VentaItem) getTableRow().getItem();
                if (item != null && nv != null) {
                    item.setQty(nv);
                    if (owner != null) owner.refresh();
                }
            });
        }

        @Override protected void updateItem(Integer value, boolean empty) {
            super.updateItem(value, empty);
            if (empty || value == null) setGraphic(null);
            else {
                spinner.getValueFactory().setValue(value);
                setGraphic(spinner);
            }
        }
    }
}
