package com.mortgagetracker.entry;

import com.mortgagetracker.entry.EntryDtos.EntryRequest;
import com.mortgagetracker.entry.EntryDtos.ImportResult;
import com.mortgagetracker.entry.XlsxEntryParser.ParseResult;
import com.mortgagetracker.entry.XlsxEntryParser.ParsedRow;
import com.mortgagetracker.mortgage.Mortgage;
import com.mortgagetracker.mortgage.MortgageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class EntryService {

    private final EntryRepository repository;
    private final MortgageService mortgageService;

    public EntryService(EntryRepository repository, MortgageService mortgageService) {
        this.repository = repository;
        this.mortgageService = mortgageService;
    }

    @Transactional(readOnly = true)
    public List<Entry> list() {
        return mortgageService.find().map(repository::findByMortgageOrderByDateAscIdAsc).orElse(List.of());
    }

    @Transactional
    public Entry create(EntryRequest req) {
        Entry e = new Entry(mortgageService.require());
        apply(e, req);
        return repository.save(e);
    }

    @Transactional
    public Entry update(long id, EntryRequest req) {
        Entry e = get(id);
        apply(e, req);
        return e;
    }

    @Transactional
    public void delete(long id) {
        repository.delete(get(id));
    }

    @Transactional
    public ImportResult importXlsx(InputStream in) {
        Mortgage mortgage = mortgageService.require();
        ParseResult parsed;
        try {
            parsed = new XlsxEntryParser().parse(in);
        } catch (IOException | RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the file as an Excel workbook");
        }

        List<Entry> entries = new ArrayList<>();
        List<Integer> mismatched = new ArrayList<>();
        for (ParsedRow row : parsed.rows()) {
            Entry e = new Entry(mortgage);
            e.setDate(row.date());
            e.setTotalPayment(row.total());
            e.setRent(row.rent());
            e.setEquity(row.equity());
            // An all-equity payment (no rent) is treated as an overpayment.
            e.setType(row.rent().signum() == 0 ? EntryType.OVERPAYMENT : EntryType.REGULAR);
            if (e.isMismatch()) {
                mismatched.add(row.row());
            }
            entries.add(e);
        }
        repository.saveAll(entries);
        return new ImportResult(entries.size(), parsed.errors(), mismatched);
    }

    private Entry get(long id) {
        return repository.findByIdAndMortgage(id, mortgageService.require())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entry not found"));
    }

    private static void apply(Entry e, EntryRequest req) {
        e.setDate(req.date());
        e.setTotalPayment(req.totalPayment());
        e.setRent(req.rent());
        e.setEquity(req.equity());
        e.setType(req.type());
    }
}
