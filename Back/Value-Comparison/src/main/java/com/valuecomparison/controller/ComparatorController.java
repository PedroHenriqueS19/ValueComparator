package com.valuecomparison.controller;

import com.valuecomparison.dto.ProductDTO;
import com.valuecomparison.model.Report;
import com.valuecomparison.repository.ReportRepository;
import com.valuecomparison.service.GeminiService;
import com.valuecomparison.service.ScraperService;
import com.valuecomparison.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/comparator")
public class ComparatorController {

    @Autowired
    private ScraperService scraperService;
    @Autowired
    private GeminiService geminiService;
    @Autowired
    private PdfService pdfService;
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
        String dataHoraOficial = LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        System.out.println("2. Enviando para o Gemini analisar...");
        String reportContent = geminiService.generatePurchaseReport(products, query, dataHoraOficial);
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
    @PostMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestBody String markdown) {
        try {
            String usuarioLogado = SecurityContextHolder.getContext().getAuthentication().getName();
            String dataHoraImpressao = LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            // Converte o Markdown em PDF (Array de bytes)
            byte[] relatorioPdf = pdfService.generatePdfFromMarkdown(markdown, usuarioLogado, dataHoraImpressao);
            // Configura os headers para o navegador entender que é um arquivo para download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            // Define o nome do arquivo que será baixado pelo usuário
            headers.setContentDispositionFormData("attachment", "Relatorio_Estimativa_Precos.pdf");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(relatorioPdf);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}