package com.mortgagetracker.mortgage;

import com.mortgagetracker.mortgage.MortgageDtos.MortgageRequest;
import com.mortgagetracker.mortgage.MortgageDtos.MortgageResponse;
import com.mortgagetracker.mortgage.MortgageDtos.RateRequest;
import com.mortgagetracker.mortgage.MortgageDtos.RateResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MortgageController {

    private final MortgageService service;

    public MortgageController(MortgageService service) {
        this.service = service;
    }

    /** 204 when the mortgage has not been set up yet. */
    @GetMapping("/mortgage")
    public ResponseEntity<MortgageResponse> get() {
        return service.find()
                .map(m -> ResponseEntity.ok(MortgageResponse.of(m)))
                .orElse(ResponseEntity.noContent().build());
    }

    @PutMapping("/mortgage")
    public MortgageResponse save(@Valid @RequestBody MortgageRequest req) {
        return MortgageResponse.of(service.save(req));
    }

    @GetMapping("/rates")
    public List<RateResponse> rates() {
        return service.find()
                .map(m -> service.rates(m).stream().map(RateResponse::of).toList())
                .orElse(List.of());
    }

    @PostMapping("/rates")
    @ResponseStatus(HttpStatus.CREATED)
    public RateResponse addRate(@Valid @RequestBody RateRequest req) {
        return RateResponse.of(service.addRate(req.rate(), req.effectiveFrom()));
    }

    @DeleteMapping("/rates/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRate(@PathVariable long id) {
        service.deleteRate(id);
    }
}
