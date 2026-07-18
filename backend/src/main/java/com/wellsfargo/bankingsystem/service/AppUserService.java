package com.wellsfargo.bankingsystem.service;

import com.wellsfargo.bankingsystem.exception.DuplicateResourceException;
import com.wellsfargo.bankingsystem.model.AppUser;
import com.wellsfargo.bankingsystem.model.Role;
import com.wellsfargo.bankingsystem.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerService customerService;
    private final AuditService auditService;

    public AppUserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
                           CustomerService customerService, AuditService auditService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.customerService = customerService;
        this.auditService = auditService;
    }

    public AppUser registerCustomerLogin(Long customerId, String username, String rawPassword) {
        if (appUserRepository.findByUsername(username).isPresent()) {
            throw new DuplicateResourceException("Username already taken: " + username);
        }
        customerService.getById(customerId);

        AppUser user = new AppUser(username, passwordEncoder.encode(rawPassword), Role.CUSTOMER, customerId);
        AppUser saved = appUserRepository.save(user);
        auditService.log("APP_USER", saved.getId().toString(), "REGISTERED",
                "Customer login created for customer " + customerId, "ADMIN");
        return saved;
    }

    public void seedDefaultStaffUsers() {
        createIfMissing("admin", "admin123", Role.ADMIN);
        createIfMissing("teller", "teller123", Role.TELLER);
    }

    private void createIfMissing(String username, String rawPassword, Role role) {
        if (appUserRepository.findByUsername(username).isEmpty()) {
            appUserRepository.save(new AppUser(username, passwordEncoder.encode(rawPassword), role, null));
        }
    }
}
