package com.crm.authservice.auth_api1.Response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class UserDetailsResponse {
    private  int id;
    private String firstname;
    private String lastname;
    private LocalDate dateOfBirth;
    private String email;
    private String phone;
    private String matricule;


}
