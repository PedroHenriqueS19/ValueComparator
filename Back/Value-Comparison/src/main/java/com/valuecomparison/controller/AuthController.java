package com.valuecomparison.controller;

import com.valuecomparison.dto.AuthenticationDTO;
import com.valuecomparison.model.User;
import com.valuecomparison.repository.UserRepository;
import com.valuecomparison.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserRepository repository;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody AuthenticationDTO data) {
        if (this.repository.findByLogin(data.login()).isPresent()) {
            return ResponseEntity.badRequest().body("Erro: Este login já está em uso!");
        }
        String passwordEncrypted = passwordEncoder.encode(data.senha());

        User newUser = new User(data.login(), passwordEncrypted);
        this.repository.save(newUser);

        return ResponseEntity.ok("Usuário cadastrado com sucesso!");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody AuthenticationDTO data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.senha());
        var auth = this.authenticationManager.authenticate(usernamePassword);
        var token = tokenService.generateToken((User) auth.getPrincipal());
        return ResponseEntity.ok(token);
    }
}