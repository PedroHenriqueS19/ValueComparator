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
            String prompt = getString(searchedName, dateAndTime, dataProducts);
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
            String respostaIA = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
            System.out.println("--- RESPOSTA CRUA DA IA --- \n" + respostaIA);
            if (respostaIA.contains("INVALIDEZ_DETECTADA")) {
                throw new RuntimeException("O termo pesquisado é inválido ou não possui correlação com produtos reais de mercado.");
            }
            return respostaIA;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    private static String getString(String searchedName, String dateAndTime, StringBuilder dataProducts) {
        String promptTemplate = """
                Atue como um Agente de Contratação Pública especialista em Pesquisa de Preços.
                Sua tarefa é elaborar um RELATÓRIO TÉCNICO DE PESQUISA DE PREÇOS para instrução de processo licitatório.
                
                REGRA DE SANIDADE: Analise o nome do objeto pesquisado ("{{TERMO}}"). Se ele for um texto claramente sem sentido (ex: 'asdasd', 'fjiosdopfihjsdpsfj', '12341234'), retorne EXATAMENTE E APENAS a palavra: INVALIDEZ_DETECTADA e ignore o resto.
                
                BASE LEGAL:
                1. Lei nº 14.133/2021, Art. 23, § 1º, inciso III (Pesquisa em sítios eletrônicos).
                2. Decreto Estadual/SP nº 67.888/2023 (Definição do valor estimado).
                3. VEDAÇÃO A MARKETPLACES: Em hipótese alguma utilize preços de marketplaces (ex: Mercado Livre, Shopee, Amazon, Magalu, Kabum) para o cálculo final. 
                
                OBJETO DA PESQUISA: "{{TERMO}}"
                DATA DA CONSULTA: {{DATA}}
                DADOS COLETADOS (Série de Preços):
                {{DADOS}}
                
                DIRETRIZES DE EXECUÇÃO:
                1. Análise Crítica: Se TODOS os dados coletados vierem de Marketplaces, gere o relatório normalmente, mas conclua que não foi possível formar preço devido à restrição das fontes. NÃO use a palavra INVALIDEZ_DETECTADA neste caso.
                2. DIVERSIDADE DE FONTES: NUNCA repita o mesmo fornecedor/loja na Tabela Comparativa. Se houver vários produtos da mesma loja na lista, escolha apenas o que tiver o menor preço e descarte os demais.
                3. Metodologia: Utilize preferencialmente a MEDIANA ou o MENOR PREÇO (apenas com fontes válidas e distintas).
                4. Formatação: Gere o relatório estritamente em Markdown misturado com HTML para cores, conforme estrutura abaixo.
                
                ESTRUTURA OBRIGATÓRIA DO RELATÓRIO:
                # RELATÓRIO TÉCNICO DE ESTIMATIVA DE PREÇOS
                **1. Objeto:** [Repetir o nome do objeto]
                **2. Parâmetro de Pesquisa:** Inciso III do § 1º do art. 23 da Lei nº 14.133/2021.
                **3. Tabela Comparativa de Preços**
                | Descrição do Item | Fornecedor (Fonte) | Preço Unitário | Observação |
                | :--- | :--- | :--- | :--- |
                *(Lembre-se: Fornecedores únicos na tabela. Siga RIGOROSAMENTE as regras de cores HTML abaixo para a coluna Observação)*
                
                REGRAS DE CORES NA COLUNA OBSERVAÇÃO (Use HTML <span>):
                - Se a fonte for VÁLIDA: O texto deve ser: <span style="color: green; font-weight: bold;">✅ Fonte válida</span>
                - Se a fonte for DESCARGADA (Marketplace/Repetida): O texto deve ser: <span style="color: red;">❌ Descartado (Marketplace)</span> ou <span style="color: red;">❌ Descartado (Fonte Duplicada)</span>
                
                **4. Metodologia de Cálculo Aplicada**
                - Média / Mediana / Menor Preço (Calcule apenas com os válidos)
                **5. Conclusão e Valor de Referência**
                [Indique qual valor deve ser adotado. Se só houver marketplaces, justifique juridicamente a impossibilidade de estimar o valor.]
                """;
        String prompt = promptTemplate
                .replace("{{TERMO}}", searchedName)
                .replace("{{DATA}}", dateAndTime)
                .replace("{{DADOS}}", dataProducts.toString());
        return prompt;
    }
}