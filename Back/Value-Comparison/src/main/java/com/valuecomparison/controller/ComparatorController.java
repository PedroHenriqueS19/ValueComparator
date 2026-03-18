package com.valuecomparison.controller;

import com.valuecomparison.dto.ProductDTO;
import com.valuecomparison.model.Report;
import com.valuecomparison.repository.ReportRepository;
import com.valuecomparison.service.GeminiService;
import com.valuecomparison.service.ScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@RestController
@RequestMapping("/api/comparator")
@CrossOrigin(origins = "*")
public class ComparatorController {
    @Autowired
    private ScraperService scraperService;
    @Autowired
    private GeminiService geminiService;
    @Autowired
    private ReportRepository reportRepository;

    @GetMapping("/status")
    public String checkStatus() {
        return "AI-powered online server!";
    }
    @GetMapping("/report")
    public String generateReport(@RequestParam("q") String query) {
        System.out.println("1. Recebendo Pedido: " + query);
        List<ProductDTO> products = scraperService.searchProducts(query);
        if (products.isEmpty()) {
            return "Nenhum produto encontrado para gerar relatório.";
        }
        System.out.println("2. Enviando para o Gemini analisar...");
        String reportContent = geminiService.generatePurchaseReport(products, query);
        String usuarioLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        Report newReport = new Report(query, reportContent, usuarioLogado);
        reportRepository.save(newReport);
        System.out.println("3. Relatório salvo no banco por: " + usuarioLogado);
        return reportContent;
    }
    @GetMapping("/history")
    public List<Report> getHistory() {
        return reportRepository.findAll(Sort.by(Sort.Direction.DESC, "creationDate"));
    }
}