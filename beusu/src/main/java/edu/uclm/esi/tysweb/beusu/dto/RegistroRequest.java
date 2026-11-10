package edu.uclm.esi.tysweb.beusu.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegistroRequest(
    @NotBlank String nombre,
    @NotBlank String apellidos,
    @NotBlank String email,
    @NotBlank String municipio,
    @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitud,
    @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitud,
    
    // Nombres de las diapositivas del profesor: pwd1 y pwd2
    @NotBlank @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{6,}$") String pwd1,
    @NotBlank @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{6,}$") String pwd2
) {
    // Métodos helper de compatibilidad
    public String psw() {
        return pwd1;
    }

    public String psw2() {
        return pwd2;
    }

    @AssertTrue(message = "Las contraseñas deben coincidir")
    public boolean isPasswordConfirmationValid() {
        return pwd1 != null && pwd1.equals(pwd2);
    }

    // Compatibilidad
    public boolean isPasswordMatch() {
        return isPasswordConfirmationValid();
    }
}