package com.valuecomparison.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valuecomparison.dto.ProductDTO;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ScraperService {
    @SuppressWarnings("unused")
    @Value("${api.serp.key}")
    private String serpApiKey;

    public List<ProductDTO> searchProducts(String searchedProduct) {
        List<ProductDTO> products = new ArrayList<>();

        String encodedSearch = URLEncoder.encode(searchedProduct, StandardCharsets.UTF_8);
        String url = "https://serpapi.com/search.json?engine=google_shopping&q="
                + encodedSearch
                + "&google_domain=google.com.br&gl=br&hl=pt&api_key=" + serpApiKey;

        System.out.println("--- CONSULTANDO SERPAPI E ORDENANDO ---");

        try {
            String jsonAnswer = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .timeout(30000)
                    .execute()
                    .body();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonAnswer);

            if (rootNode.has("error")) {
                String erroApi = rootNode.path("error").asText();
                throw new RuntimeException("O Google bloqueou a busca: " + erroApi);
            }

            JsonNode results = rootNode.path("shopping_results");
            if (results.isMissingNode() || !results.isArray() || results.isEmpty()) {
                throw new RuntimeException("O Google Shopping não encontrou nenhum anúncio para: '" + searchedProduct + "'. Tente ser mais genérico.");
            }

            for (JsonNode item : results) {
                if (products.size() >= 10) break;
                try {
                    String title = item.path("title").asText();
                    String price = item.path("price").asText();
                    String store = item.path("source").asText();
                    String link = item.path("link").asText();
                    if (store.isEmpty()) store = "Google Shopping";
                    products.add(new ProductDTO(title, price, link, store));
                } catch (Exception e) {
                    System.err.println("Item ignorado devido a formatação inesperada: " + e.getMessage());
                }
            }

            sortProductsByPrice(products);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Falha de conexão ao buscar os preços: " + e.getMessage());
        }

        return products;
    }

    private void sortProductsByPrice(List<ProductDTO> products) {
        products.sort(Comparator.comparingDouble(p -> {
            try {
                if (p.getOriginalPrice() == null || p.getOriginalPrice().isEmpty()) {
                    return Double.MAX_VALUE;
                }
                String cleanPrice = p.getOriginalPrice()
                        .replaceAll("[^\\d,]", "")
                        .replace(",", ".");
                return Double.parseDouble(cleanPrice);
            } catch (Exception e) {
                return Double.MAX_VALUE;
            }
        }));
    }
}