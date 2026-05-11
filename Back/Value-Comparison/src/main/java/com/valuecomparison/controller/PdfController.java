package com.valuecomparison.controller;

import com.valuecomparison.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    @Autowired
    private PdfService pdfService;

    @PostMapping("/export")
    public ResponseEntity<byte[]> exportPdf(@RequestBody Map<String, String> payload, Principal principal) throws Exception {

        String markdown = payload.get("markdown");
        String termo = payload.get("termo");

        String nomeUsuario = (principal != null && principal.getName() != null) ? principal.getName() : "Auditor do Sistema";
        String dataEmissao = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());

        byte[] arquivoPdf = pdfService.generatePdfFromMarkdown(markdown, nomeUsuario, dataEmissao);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String nomeArquivo = "Relatorio_" + termo.replaceAll("\\s+", "_") + ".pdf";
        headers.setContentDispositionFormData("attachment", nomeArquivo);

        return new ResponseEntity<>(arquivoPdf, headers, HttpStatus.OK);
    }
}