package com.mortgagetracker.mortgage;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "mortgage")
public class Mortgage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal propertyPrice;

    @Column(nullable = false)
    private BigDecimal deposit;

    @Column(nullable = false)
    private BigDecimal mortgageAmount;

    @Column(nullable = false)
    private int termYears;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private int paymentDay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepaymentType repaymentType;

    public Long getId() { return id; }

    public BigDecimal getPropertyPrice() { return propertyPrice; }
    public void setPropertyPrice(BigDecimal propertyPrice) { this.propertyPrice = propertyPrice; }

    public BigDecimal getDeposit() { return deposit; }
    public void setDeposit(BigDecimal deposit) { this.deposit = deposit; }

    public BigDecimal getMortgageAmount() { return mortgageAmount; }
    public void setMortgageAmount(BigDecimal mortgageAmount) { this.mortgageAmount = mortgageAmount; }

    public int getTermYears() { return termYears; }
    public void setTermYears(int termYears) { this.termYears = termYears; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public int getPaymentDay() { return paymentDay; }
    public void setPaymentDay(int paymentDay) { this.paymentDay = paymentDay; }

    public RepaymentType getRepaymentType() { return repaymentType; }
    public void setRepaymentType(RepaymentType repaymentType) { this.repaymentType = repaymentType; }
}
