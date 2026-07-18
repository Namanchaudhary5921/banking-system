package com.wellsfargo.bankingsystem.security;

import com.wellsfargo.bankingsystem.exception.ResourceNotFoundException;
import com.wellsfargo.bankingsystem.model.AppUser;
import com.wellsfargo.bankingsystem.repository.AppUserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final AppUserRepository appUserRepository;

    public CurrentUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public AppUser getCurrentAppUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found: " + username));
    }
}
