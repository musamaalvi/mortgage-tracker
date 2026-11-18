package com.mortgagetracker.summary;

import com.mortgagetracker.entry.EntryService;
import com.mortgagetracker.mortgage.Mortgage;
import com.mortgagetracker.mortgage.MortgageService;
import com.mortgagetracker.summary.SummaryDtos.Summary;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class SummaryController {

    private final MortgageService mortgageService;
    private final EntryService entryService;

    public SummaryController(MortgageService mortgageService, EntryService entryService) {
        this.mortgageService = mortgageService;
        this.entryService = entryService;
    }

    /** 204 when the mortgage has not been set up yet. */
    @GetMapping("/api/summary")
    @Transactional(readOnly = true)
    public ResponseEntity<Summary> summary() {
        return mortgageService.find()
                .map(m -> ResponseEntity.ok(calculate(m)))
                .orElse(ResponseEntity.noContent().build());
    }

    private Summary calculate(Mortgage m) {
        return new MortgageCalculator(m, mortgageService.rates(m), entryService.list(), LocalDate.now()).summarise();
    }
}
