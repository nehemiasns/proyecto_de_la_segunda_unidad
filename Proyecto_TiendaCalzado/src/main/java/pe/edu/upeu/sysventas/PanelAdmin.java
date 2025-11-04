package pe.edu.upeu.sysventas;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pe.edu.upeu.sysventas.servicio.*;
import pe.edu.upeu.sysventas.modelo.Producto;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;

public class PanelAdmin {

    public static Pane crear(ServicioProducto servicioProducto, ServicioCategoria servicioCategoria, ServicioVenta servicioVenta, pe.edu.upeu.sysventas.servicio.ServicioUsuario servicioUsuario, Runnable onProductAdded) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        Label titulo = new Label("Panel de Administración");
        titulo.setStyle("-fx-text-fill:white; -fx-font-size:16px; -fx-font-weight:bold;");

        // Area categorías
        TextField txtCategoria = new TextField();
        txtCategoria.setPromptText("Nombre categoría");
        Button btnAddCat = new Button("Agregar categoría");
        btnAddCat.setOnAction(e -> {
            servicioCategoria.crear(txtCategoria.getText().trim());
            txtCategoria.clear();
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Categoría agregada.", ButtonType.OK);
            a.showAndWait();
        });

        // Area productos - formulario y tabla
        TextField idField = new TextField(); idField.setPromptText("ID"); idField.setDisable(true);
        TextField nom = new TextField(); nom.setPromptText("Nombre");
        TextField cat = new TextField(); cat.setPromptText("Categoría");
        TextField talla = new TextField(); talla.setPromptText("Talla");
        TextField color = new TextField(); color.setPromptText("Color");
        TextField precio = new TextField(); precio.setPromptText("Precio");
        TextField stock = new TextField(); stock.setPromptText("Stock");
        Button btnAddProd = new Button("Agregar/Guardar producto");

        TableView<Producto> tv = new TableView<>();
        ObservableList<Producto> items = FXCollections.observableArrayList(servicioProducto.todos());
        tv.setItems(items);
        TableColumn<Producto, String> cId = new TableColumn<>("ID"); cId.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<Producto, String> cNom = new TableColumn<>("Nombre"); cNom.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        TableColumn<Producto, String> cCat = new TableColumn<>("Categoría"); cCat.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        TableColumn<Producto, String> cTalla = new TableColumn<>("Talla"); cTalla.setCellValueFactory(new PropertyValueFactory<>("talla"));
        TableColumn<Producto, String> cColor = new TableColumn<>("Color"); cColor.setCellValueFactory(new PropertyValueFactory<>("color"));
        TableColumn<Producto, Integer> cStock = new TableColumn<>("Stock"); cStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        TableColumn<Producto, Double> cPrecio = new TableColumn<>("Precio"); cPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        tv.setPrefHeight(240);

        // resaltar filas con stock bajo
        tv.setRowFactory(tvrow -> new TableRow<Producto>(){
            @Override
            protected void updateItem(Producto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    if (item.getStock() <= 5) {
                        setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, rgba(80,0,120,0.2), rgba(30,0,60,0.05));");
                    } else setStyle("");
                }
            }
        });

        // seleccionar producto en la tabla para editar
        tv.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
            if (nv != null) {
                idField.setText(nv.getId());
                nom.setText(nv.getNombre());
                cat.setText(nv.getCategoria());
                talla.setText(nv.getTalla());
                color.setText(nv.getColor());
                precio.setText(String.valueOf(nv.getPrecio()));
                stock.setText(String.valueOf(nv.getStock()));
            }
        });

        Button btnDelete = new Button("Eliminar seleccionado");
        btnDelete.setOnAction(e -> {
            Producto sel = tv.getSelectionModel().getSelectedItem();
            if (sel == null) { Alert a = new Alert(Alert.AlertType.WARNING, "Seleccione un producto.", ButtonType.OK); a.showAndWait(); return; }
            servicioProducto.eliminar(sel.getId());
            items.setAll(servicioProducto.todos());
            if (onProductAdded != null) try { onProductAdded.run(); } catch (Exception ex){ex.printStackTrace();}
        });

        btnAddProd.setOnAction(e -> {
            try {
                if (nom.getText().trim().isEmpty()) { Alert a = new Alert(Alert.AlertType.WARNING, "Nombre requerido.", ButtonType.OK); a.showAndWait(); return; }
                Producto p = new Producto(idField.getText().isEmpty() ? null : idField.getText(), nom.getText().trim(), cat.getText().trim(), talla.getText().trim(), color.getText().trim(), Double.parseDouble(precio.getText()), Integer.parseInt(stock.getText()), "");
                if (p.getId() == null || p.getId().isEmpty()) servicioProducto.crear(p); else servicioProducto.actualizar(p);
                idField.clear(); nom.clear(); cat.clear(); talla.clear(); color.clear(); precio.clear(); stock.clear();
                items.setAll(servicioProducto.todos());
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Producto guardado.", ButtonType.OK); a.showAndWait();
                if (onProductAdded != null) try { onProductAdded.run(); } catch (Exception ex2) { ex2.printStackTrace(); }
            } catch (Exception ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Error al guardar producto: " + ex.getMessage(), ButtonType.OK);
                a.showAndWait();
            }
        });

        root.getChildren().addAll(titulo,
                new HBox(8, txtCategoria, btnAddCat),
                new Separator(),
                new Label("Agregar producto"),
                new HBox(6, nom, cat, talla, color, precio, stock),
                btnAddProd
        );

        root.getStylesheets().add(PanelAdmin.class.getResource("/estilo.css").toExternalForm());
        return root;
    }
}
