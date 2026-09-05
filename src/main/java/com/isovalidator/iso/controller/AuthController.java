package com.isovalidator.iso.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.isovalidator.iso.DTO.Blog.LoginRequest;
import com.isovalidator.iso.DTO.Blog.LoginResponse;
import com.isovalidator.iso.Repository.UserRepository;
import com.isovalidator.iso.model.User;
import com.isovalidator.iso.security.JwtService;


@RestController
@RequestMapping("/v1/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserRepository userRepository) {

        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository
                .findByUsername(request.getUsername())
                .orElseThrow();

        String token = jwtService.generateToken(
                user.getUsername(),
                user.getRole()
        );

        return ResponseEntity.ok(
                new LoginResponse(
                        token,
                        user.getUsername(),
                        user.getRole()
                )
        );
    }

}
