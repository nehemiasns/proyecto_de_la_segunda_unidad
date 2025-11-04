package pe.edu.upeu.sysventas;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import pe.edu.upeu.sysventas.modelo.Usuario;
import pe.edu.upeu.sysventas.servicio.*;

public class VentanaDashboard {

    public static void mostrar(Stage stage, Usuario user) {
        ServicioProducto servicioProducto = new ServicioProducto();
        ServicioCategoria servicioCategoria = new ServicioCategoria();
        ServicioVenta servicioVenta = new ServicioVenta();
        ServicioUsuario servicioUsuario = new ServicioUsuario();

        // lista compartida de productos para que tienda y admin vean cambios en tiempo real
        ObservableList<pe.edu.upeu.sysventas.modelo.Producto> sharedProducts = FXCollections.observableArrayList(servicioProducto.todos());

        Label lbl = new Label("Bienvenido, " + user.getUsername());
        lbl.setStyle("-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:14px;");

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tienda = new Tab("Tienda");
        tienda.setContent(PanelCliente.crear(servicioProducto, servicioCategoria, servicioVenta, user, sharedProducts));

        tabs.getTabs().add(tienda);

        Tab historialTab = new Tab("Historial");
        Button btnAbrirHist = new Button("Abrir historial");
        btnAbrirHist.setOnAction(e -> {
            try {
                // si es admin muestra todo, si no muestra sólo del usuario
                HistorialVentas.mostrarModal((Stage) btnAbrirHist.getScene().getWindow(), servicioVenta, user.isAdmin() ? null : user.getUsername());
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        historialTab.setContent(new VBox(10, btnAbrirHist));
        tabs.getTabs().add(historialTab);

        if (user.isAdmin()) {
            Tab admin = new Tab("Administración");
            // Pasar un callback para que al agregar un producto se refresque la lista compartida
            Runnable onProductAdded = () -> {
                // Recargar la lista compartida en el hilo de JavaFX
                Platform.runLater(() -> {
                    sharedProducts.setAll(servicioProducto.todos());
                });
            };
            admin.setContent(PanelAdmin.crear(servicioProducto, servicioCategoria, servicioVenta, servicioUsuario, onProductAdded));
            tabs.getTabs().add(admin);
        }

        Button btnSalir = new Button("Cerrar sesión");
        btnSalir.setOnAction(e -> {
            stage.close();
            try {
                new MainApp().start(new Stage());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        BorderPane root = new BorderPane();
        HBox top = new HBox(12, lbl, btnSalir);
        top.setPadding(new Insets(10));
        root.setTop(top);
        root.setCenter(tabs);
        root.getStylesheets().add(VentanaDashboard.class.getResource("/estilo.css").toExternalForm());

        Scene scene = new Scene(root, 1000, 640);
        stage.setScene(scene);
        stage.setTitle("Panel - Tienda Calzado");
        stage.show();
    }
}
