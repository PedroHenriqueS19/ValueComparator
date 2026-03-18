package com.valuecomparison.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valuecomparison.dto.ProductDTO;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ScraperService {
    public List<ProductDTO> searchProducts(String searchedProduct) {
        List<ProductDTO> products = new ArrayList<>();
        String apiKey = "dc4244ef297c0e83b1f4b3eede9ff18df1fa66e3f61f15f6498ff53cd2df1fb4";
        String url = "https://serpapi.com/search.json?engine=google_shopping&q="
                + searchedProduct.replace(" ", "+")
                + "&google_domain=google.com.br&gl=br&hl=pt&api_key=" + apiKey;
        System.out.println("--- CONSULTING SERPAPI AND ORDERING ---");
        try {
            String jsonAnswer = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .timeout(30000)
                    .execute()
                    .body();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonAnswer);
            JsonNode results = rootNode.path("shopping_results");
            if (results.isArray()) {
                for (JsonNode item : results) {
                    if (products.size() >= 10) break;
                    try {
                        String title = item.path("title").asText();
                        String price = item.path("price").asText();
                        String store = item.path("source").asText();
                        String link = item.path("link").asText();
                        if (store.isEmpty()) store = "Google Shopping";
                        products.add(new ProductDTO(title, price, link, store));
                    } catch (Exception e) {// ignore item with error
                    }
                }
            }
            // SECURE ORDERING
            products.sort(Comparator.comparingDouble(p -> {
                try {
                    if (p.getOriginalPrice() == null || p.getOriginalPrice().isEmpty()) {
                        return Double.MAX_VALUE;
                    }
                    String cleanPrice = p.getOriginalPrice()
                            .replaceAll("[^\\d,]", "")// Remove letters and symbols (except numbers and commas).
                            .replace(",", ".");// Replace comma with period.
                    return Double.parseDouble(cleanPrice);
                } catch (Exception e) {
                    return Double.MAX_VALUE; // If the conversion fails, play it to the end.
                }
            }));
        } catch (Exception e) {
            System.err.println("Erro: " + e.getMessage());
        }
        return products;
    }
}