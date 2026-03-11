package com.infy.tmwa.service;

import com.infy.tmwa.config.JwtService;
import com.infy.tmwa.dto.AuthRequest;
import com.infy.tmwa.dto.AuthResponse;
import com.infy.tmwa.dto.RegisterRequest;
import com.infy.tmwa.entity.User;
import com.infy.tmwa.entity.UserRole;
import com.infy.tmwa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public void register(RegisterRequest request) {

        log.info("Registration attempt for email: {}", request.getEmail());

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Registration failed — email already exists: {}", request.getEmail());
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        boolean isFirstUser = userRepository.count() == 0;
        UserRole role = isFirstUser ? UserRole.ADMIN : UserRole.MEMBER;

        log.info("Registering user: {} with role: {}", request.getEmail(), role);

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);

        log.info("User registered successfully — email: {}", user.getEmail());
    }

    public AuthResponse login(AuthRequest request) {

        log.info("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed — email not found: {}", request.getEmail());
                    return new BadCredentialsException("Invalid email or password");
                });

        if(!user.isActive()){
            throw new DisabledException("Account deactivated. Please contact your administrator.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed — incorrect password for email: {}", request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);

//        log.info("Login successful — token generated for email: {}", user.getEmail());

        log.info("Login successful for: {} | role: {}", user.getEmail(), user.getRole());

        return new AuthResponse(token);
    }
}