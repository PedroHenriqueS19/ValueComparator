package com.valuecomparison.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Captura os erros de negócio que nós mapeamos (Ex: Os throw new RuntimeException do Scraper)
     * Retorna o Status 400 (Bad Request)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> erro = new HashMap<>();
        erro.put("message", ex.getMessage()); // Manda a mensagem exata que gerou o erro

        // Imprime no terminal do servidor para o desenvolvedor ver
        System.err.println("⚠️ Alerta de Regra de Negócio: " + ex.getMessage());

        return new ResponseEntity<>(erro, HttpStatus.BAD_REQUEST);
    }

    /**
     * Captura qualquer erro "Grave" e inesperado que não prevemos (Ex: Banco de Dados caiu, NullPointer)
     * Retorna o Status 500 (Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        Map<String, String> erro = new HashMap<>();
        erro.put("message", "Erro crítico no servidor: " + ex.getMessage());

        // Imprime a árvore completa do erro no terminal para podermos debugar depois
        System.err.println("❌ ERRO CRÍTICO INESPERADO:");
        ex.printStackTrace();

        return new ResponseEntity<>(erro, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}