package com.crm.authservice.auth_api1.controller;

import com.crm.authservice.auth_api1.Request.AuthenticationRequest;
import com.crm.authservice.auth_api1.Request.RegistrationRequest;
import com.crm.authservice.auth_api1.Request.UpdateProfileRequest;
import com.crm.authservice.auth_api1.Response.AuthenticationResponse;
import com.crm.authservice.auth_api1.Response.UserDetailsResponse;
import com.crm.authservice.auth_api1.Service.AuthenticationService;
import com.crm.authservice.auth_api1.handle.UserNotFoundException;
import com.crm.authservice.auth_api1.models.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("candidate")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")

@Tag(name = "Authentication Controller", description = "Handles user authentication and account management")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

    // Register a new user and initiate verification
    // Register a new user and initiate verification
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegistrationRequest request) {
        try {
            authenticationService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("User registered successfully. Please check your email for the verification code.");
        } catch (Exception e) {
            logger.error("Error during registration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    // Verify email with the code
    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String email, @RequestParam String code) {
        Map<String, String> response = new HashMap<>();
        try {
            // Call the service to verify email and code
            authenticationService.verifyEmail(email, code);

            // Return a structured success message
            response.put("message", "Email verified successfully. You can now set your password.");
            return ResponseEntity.ok(response);
        } catch (UsernameNotFoundException e) {
            logger.error("Verification failed: Email not found", e);
            response.put("error", "No account registered with the provided email.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (IllegalArgumentException e) {
            logger.error("Verification failed: Invalid code", e);
            response.put("error", "Invalid verification code.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (RuntimeException e) {
            logger.error("Verification failed: Token expired or other issue", e);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            logger.error("Unexpected error verifying email", e);
            response.put("error", "An unexpected error occurred. Please try again later.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    // Set password after email verification
    @PostMapping("/set-password")
    public ResponseEntity<String> setPassword(@RequestParam String email, @RequestParam String newPassword) {
        try {
            authenticationService.setPassword(email, newPassword);
            return ResponseEntity.ok("Password set successfully. You can now log in.");
        } catch (Exception e) {
            logger.error("Error setting password", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error setting password: " + e.getMessage());
        }
    }

    // Authenticate user (login)
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        try {
            AuthenticationResponse response = authenticationService.authenticate(request);
            return ResponseEntity.ok(response);
        } catch (UsernameNotFoundException e) {
            logger.warn("Invalid login attempt", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthenticationResponse.builder().message("Invalid credentials").build());
        } catch (Exception e) {
            logger.error("Authentication error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthenticationResponse.builder().message("Authentication error").build());
        }
    }


    // Activate user account (using the token sent to the user’s email)
    @GetMapping("/activate/{token}")
    public ResponseEntity<String> activateAccount(@PathVariable String token) {
        try {
            authenticationService.activateAccount(token);
            return ResponseEntity.ok("Account activated successfully.");
        } catch (UserNotFoundException e) {
            logger.warn("Invalid activation token", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Invalid activation token.");
        } catch (Exception e) {
            logger.error("Error during activation", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error activating account.");
        }
    }

    // Forgot password - send reset token
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        try {
            authenticationService.sendPasswordResetToken(email);
            return ResponseEntity.ok("Password reset token sent. Please check your email.");
        } catch (UserNotFoundException e) {
            logger.warn("User not found for email: {}", email, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No account found with the provided email.");
        } catch (Exception e) {
            logger.error("Error sending password reset token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error sending password reset token: " + e.getMessage());
        }
    }

    // Reset password with token
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        try {
            String result = authenticationService.resetPasswordWithToken(token, newPassword);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error resetting password", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error resetting password: " + e.getMessage());
        }
    }

    // Edit user profile
    @PutMapping("/edit-profile")
    public ResponseEntity<String> editProfile(@Valid @RequestBody UpdateProfileRequest request) {
        try {
            authenticationService.editProfile(request);
            return ResponseEntity.ok("Profile updated successfully.");
        } catch (Exception e) {
            logger.error("Error updating profile", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error updating profile: " + e.getMessage());
        }
    }

    // Change user password
    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestParam String oldPassword, @RequestParam String newPassword) {
        try {
            authenticationService.changePassword(oldPassword, newPassword);
            return ResponseEntity.ok("Password changed successfully.");
        } catch (Exception e) {
            logger.error("Error changing password", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error changing password: " + e.getMessage());
        }
    }
    @PostMapping("/logout")
    @Transactional
    public ResponseEntity<String> logout(@RequestParam String token) {
        try {
            authenticationService.logout(token);
            return ResponseEntity.ok("Logout successful.");
        } catch (Exception e) {
            logger.error("Error during logout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error during logout: " + e.getMessage());
        }
    }
    @GetMapping("/get-user-details")
    public ResponseEntity<UserDetailsResponse> getUserDetails(@RequestParam String token) {
        try {
            User user = authenticationService.getUserInfoFromToken(token);

            // Build response with all user information
            UserDetailsResponse response = UserDetailsResponse.builder()
                    .id(user.getId())
                    .firstname(user.getFirstname())
                    .lastname(user.getLastname())
                    .dateOfBirth(user.getDateOfBirth())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .matricule(user.getMatricule())
                    .build();

            return ResponseEntity.ok(response);
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @GetMapping("/get-all-candidates")
    public ResponseEntity<List<UserDetailsResponse>> getAllCandidates() {
        try {
            List<UserDetailsResponse> candidates = authenticationService.getAllCandidates();
            return ResponseEntity.ok(candidates);
        } catch (Exception e) {
            logger.error("Error retrieving candidates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
