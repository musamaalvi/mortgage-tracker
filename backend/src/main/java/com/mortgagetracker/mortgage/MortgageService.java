package com.mortgagetracker.mortgage;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class MortgageService {

    private final MortgageRepository mortgageRepository;
    private final InterestRateRepository rateRepository;

    public MortgageService(MortgageRepository mortgageRepository, InterestRateRepository rateRepository) {
        this.mortgageRepository = mortgageRepository;
        this.rateRepository = rateRepository;
    }

    /** The app tracks a single mortgage; this returns it if it has been set up. */
    @Transactional(readOnly = true)
    public Optional<Mortgage> find() {
        return mortgageRepository.findFirstByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public Mortgage require() {
        return find().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.CONFLICT, "Set up the mortgage first"));
    }

    @Transactional
    public Mortgage save(MortgageDtos.MortgageRequest req) {
        if (req.deposit().compareTo(req.propertyPrice()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deposit cannot exceed the property price");
        }
        Optional<Mortgage> existing = find();
        Mortgage m = existing.orElseGet(Mortgage::new);
        m.setPropertyPrice(req.propertyPrice());
        m.setDeposit(req.deposit());
        m.setMortgageAmount(req.mortgageAmount());
        m.setTermYears(req.termYears());
        m.setStartDate(req.startDate());
        m.setPaymentDay(req.paymentDay());
        m.setRepaymentType(req.repaymentType());
        m = mortgageRepository.save(m);

        if (existing.isEmpty()) {
            if (req.initialRate() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An initial interest rate is required");
            }
            rateRepository.save(new InterestRate(m, req.initialRate(), req.startDate()));
        }
        return m;
    }

    @Transactional(readOnly = true)
    public List<InterestRate> rates(Mortgage m) {
        return rateRepository.findByMortgageOrderByEffectiveFromAscIdAsc(m);
    }

    @Transactional
    public InterestRate addRate(BigDecimal rate, LocalDate effectiveFrom) {
        return rateRepository.save(new InterestRate(require(), rate, effectiveFrom));
    }

    @Transactional
    public void deleteRate(long id) {
        Mortgage m = require();
        InterestRate rate = rateRepository.findById(id)
                .filter(r -> r.getMortgage().getId().equals(m.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rate not found"));
        if (rateRepository.countByMortgage(m) <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one interest rate is required");
        }
        rateRepository.delete(rate);
    }
}
