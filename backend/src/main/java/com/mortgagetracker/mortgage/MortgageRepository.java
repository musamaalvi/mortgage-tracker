package com.mortgagetracker.mortgage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MortgageRepository extends JpaRepository<Mortgage, Long> {

    Optional<Mortgage> findFirstByOrderByIdAsc();
}
