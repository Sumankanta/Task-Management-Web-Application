package com.infy.tmwa.controller;

import com.infy.tmwa.dto.AuthRequest;
import com.infy.tmwa.dto.AuthResponse;
import com.infy.tmwa.dto.RegisterRequest;
import com.infy.tmwa.entity.UserDTO;
import com.infy.tmwa.repository.UserRepository;
import com.infy.tmwa.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;   // ✅ Inject repository

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterRequest request) {
        log.info("Register request received for email: {}", request.getEmail());
        try {
            authService.register(request);
            log.info("User registered successfully: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            log.error("Registration failed for email: {} — {}", request.getEmail(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        try {
            AuthResponse response = authService.login(request);
            log.info("Login successful for email: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Login failed for email: {} — {}", request.getEmail(), e.getMessage());
            throw e;
        }
    }

    @GetMapping("/users")
    public List<UserDTO> getUsers() {

        log.info("Fetching all users for assignment dropdown");

        return userRepository.findAll()
                .stream()
                .map(user -> new UserDTO(
                        user.getId(),
                        user.getFullName()
                ))
                .toList();
    }
}