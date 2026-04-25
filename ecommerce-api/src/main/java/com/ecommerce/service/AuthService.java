package com.ecommerce.service;

import com.ecommerce.audit.AuditService;
import com.ecommerce.dto.Dtos;
import com.ecommerce.entity.AuditLog;
import com.ecommerce.entity.User;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtils jwtUtils;
    private final AuditService auditService;

    @Transactional
    public Dtos.UserResponse register(Dtos.RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Ya existe un usuario con el email: " + request.email());
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        user = userRepository.save(user);
        auditService.logSuccess("REGISTER", "USER", user.getId(), request.email(),
                "Usuario registrado con rol " + request.role());

        return toUserResponse(user);
    }

    public Dtos.AuthResponse login(Dtos.LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (BadCredentialsException e) {
            auditService.logFailure("LOGIN", "USER", null, request.email(),
                    "Intento de login fallido");
            throw new BadCredentialsException("Credenciales inválidas");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtUtils.generateToken(userDetails);

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        auditService.logSuccess("LOGIN", "USER", user.getId(), request.email(),
                "Login exitoso");

        return new Dtos.AuthResponse(token, user.getEmail(), user.getName(), user.getRole().name());
    }

    private Dtos.UserResponse toUserResponse(User user) {
        return new Dtos.UserResponse(
                user.getId(), user.getName(), user.getEmail(),
                user.getRole().name(), user.isActive(), user.getCreatedAt()
        );
    }
}
