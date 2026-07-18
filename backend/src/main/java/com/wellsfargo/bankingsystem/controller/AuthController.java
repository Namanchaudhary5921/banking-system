package com.wellsfargo.bankingsystem.controller;

import com.wellsfargo.bankingsystem.dto.RegisterCustomerLoginRequest;
import com.wellsfargo.bankingsystem.model.AppUser;
import com.wellsfargo.bankingsystem.service.AppUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserService appUserService;

    public AuthController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerCustomerLogin(@Valid @RequestBody RegisterCustomerLoginRequest request) {
        AppUser user = appUserService.registerCustomerLogin(request.getCustomerId(), request.getUsername(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "username", user.getUsername(),
                "role", user.getRole(),
                "customerId", user.getCustomerId()
        ));
    }
}
