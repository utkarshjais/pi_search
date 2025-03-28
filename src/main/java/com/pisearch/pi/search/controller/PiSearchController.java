package com.pisearch.pi.search.controller;

import com.pisearch.pi.search.services.PiSearch;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pi")
public class PiSearchController {

    private final PiSearch piSearchService;

    public PiSearchController(PiSearch piSearchService) {
        this.piSearchService = piSearchService;
    }

    @GetMapping("/search")
    public String searchInPi(
            @RequestParam String query,
            @RequestParam(defaultValue = "1M") String dataset) { // Default: 1 million digits

        String filePath = dataset.equalsIgnoreCase("1B") ? "pi_1b.pdf" : "pi_1m.pdf";

        int result = piSearchService.searchInPi(filePath, query);
        return (result != -1) ? "Found at index: " + result : "Not Found";
    }

    @GetMapping("/generate")
    public String generatePi(
            @RequestParam int number) {

        return piSearchService.generatePi( number);
    }
}