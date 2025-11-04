package pe.edu.upeu.sysventas;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pe.edu.upeu.sysventas.servicio.ServicioUsuario;

public class VentanaRegistro {

    public static void mostrar(Stage owner, ServicioUsuario servicioUsuario) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Registrar usuario");

        TextField txtUsuario = new TextField();
        txtUsuario.setPromptText("Usuario");
        PasswordField txtPwd = new PasswordField();
        txtPwd.setPromptText("Contraseña");

        Button btnRegistrar = new Button("Registrar");
        btnRegistrar.setOnAction(e -> {
            boolean ok = servicioUsuario.registrar(txtUsuario.getText().trim(), txtPwd.getText(), false);
            if (ok) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Usuario registrado.", ButtonType.OK);
                a.showAndWait();
                stage.close();
            } else {
                Alert a = new Alert(Alert.AlertType.ERROR, "No se pudo registrar (usuario existe o datos vacíos).", ButtonType.OK);
                a.showAndWait();
            }
        });

        VBox v = new VBox(10, txtUsuario, txtPwd, btnRegistrar);
        v.setPadding(new Insets(12));
        v.setAlignment(Pos.CENTER);
        v.getStylesheets().add(VentanaRegistro.class.getResource("/estilo.css").toExternalForm());
        Scene sc = new Scene(v, 340, 220);
        stage.setScene(sc);
        stage.show();
    }
}
