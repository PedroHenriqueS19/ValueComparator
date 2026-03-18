package com.valuecomparison.dto;

// O 'record' é um recurso moderno do Java que cria os getters e setters automaticamente

public record AuthenticationDTO(String login, String senha) {
}