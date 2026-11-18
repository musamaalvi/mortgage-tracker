package com.mortgagetracker.mortgage;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "interest_rate")
public class InterestRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mortgage_id")
    private Mortgage mortgage;

    /** Annual rate in percent, e.g. 4.250 means 4.25%. */
    @Column(nullable = false)
    private BigDecimal rate;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    protected InterestRate() {}

    public InterestRate(Mortgage mortgage, BigDecimal rate, LocalDate effectiveFrom) {
        this.mortgage = mortgage;
        this.rate = rate;
        this.effectiveFrom = effectiveFrom;
    }

    public Long getId() { return id; }
    public Mortgage getMortgage() { return mortgage; }
    public BigDecimal getRate() { return rate; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
}
