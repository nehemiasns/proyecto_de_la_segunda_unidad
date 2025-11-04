package pe.edu.upeu.sysventas;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import pe.edu.upeu.sysventas.servicio.ServicioUsuario;
import pe.edu.upeu.sysventas.modelo.Usuario;

public class MainApp extends Application {

    private ServicioUsuario servicioUsuario = new ServicioUsuario();

    @Override
    public void start(Stage primaryStage) throws Exception {
        servicioUsuario.asegurarAdminPorDefecto();

        Label titulo = new Label("Tienda de Calzado - NeheStore");
        titulo.setStyle("-fx-font-size:18px; -fx-text-fill:white; -fx-font-weight:bold;");

        TextField txtUsuario = new TextField();
        txtUsuario.setPromptText("Usuario");

        PasswordField txtContrasena = new PasswordField();
        txtContrasena.setPromptText("Contraseña");

        Button btnEntrar = new Button("Iniciar sesión");
        Button btnRegistrar = new Button("Registrar");

        HBox hbBtns = new HBox(10, btnEntrar, btnRegistrar);
        hbBtns.setAlignment(Pos.CENTER);

        VBox vbox = new VBox(12, titulo, txtUsuario, txtContrasena, hbBtns);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-background-color: linear-gradient(to bottom, #0b0b0b, #2a0033); -fx-border-radius:12; -fx-background-radius:12;");

        StackPane root = new StackPane(vbox);
        root.setPadding(new Insets(40));
        root.getStylesheets().add(getClass().getResource("/estilo.css").toExternalForm());
        Scene scene = new Scene(root, 560, 380);

        primaryStage.setTitle("Proyecto Tienda Calzado");
        primaryStage.setScene(scene);
        primaryStage.show();

        btnEntrar.setOnAction(ev -> {
            String u = txtUsuario.getText().trim();
            String p = txtContrasena.getText();
            Usuario user = servicioUsuario.autenticar(u, p);
            if (user != null) {
                VentanaDashboard.mostrar(primaryStage, user);
            } else {
                Alert a = new Alert(Alert.AlertType.ERROR, "Usuario o contraseña incorrectos", ButtonType.OK);
                a.showAndWait();
            }
        });

        btnRegistrar.setOnAction(ev -> {
            VentanaRegistro.mostrar(primaryStage, servicioUsuario);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
