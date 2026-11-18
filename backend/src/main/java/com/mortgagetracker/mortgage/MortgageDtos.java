package com.mortgagetracker.mortgage;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class MortgageDtos {

    private MortgageDtos() {}

    public record MortgageRequest(
            @NotNull @DecimalMin(value = "0.01") BigDecimal propertyPrice,
            @NotNull @DecimalMin(value = "0") BigDecimal deposit,
            @NotNull @DecimalMin(value = "0.01") BigDecimal mortgageAmount,
            @NotNull @Min(1) @Max(50) Integer termYears,
            @NotNull LocalDate startDate,
            @NotNull @Min(1) @Max(31) Integer paymentDay,
            @NotNull RepaymentType repaymentType,
            /** Only used when the mortgage is first created. */
            @DecimalMin(value = "0") @DecimalMax(value = "100") BigDecimal initialRate) {}

    public record MortgageResponse(
            long id,
            BigDecimal propertyPrice,
            BigDecimal deposit,
            BigDecimal mortgageAmount,
            int termYears,
            LocalDate startDate,
            int paymentDay,
            RepaymentType repaymentType) {

        static MortgageResponse of(Mortgage m) {
            return new MortgageResponse(m.getId(), m.getPropertyPrice(), m.getDeposit(), m.getMortgageAmount(),
                    m.getTermYears(), m.getStartDate(), m.getPaymentDay(), m.getRepaymentType());
        }
    }

    public record RateRequest(
            @NotNull @DecimalMin(value = "0") @DecimalMax(value = "100") BigDecimal rate,
            @NotNull LocalDate effectiveFrom) {}

    public record RateResponse(long id, BigDecimal rate, LocalDate effectiveFrom) {

        static RateResponse of(InterestRate r) {
            return new RateResponse(r.getId(), r.getRate(), r.getEffectiveFrom());
        }
    }
}
