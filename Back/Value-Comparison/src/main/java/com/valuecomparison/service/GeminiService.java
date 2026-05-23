package com.valuecomparison.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valuecomparison.dto.ProductDTO;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

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
    @Value("${api.gemini.key}")
    private String geminiApiKey;
    private static final String MODEL_NAME = "gemini-2.5-flash";
    public String generatePurchaseReport(List<ProductDTO> products, String searchedName, String dateAndTime) {
        try {
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

            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_NAME + ":generateContent?key=" + geminiApiKey;

            // 5. Envio via HttpClient
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            System.out.println("Gerando Relatório Legal (Lei 14.133)...");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("Erro API Google: " + response.body());
                throw new RuntimeException("A Inteligência Artificial está temporariamente indisponível devido à alta demanda do Google (Erro " + response.statusCode() + "). Por favor, tente novamente em alguns instantes.");
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
                Atue como um Agente de Pesquisa de Preços especialista em Pesquisa de Preços sob a égide da Lei nº 14.133/2021.
                Sua tarefa é elaborar um RELATÓRIO TÉCNICO DE PESQUISA DE PREÇOS, atuando como um assistente de auditoria.
                
                REGRA DE SANIDADE: Se o termo "{{TERMO}}" for sem sentido (ex: 'asdasd'), retorne apenas: INVALIDEZ_DETECTADA. Reconheça " como polegadas.
                
                SISTEMA DE FILTRAGEM (REGRAS ESTABELECIDAS):
                
                1. VEDAÇÃO A PREÇOS PROMOCIONAIS (ESTRITO): Analise se os títulos indicam "Promoção", "Oferta", "Desconto" ou "Queima de Estoque".
                   - REGRA: DESCARTAR AUTOMATICAMENTE.
                   
                2. FILTRO DE UNIDADE DE MEDIDA (ESTRITO): Verifique se a unidade (Kg, L, ml, g, polegadas, etc.) da loja coincide com o pedido ("{{TERMO}}").
                   - REGRA: Se a unidade for diferente ou ausente, DESCARTAR AUTOMATICAMENTE para evitar erro de objeto.
                   
                3. VEDAÇÃO A MARKETPLACES (ESTRITO): 
                   - REGRA: Itens de marketplaces (Mercado Livre, Amazon, Magalu, Shopee, etc.) devem ser DESCARTADOS AUTOMATICAMENTE.
                   
                4. ANÁLISE DE PREÇO INEXEQUÍVEL (ALERTA): Se o preço for absurdamente mais baixo que a média, MAS não possuir indicação clara de promoção.
                   - REGRA: NÃO descarte. SINALIZAR como alerta na tabela para diligência do Agente da pesquisa.
                   
                5. Nunca declare o responsável da pesquisa como "Agente da Contratação" e sim como "Agente da Pesquisa de Preços"
                
                DADOS PARA ANÁLISE:
                Objeto: "{{TERMO}}" | Data: {{DATA}}
                Série de Preços:
                {{DADOS}}
                
                ESTRUTURA DO RELATÓRIO:
                # RELATÓRIO TÉCNICO DE ESTIMATIVA DE PREÇOS

                **1. Objeto:** [Nome do objeto]
                
                **2. Parâmetro:** Inciso III do § 1º do art. 23 da Lei nº 14.133/2021 - "III - utilização de dados de pesquisa publicada em mídia especializada, de tabela de referência formalmente aprovada pelo Poder Executivo federal e de sítios eletrônicos especializados ou de domínio amplo, desde que contenham a data e hora de acesso;".
                
                **3. Tabela Comparativa de Preços**
                
                | Item | Fornecedor | Preço | Status de Validação (Observação) |
                | :--- | :--- | :--- | :--- |
                
                REGRAS DE STATUS (Use HTML <span> para cores):
                - Válido: <span style="color: green; font-weight: bold;">✅ Fonte Válida</span>
                - Alerta Inexequível: <span style="color: orange; font-weight: bold;">⚠️ Atenção: Possível Preço Inexequível (Decisão do Agente)</span>
                - Descartado (Promoção): <span style="color: red;">❌ Descartado (Preço Promocional)</span>
                - Descartado (Medida): <span style="color: red;">❌ Descartado (Unidade/Medida Incompatível)</span>
                - Descartado (Marketplace): <span style="color: red;">❌ Descartado (Marketplace)</span>
                
                **4. Metodologia de Cálculo**
                
                - Explique que o cálculo baseia-se nos itens não descartados.
                - REGRA OBRIGATÓRIA: Se houver itens com "Alerta Inexequível", explique que serão apresentados dois cenários de cálculo para subsidiar a tomada de decisão do Agente de Contratação.
                - Desconsidere todos os valores médios e me traga somente a metodologia e os valores unitários, sem realizar o cálculo da média aritimética.
                
                **5. Conclusão e Valor de Referência**
                - Desconsidere todos os valores médios e me traga somente a metodologia e os valores unitários, sem realizar o cálculo da média aritimética.
                - Informe que os valores auferidos serão direcionados para uma tabela apartada, onde será realizado de fato a comparação em um quadro comparativo.
                """;

        return promptTemplate
                .replace("{{TERMO}}", searchedName)
                .replace("{{DATA}}", dateAndTime)
                .replace("{{DADOS}}", dataProducts.toString());
    }
}