package com.valuecomparison.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valuecomparison.dto.ProductDTO;
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
    private static final String API_KEY = "AIzaSyBTzKWtpctG6OadFRV4798iaLnyPyxaDXg";
    private static final String MODEL_NAME = "gemini-2.5-flash";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_NAME + ":generateContent?key=" + API_KEY;
    public String generatePurchaseReport(List<ProductDTO> products, String searchedName) {
        try {
            // 1. Date and Time
            String dateAndTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

            // 2. Prepare data
            StringBuilder dataProducts = new StringBuilder();
            for (ProductDTO p : products) {
                dataProducts.append("- Item: ").append(p.getName())
                        .append(" | Valor: ").append(p.getOriginalPrice())
                        .append(" | Fonte: ").append(p.getStore())
                        .append(" | Link: ").append(p.getLink()).append("\n");
            }
            // 3. O PROMPT JURÍDICO (Usando placeholders seguros {{VARIAVEL}})
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
            String prompt = promptTemplate
                    .replace("{{TERMO}}", searchedName)
                    .replace("{{DATA}}", dateAndTime)
                    .replace("{{DADOS}}", dataProducts.toString());
            // 4. Monta o JSON
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);
            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(part));
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(content));
            String jsonBody = mapper.writeValueAsString(requestBody);
            // 5. Envio via HttpClient
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            System.out.println("Gerando Relatório Legal (Lei 14.133)...");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("Erro API Google: " + response.body());
                return "Erro na IA (" + response.statusCode() + "): " + response.body();
            }
            JsonNode root = mapper.readTree(response.body());
            JsonNode candidates = root.path("candidates");
            if (candidates.isEmpty()) {
                return "A IA não gerou resposta. Motivo: " + root.path("promptFeedback").toString();
            }
            return candidates.get(0).path("content").path("parts").get(0).path("text").asText();
        } catch (Exception e) {
            e.printStackTrace();
            return "ERRO TÉCNICO: " + e.getMessage();
        }
    }
}