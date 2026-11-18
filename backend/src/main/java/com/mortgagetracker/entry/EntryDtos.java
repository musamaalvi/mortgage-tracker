package com.mortgagetracker.entry;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class EntryDtos {

    private EntryDtos() {}

    public record EntryRequest(
            @NotNull LocalDate date,
            @NotNull @DecimalMin("0") BigDecimal totalPayment,
            @NotNull @DecimalMin("0") BigDecimal rent,
            @NotNull @DecimalMin("0") BigDecimal equity,
            @NotNull EntryType type) {}

    public record EntryResponse(
            long id,
            LocalDate date,
            BigDecimal totalPayment,
            BigDecimal rent,
            BigDecimal equity,
            EntryType type,
            boolean mismatch) {

        static EntryResponse of(Entry e) {
            return new EntryResponse(e.getId(), e.getDate(), e.getTotalPayment(), e.getRent(), e.getEquity(),
                    e.getType(), e.isMismatch());
        }
    }

    public record RowError(int row, String message) {}

    public record ImportResult(int imported, List<RowError> errors, List<Integer> mismatchedRows) {}
}
