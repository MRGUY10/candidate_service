package com.crm.authservice.auth_api1.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "_candidate",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"firstname", "lastname"}), // Composite unique constraint
                @UniqueConstraint(columnNames = "email"), // Unique constraint for email
                @UniqueConstraint(columnNames = "phone")  // Unique constraint for phone
        }
)
@EntityListeners(AuditingEntityListener.class)
public class User implements UserDetails, Principal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String firstname;

    @Column(nullable = false)
    private String lastname;

    @Column(nullable = false, unique = true)
    private String phone; // Unique constraint applied

    @Column(nullable = false, unique = true)
    private String email; // Unique constraint applied

    private String password;
    private LocalDate dateOfBirth;

    @Column(nullable = false, unique = true)
    private String matricule; // Add matricule here

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, optional = true)
    private ProfilePhoto profilePhoto;

    private boolean isTemporaryPassword;
    private boolean accountLocked;
    private boolean enabled;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime lastModifiedDate;

    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "token_expiration_time")
    private LocalDateTime tokenExpirationTime;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<GrantedAuthority> authorities = new HashSet<>();

    // Other fields...

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities != null ? authorities : Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public String getFullName() {
        return this.firstname + " " + this.lastname;
    }

    @Override
    public String getName() {
        return email;
    }
}
