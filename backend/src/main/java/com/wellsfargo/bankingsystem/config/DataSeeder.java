package com.wellsfargo.bankingsystem.config;

import com.wellsfargo.bankingsystem.service.AppUserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final AppUserService appUserService;

    public DataSeeder(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @Override
    public void run(String... args) {
        appUserService.seedDefaultStaffUsers();
    }
}
