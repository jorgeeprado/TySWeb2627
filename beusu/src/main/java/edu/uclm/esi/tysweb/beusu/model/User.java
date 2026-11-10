package edu.uclm.esi.tysweb.beusu.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import edu.uclm.esi.tysweb.beusu.dto.RegistroRequest;

@Entity
public class User {

    @Id 
    @Column(length = 36)
    private String id;
    
    private String nombre;
    private String apellidos;
    
    // Obligatorio y no se puede repetir en la base de datos
    @Column(nullable = false, unique = true) 
    private String email;
    
    private String municipio;
    private Double latitud;
    private Double longitud;
    
    @Column(nullable = false)
    private String password;
    
    // Cumplimiento de la regla RN-CUE-01: Nacen inactivos hasta que confirmen el email
    private boolean activo = false;
    
    private String tokenCreacion;
    private Long horaCreacion;
    private Long horaConfirmacion;

    // Tokens de recuperación de contraseña independientes (RN-CUE-04, RT-CONF-02)
    private String tokenRecuperacion;
    private Long horaRecuperacion;

    // Estado de pago de alta para ciudadanos (RN-CIU-01, RN-CIU-02)
    private boolean pagadoAlta = false;

    // Necesario para distinguir entre "EMPLEADO_AYTO" y "CIUDADANO"
    private String rol; 

    // Campos para 2FA (diagrama del profesor: secretKey:String, fa2Activated:boolean)
    @Column(length = 100)
    private String secretKey;

    private boolean fa2Activated = false;


    public User() {} // Constructor vacío requerido por JPA

    // Constructor a partir de RegistroRequest (especificado en el diagrama de la diapositiva)
    public User(RegistroRequest request) {
        this.id = java.util.UUID.randomUUID().toString();
        this.nombre = request.nombre();
        this.apellidos = request.apellidos();
        this.email = request.email();
        this.municipio = request.municipio();
        this.latitud = request.latitud();
        this.longitud = request.longitud();
        this.tokenCreacion = java.util.UUID.randomUUID().toString();
        this.horaCreacion = System.currentTimeMillis();
        this.activo = false;
        this.fa2Activated = false;
        this.rol = "EMPLEADO_AYTO";
        this.pagadoAlta = false;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }

    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }

    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public boolean isActivo() { return activo; }
    public Boolean getActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public String getTokenCreacion() { return tokenCreacion; }
    public void setTokenCreacion(String tokenCreacion) { this.tokenCreacion = tokenCreacion; }

    public Long getHoraCreacion() { return horaCreacion; }
    public void setHoraCreacion(Long horaCreacion) { this.horaCreacion = horaCreacion; }

    public Long getHoraConfirmacion() { return horaConfirmacion; }
    public void setHoraConfirmacion(Long horaConfirmacion) { this.horaConfirmacion = horaConfirmacion; }

    public String getTokenRecuperacion() { return tokenRecuperacion; }
    public void setTokenRecuperacion(String tokenRecuperacion) { this.tokenRecuperacion = tokenRecuperacion; }

    public Long getHoraRecuperacion() { return horaRecuperacion; }
    public void setHoraRecuperacion(Long horaRecuperacion) { this.horaRecuperacion = horaRecuperacion; }

    public boolean isPagadoAlta() { return pagadoAlta; }
    public void setPagadoAlta(boolean pagadoAlta) { this.pagadoAlta = pagadoAlta; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public boolean isFa2Activated() { return fa2Activated; }
    public void setFa2Activated(boolean fa2Activated) { this.fa2Activated = fa2Activated; }
}
