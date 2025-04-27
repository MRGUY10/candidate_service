package com.crm.authservice.auth_api1.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RegistrationRequest {

    @NotEmpty(message = "Firstname is mandatory")
    @NotNull(message = "Firstname is mandatory")
    private String firstname;

    @NotEmpty(message = "Lastname is mandatory")
    @NotNull(message = "Lastname is mandatory")
    private String lastname;

    @Email(message = "Email is not well formatted")
    @NotEmpty(message = "Email is mandatory")
    @NotNull(message = "Email is mandatory")
    private String email;

    @NotEmpty(message = "Phone is mandatory")
    @NotNull(message = "Phone is mandatory")
    @Pattern(regexp = "^[+]?[0-9]{10,13}$", message = "Phone number must be valid")
    private String phone;

    @NotNull(message = "Date of birth is mandatory")
    private LocalDate dateOfBirth;

    // Add a method to combine first and last name into full name
    public String getFullName() {
        return this.firstname + " " + this.lastname;
    }
}
