package com.wellsfargo.bankingsystem.dto;

import com.wellsfargo.bankingsystem.model.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class AccountRequest {

    @NotNull
    private Long customerId;

    @NotNull
    private AccountType accountType;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }
    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal openingBalance) { this.openingBalance = openingBalance; }
}
