package pe.edu.upeu.sysventas.modelo;

import java.io.Serializable;

public class Usuario implements Serializable {
    private String username;
    private String passwordHash;
    private boolean admin;

    public Usuario() {}

    public Usuario(String username, String passwordHash, boolean admin) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.admin = admin;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }
}
