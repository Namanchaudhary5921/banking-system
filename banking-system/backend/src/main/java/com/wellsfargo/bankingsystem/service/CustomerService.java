package com.wellsfargo.bankingsystem.service;

import com.wellsfargo.bankingsystem.dto.CustomerRequest;
import com.wellsfargo.bankingsystem.exception.DuplicateResourceException;
import com.wellsfargo.bankingsystem.exception.ResourceNotFoundException;
import com.wellsfargo.bankingsystem.model.Customer;
import com.wellsfargo.bankingsystem.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AuditService auditService;

    public CustomerService(CustomerRepository customerRepository, AuditService auditService) {
        this.customerRepository = customerRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Customer onboardCustomer(CustomerRequest request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("A customer with this email already exists");
        }
        if (customerRepository.existsByNationalId(request.getNationalId())) {
            throw new DuplicateResourceException("A customer with this national ID already exists");
        }

        Customer customer = new Customer(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getNationalId(),
                request.getPhone(),
                request.getAddress()
        );

        Customer saved = customerRepository.save(customer);
        auditService.log("CUSTOMER", saved.getId().toString(), "ONBOARDED",
                "New customer onboarded: " + saved.getFirstName() + " " + saved.getLastName(), "SYSTEM");
        return saved;
    }

    public Customer getById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    public List<Customer> getAll() {
        return customerRepository.findAll();
    }

    @Transactional
    public Customer update(Long id, CustomerRequest request) {
        Customer customer = getById(id);
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        Customer saved = customerRepository.save(customer);
        auditService.log("CUSTOMER", saved.getId().toString(), "UPDATED", "Customer profile updated", "SYSTEM");
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = getById(id);
        customerRepository.delete(customer);
        auditService.log("CUSTOMER", id.toString(), "DELETED", "Customer record removed", "SYSTEM");
    }
}
