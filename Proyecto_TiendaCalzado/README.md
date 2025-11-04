# Proyecto_TiendaCalzado

Proyecto JavaFX minimalista para gestión de tienda de calzado.

- Java 17 (configura en IntelliJ).
- Interfaz JavaFX sin librerías externas.
- Persistencia simple en CSV en la carpeta `datos/`.
- Usuario administrador por defecto:
    - usuario: **nehemias**
    - contraseña: **123456**

Para ejecutar en IntelliJ:
1. Importar como Maven Project.
2. Configurar JDK 17 en Project Structure.
3. Si tu JDK no incluye JavaFX, añade VM options en la Run Configuration:
   `--module-path /ruta/al/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml`
4. Ejecutar la clase `pe.edu.upeu.sysventas.MainApp`.
