package com.pisearch.pi.search.controller;

import com.pisearch.pi.search.services.PiSearch;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search")
public class PiSearchController {

    private final PiSearch piSearchService;

    public PiSearchController(PiSearch piSearchService) {
        this.piSearchService = piSearchService;
    }

    @GetMapping
    public String searchInPi(
            @RequestParam String query,
            @RequestParam(defaultValue = "1M") String dataset) {

        String filePath = "pi_1m.pdf";  // Default: 1 million digits

        int result = piSearchService.searchInPi(filePath, query);
        return (result != -1) ? "Found at index: " + result : "Not Found";
    }
}