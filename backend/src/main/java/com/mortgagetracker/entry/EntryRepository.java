package com.mortgagetracker.entry;

import com.mortgagetracker.mortgage.Mortgage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface EntryRepository extends JpaRepository<Entry, Long> {

    List<Entry> findByMortgageOrderByDateAscIdAsc(Mortgage mortgage);

    Optional<Entry> findByIdAndMortgage(Long id, Mortgage mortgage);
}
