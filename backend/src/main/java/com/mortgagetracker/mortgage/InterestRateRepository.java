package com.mortgagetracker.mortgage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterestRateRepository extends JpaRepository<InterestRate, Long> {

    List<InterestRate> findByMortgageOrderByEffectiveFromAscIdAsc(Mortgage mortgage);

    long countByMortgage(Mortgage mortgage);
}
