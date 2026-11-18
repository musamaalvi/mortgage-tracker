package com.mortgagetracker.entry;

import com.mortgagetracker.entry.EntryDtos.EntryRequest;
import com.mortgagetracker.entry.EntryDtos.EntryResponse;
import com.mortgagetracker.entry.EntryDtos.ImportResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/entries")
public class EntryController {

    private final EntryService service;

    public EntryController(EntryService service) {
        this.service = service;
    }

    @GetMapping
    public List<EntryResponse> list() {
        return service.list().stream().map(EntryResponse::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EntryResponse create(@Valid @RequestBody EntryRequest req) {
        return EntryResponse.of(service.create(req));
    }

    @PutMapping("/{id}")
    public EntryResponse update(@PathVariable long id, @Valid @RequestBody EntryRequest req) {
        return EntryResponse.of(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }

    @PostMapping("/import")
    public ImportResult importXlsx(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file is empty");
        }
        try (InputStream in = file.getInputStream()) {
            return service.importXlsx(in);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the uploaded file");
        }
    }
}
