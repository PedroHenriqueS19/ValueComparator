package com.valuecomparison.controller;

import com.valuecomparison.dto.ProductDTO;
import com.valuecomparison.service.GeminiService;
import com.valuecomparison.service.ScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comparator")
@CrossOrigin(origins = "*")
public class ComparatorController {
    @Autowired
    private ScraperService scraperService;
    @Autowired
    private GeminiService geminiService;
    @GetMapping("/status")
    public String checkStatus() {
        return "AI-powered online server!";
    }
    // Returns the text of the report.
    @GetMapping("/report")
    public String generateReport(@RequestParam("q") String query) {
        System.out.println("1. Receiving Order': " + query); // Passo 1: Search for the data on the web. (Google Shopping / SerpApi)
        List<ProductDTO> products = scraperService.searchProducts(query);
        System.out.println("2. Products found: " + products.size());
        if (products.isEmpty()) {
            return "No products found to generate a report.";
        } // Passo 2: Send it to Artificial Intelligence for analysis.
        System.out.println("3. Sending to Gemini for analysis...");
        String report = geminiService.generatePurchaseReport(products, query);
        return report;
    }
}