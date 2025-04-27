package com.crm.authservice.auth_api1.Service;

import com.crm.authservice.auth_api1.Repository.TokenRepository;
import com.crm.authservice.auth_api1.Repository.UserRepository;
import com.crm.authservice.auth_api1.Request.AuthenticationRequest;
import com.crm.authservice.auth_api1.Request.UpdateProfileRequest;
import com.crm.authservice.auth_api1.Response.AuthenticationResponse;
import com.crm.authservice.auth_api1.Request.RegistrationRequest;
import com.crm.authservice.auth_api1.Response.UserDetailsResponse;
import com.crm.authservice.auth_api1.handle.UserNotFoundException;
import com.crm.authservice.auth_api1.models.EmailTemplateName;
import com.crm.authservice.auth_api1.filters.JwtService;
import com.crm.authservice.auth_api1.models.Token;
import com.crm.authservice.auth_api1.models.User;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final TokenRepository tokenRepository;

    @Value("${application.mailing.frontend.activation-url}")
    private String activationUrl;

    // Register method without password (send verification code)
    public void register(RegistrationRequest request) throws MessagingException {
        // Create user with all required details
        String matricule = generateMatricule(request.getFirstname(), request.getLastname());
        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .phone(request.getPhone()) // Set phone
                .matricule(matricule) // Assign matricule her
                .dateOfBirth(request.getDateOfBirth()) // Set date of birth
                .accountLocked(false)
                .enabled(false) // Ensure that the user is not enabled until verified
                .isTemporaryPassword(false) // Temporary password flag set to false
                .build();

        // Save the user without a password
        userRepository.save(user);

        // Generate and send the verification code
        sendVerificationCodeEmail(user);
    }
    private String generateMatricule(String firstname, String lastname) {
        String initials = firstname.substring(0, 1).toUpperCase() + lastname.substring(0, 1).toUpperCase();
        String uniqueNumber = String.format("%04d", System.currentTimeMillis() % 10000); // Generate unique number
        return initials + uniqueNumber; // Example: JD1234
    }

    // Method to send the verification code to the user's email
    private void sendVerificationCodeEmail(User user) throws MessagingException {
        String verificationCode = generateVerificationCode(4); // 4-digit code
        saveVerificationToken(user, verificationCode); // Save the verification code in the database

        // Send email with verification code
        emailService.sendEmail(
                user.getEmail(),
                user.getFullName(),
                EmailTemplateName.VERIFY_ACCOUNT, // You can create a template for this
                activationUrl,
                verificationCode,
                "Please verify your email address",
                null
        );
    }

    // Generate a random 4-digit verification code
    private String generateVerificationCode(int length) {
        String characters = "0123456789";
        StringBuilder codeBuilder = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();

        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }

        return codeBuilder.toString();
    }

    // Save the verification token to the database
    private void saveVerificationToken(User user, String verificationCode) {
        Token token = Token.builder()
                .token(verificationCode)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15)) // Expire after 15 minutes
                .user(user)
                .build();
        tokenRepository.save(token);
    }


    // Verify the code provided by the user
    @Transactional
    public void verifyEmail(String email, String verificationCode) {
        // Ensure the user exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account registered with the provided email"));


        // Ensure the token exists
        Token token = tokenRepository.findByTokenAndUser_Email(verificationCode, email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification code."));

        // Check if the token is expired
        if (LocalDateTime.now().isAfter(token.getExpiresAt())) {
            throw new RuntimeException("Verification code expired. Please request a new one.");
        }

        // Activate the user account
        user.setEnabled(true);
        userRepository.save(user);

        // Delete the token to prevent reuse
        tokenRepository.delete(token);

        // Send confirmation email
        sendVerificationSuccessEmail(user);
    }

    private void sendVerificationSuccessEmail(User user) {
        try {
            emailService.sendEmail(
                    user.getEmail(),
                    user.getFullName(),
                    EmailTemplateName.VERIFY_ACCOUNT,
                    null, // No confirmation URL needed
                    null, // No activation code needed
                    "Email Verification Successful",
                    null // No temporary password
            );
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send confirmation email: " + e.getMessage());
        }
    }



    // Method to set the password after successful verification
    public String setPassword(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Ensure the user is verified before setting the password
        if (!user.isEnabled()) {
            return "Please verify your email first.";
        }

        // Set the new password
        user.setPassword(passwordEncoder.encode(password)); // Encrypt the password
        userRepository.save(user); // Save the updated user

        return "Password set successfully. You can now log in.";
    }
    public void sendPasswordResetToken(String email) throws MessagingException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        // Generate a 4-digit reset token
        String resetToken = generateFourDigitToken();

        // Save the token
        Token token = Token.builder()
                .token(resetToken)
                .user(user)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(30)) // Token expires in 30 minutes
                .build();
        tokenRepository.save(token);

        // Send email
        emailService.sendEmail(
                user.getEmail(),
                user.getFullName(),
                EmailTemplateName.RESET_PASSWORD,
                null, // Confirmation URL not needed after verification
                resetToken, // Pass the 4-digit reset token to the email template
                "Password Reset Request",
                null // No temporary password for this case
        );
    }

    private String generateFourDigitToken() {
        SecureRandom random = new SecureRandom();
        int token = 1000 + random.nextInt(9000); // Generates a number between 1000 and 9999
        return String.valueOf(token);
    }


    private String generateActivationCode(int length) {
        String characters = "0123456789";
        StringBuilder codeBuilder = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();

        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }

        return codeBuilder.toString();
    }


    public void changePassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTemporaryPassword(false);
        userRepository.save(user);
    }



    public String resetPasswordWithToken(String token, String newPassword) {
        // Retrieve the token from the repository
        Optional<Token> savedTokenOpt = tokenRepository.findByToken(token);

        if (savedTokenOpt.isPresent()) {
            Token savedToken = savedTokenOpt.get();

            // Check if the token has expired
            if (LocalDateTime.now().isAfter(savedToken.getExpiresAt())) {
                return "Token has expired. Please request a new password reset.";
            }

            // Proceed with password reset
            User user = savedToken.getUser();
            user.setPassword(passwordEncoder.encode(newPassword)); // Encode the new password
            user.setTemporaryPassword(false); // Set temporary password flag to false
            userRepository.save(user); // Save the updated user

            // Send confirmation email after successful password reset
            sendPasswordResetConfirmationEmail(user);

            // Delete the token after it has been used
            tokenRepository.delete(savedToken);

            return "Password reset successfully.";
        } else {
            return "Invalid token. Please request a new password reset.";
        }
    }

    // Token validation for password reset
    public boolean isTokenValid(String token) {
        Optional<Token> savedTokenOpt = tokenRepository.findByToken(token);
        return savedTokenOpt.isPresent() && !LocalDateTime.now().isAfter(savedTokenOpt.get().getExpiresAt());
    }

    private void sendPasswordResetConfirmationEmail(User user) {
        try {
            emailService.sendEmail(
                    user.getEmail(),
                    user.getFullName(),
                    EmailTemplateName.PASSWORD_RESET_CONFIRMATION,
                    null,
                    null,
                    "Your Password Has Been Reset",
                    "Your password has been successfully reset. If you did not request this change, please contact support."
            );
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public User getUserInfoFromToken(String token) {
        // Extract email or username from the token
        String email = jwtService.extractUsername(token);

        // Fetch the user by email
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Transactional
    public void logout(String token) {
        Optional<Token> savedTokenOpt = tokenRepository.findByToken(token);
        if (savedTokenOpt.isPresent()) {
            Token savedToken = savedTokenOpt.get();
            savedToken.setExpiresAt(LocalDateTime.now()); // Mark as expired
            tokenRepository.save(savedToken); // Update the token status in the database
        }
    }


    public String activateAccount(String token) {
        Optional<Token> activationToken = tokenRepository.findByToken(token);

        if (activationToken.isPresent()) {
            Token tokenEntity = activationToken.get();
            if (LocalDateTime.now().isBefore(tokenEntity.getExpiresAt())) {
                User user = tokenEntity.getUser();
                user.setEnabled(true); // Activate user
                userRepository.save(user);
                tokenRepository.delete(tokenEntity); // Remove the activation token

                return "Account successfully activated!";
            } else {
                return "Activation token has expired!";
            }
        } else {
            return "Invalid activation token!";
        }
    }
    public User editProfile(UpdateProfileRequest request) {
        User existingUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + request.getUserId()));

        // Validate inputs
        if (request.getFirstname() == null || request.getFirstname().isEmpty()) {
            throw new IllegalArgumentException("Firstname cannot be empty.");
        }

        if (request.getLastname() == null || request.getLastname().isEmpty()) {
            throw new IllegalArgumentException("Lastname cannot be empty.");
        }

        if (request.getEmail() == null || !request.getEmail().matches("^.+@.+\\..+$")) {
            throw new IllegalArgumentException("Invalid email format.");
        }

        // Check email uniqueness
        if (!existingUser.getEmail().equals(request.getEmail())) {
            boolean emailExists = userRepository.existsByEmail(request.getEmail());
            if (emailExists) {
                throw new RuntimeException("Email is already in use by another account.");
            }
        }

        // Update user fields
        existingUser.setFirstname(request.getFirstname());
        existingUser.setLastname(request.getLastname());
        existingUser.setEmail(request.getEmail());

        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            if (request.getPassword().length() < 8) {
                throw new IllegalArgumentException("Password must be at least 8 characters long.");
            }
            existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Save user
        try {
            return userRepository.save(existingUser);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("The update failed due to duplicate or invalid data: " + e.getMessage(), e);
        }
    }



    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
            // Authenticate user credentials
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // Find the user in the database
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("candidate not found"));

            // Generate JWT token
            String token = jwtService.generateToken(user);

            // Return response
            return AuthenticationResponse.builder()
                    .token(token)
                    .message("Authentication successful")
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }
    public List<UserDetailsResponse> getAllCandidates() {
        List<User> users = userRepository.findAll();

        return users.stream().map(user -> UserDetailsResponse.builder()
                .id(user.getId())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .dateOfBirth(user.getDateOfBirth())
                .email(user.getEmail())
                .phone(user.getPhone())
                .matricule(user.getMatricule())
                .build()
        ).collect(Collectors.toList());
    }
    @Transactional
    public User updateMatricule(String oldMatricule, String newMatricule) {
        User user = (User) userRepository.findByMatricule(oldMatricule)
                .orElseThrow(() -> new IllegalArgumentException("User not found with matricule: " + oldMatricule));

        // Update matricule
        user.setMatricule(newMatricule);
        return userRepository.save(user);
    }
}
