package com.valuecomparison.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valuecomparison.dto.ProductDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {
    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    private static final String API_KEY = "AIzaSyBTzKWtpctG6OadFRV4798iaLnyPyxaDXg";
    private static final String MODEL_NAME = "gemini-2.5-flash";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_NAME + ":generateContent?key=" + API_KEY;

    private final ObjectMapper mapper = new ObjectMapper();

    public String generatePurchaseReport(List<ProductDTO> products, String searchedName) {
        logger.info("--- INICIANDO GERAÇÃO DE RELATÓRIO COM IA PARA: '{}' ---", searchedName);
        try {
            String formattedProducts = formatProductList(products);
            String prompt = createLegalPrompt(searchedName, formattedProducts);
            String jsonBody = createJsonRequestBody(prompt);
            String apiResponse = sendGoogleApiRequest(jsonBody);
            String report = extractTextFromResponse(apiResponse);
            logger.info("Relatório gerado com sucesso para o termo '{}'.", searchedName);
            return report;

        } catch (Exception e) {
            logger.error("Erro fatal ao processar a requisição com a Inteligência Artificial para '{}'", searchedName, e);
            throw new RuntimeException("Falha na geração do relatório via IA: " + e.getMessage(), e);
        }
    }
    private String formatProductList(List<ProductDTO> products) {
        StringBuilder dataProducts = new StringBuilder();
        for (ProductDTO p : products) {
            dataProducts.append("- Item: ").append(p.getName())
                    .append(" | Valor: ").append(p.getOriginalPrice())
                    .append(" | Fonte: ").append(p.getStore())
                    .append(" | Link: ").append(p.getLink()).append("\n");
        }
        return dataProducts.toString();
    }

    private String createLegalPrompt(String searchedName, String formattedProducts) {
        String dateAndTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        String promptTemplate = """
                Atue como um Agente de Contratação Pública especialista em Pesquisa de Preços.
                Sua tarefa é elaborar um RELATÓRIO TÉCNICO DE PESQUISA DE PREÇOS para instrução de processo licitatório.
                BASE LEGAL:
                1. Lei nº 14.133/2021, Art. 23, § 1º, inciso III (Pesquisa em sítios eletrônicos).
                2. Decreto Estadual/SP nº 67.888/2023 (Definição do valor estimado).
                OBJETO DA PESQUISA: "{{TERMO}}"
                DATA DA CONSULTA: {{DATA}}
                DADOS COLETADOS (Série de Preços):
                {{DADOS}}
                DIRETRIZES DE EXECUÇÃO:
                1. Análise Crítica: Descarte itens que não correspondam exatamente à especificação técnica ou que apresentem preços manifestamente inexequíveis ou excessivos (outliers).
                2. Metodologia: Utilize preferencialmente a MEDIANA ou o MENOR PREÇO (justifique a escolha visando a economicidade).
                3. Formatação: Gere o relatório estritamente em Markdown.
                ESTRUTURA OBRIGATÓRIA DO RELATÓRIO:
                # RELATÓRIO TÉCNICO DE ESTIMATIVA DE PREÇOS
                **1. Objeto:** [Repetir o nome do objeto]
                **2. Parâmetro de Pesquisa:** Inciso III do § 1º do art. 23 da Lei nº 14.133/2021 (Sítios Eletrônicos).
                **3. Tabela Comparativa de Preços (Cesta de Preços Válidos)**
                | Descrição do Item | Fornecedor (Fonte) | Preço Unitário |
                | :--- | :--- | :--- |
                | ... | ... | ... |
                **4. Metodologia de Cálculo Aplicada**
                - Média Aritmética: R$ ...
                - Mediana: R$ ...
                - Menor Preço: R$ ...
                **5. Conclusão e Valor de Referência**
                [Indique qual valor deve ser adotado como Estimado para a licitação. Justifique com base no Art. 6º do Decreto 67.888/2023.]
                """;
        return promptTemplate
                .replace("{{TERMO}}", searchedName)
                .replace("{{DATA}}", dateAndTime)
                .replace("{{DADOS}}", formattedProducts);
    }

    private String createJsonRequestBody(String prompt) throws Exception {
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));
        return mapper.writeValueAsString(requestBody);
    }
    private String sendGoogleApiRequest(String jsonBody) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        logger.info("Enviando requisição para a API do Google Gemini...");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            // Se o Google recusar a chave ou a requisição, logamos o corpo do erro para debug
            logger.error("Falha na API do Google. Status HTTP: {}. Detalhes: {}", response.statusCode(), response.body());
            throw new RuntimeException("A API do Google retornou um erro (" + response.statusCode() + ").");
        }
        return response.body();
    }
    private String extractTextFromResponse(String responseBody) throws Exception {
        JsonNode root = mapper.readTree(responseBody);
        JsonNode candidates = root.path("candidates");
        if (candidates.isEmpty()) {
            // Isso acontece se a IA bloquear a resposta (ex: detecção de conteúdo ofensivo/inseguro)
            String feedback = root.path("promptFeedback").toString();
            logger.warn("A IA não gerou resposta. Isso geralmente indica um bloqueio de segurança. Motivo retornado: {}", feedback);
            throw new RuntimeException("A resposta foi bloqueada ou retornou vazia pelos filtros da IA. Motivo: " + feedback);
        }
        return candidates.get(0).path("content").path("parts").get(0).path("text").asText();
    }
}