package com.mortgagetracker.entry;

import com.mortgagetracker.mortgage.Mortgage;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One payment: total = rent (interest) + equity (principal). */
@Entity
@Table(name = "entry")
public class Entry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mortgage_id")
    private Mortgage mortgage;

    @Column(name = "entry_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private BigDecimal totalPayment;

    @Column(nullable = false)
    private BigDecimal rent;

    @Column(nullable = false)
    private BigDecimal equity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryType type;

    protected Entry() {}

    public Entry(Mortgage mortgage) {
        this.mortgage = mortgage;
    }

    public Long getId() { return id; }
    public Mortgage getMortgage() { return mortgage; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public BigDecimal getTotalPayment() { return totalPayment; }
    public void setTotalPayment(BigDecimal totalPayment) { this.totalPayment = totalPayment; }

    public BigDecimal getRent() { return rent; }
    public void setRent(BigDecimal rent) { this.rent = rent; }

    public BigDecimal getEquity() { return equity; }
    public void setEquity(BigDecimal equity) { this.equity = equity; }

    public EntryType getType() { return type; }
    public void setType(EntryType type) { this.type = type; }

    public boolean isMismatch() {
        return totalPayment.subtract(rent).subtract(equity).abs().compareTo(new BigDecimal("0.01")) > 0;
    }
}
