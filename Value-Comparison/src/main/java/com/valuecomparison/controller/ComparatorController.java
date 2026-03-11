package com.valuecomparison.controller;

import com.valuecomparison.dto.ProductDTO;
import com.valuecomparison.service.GeminiService;
import com.valuecomparison.service.ScraperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/comparator")
public class ComparatorController {
    private static final Logger logger = LoggerFactory.getLogger(ComparatorController.class);
    private final ScraperService scraperService;
    private final GeminiService geminiService;

    // 1. Injeção de Dependência via Construtor
    public ComparatorController(ScraperService scraperService, GeminiService geminiService) {
        this.scraperService = scraperService;
        this.geminiService = geminiService;
    }
    @GetMapping("/status")
    public ResponseEntity<String> checkStatus() {
        return ResponseEntity.ok("Online server integrated with AI!");
    }
    // 2. Uso do ResponseEntity para controlar os Códigos de Status HTTP
    @GetMapping("/report")
    public ResponseEntity<String> generateReport(@RequestParam("q") String query) {
        logger.info("=== Receiving report request for: '{}' ===", query);
        try {
            // Passo 1: Busca de dados na Web
            List<ProductDTO> products = fetchProducts(query);
            if (products.isEmpty()) {
                logger.warn("The search for '{}' No results were returned. Aborting AI.", query);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No products were found on the market to generate the report.");
            }
            // Passo 2: Geração do Relatório via Inteligência Artificial
            String report = generateAiReport(products, query);
            logger.info("=== Report generated successfully for: '{}' ===", query);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            // 3. Tratamento Centralizado de Erros no Controller
            logger.error("Failure in the orchestration of the report for '{}': {}", query, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An internal error occurred while processing your request: " + e.getMessage());
        }
    }
    private List<ProductDTO> fetchProducts(String query) {
        logger.debug("Activating the ScraperService to collect data...");
        List<ProductDTO> products = scraperService.searchProducts(query);
        logger.info("Collection Completed: {} products found.", products.size());
        return products;
    }
    private String generateAiReport(List<ProductDTO> products, String query) {
        logger.debug("Sending structured data to GeminiService...");
        return geminiService.generatePurchaseReport(products, query);
    }
}