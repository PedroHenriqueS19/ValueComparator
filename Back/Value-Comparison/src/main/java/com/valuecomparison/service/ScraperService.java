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
                throw new RuntimeException("O Google Shopping não encontrou nenhum anúncio para: '" + searchedProduct + "'. Tente ser mais genérico na pesquisa.");
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
                }
            }
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
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Falha de conexão ao buscar os preços: " + e.getMessage());
        }
        return products;
    }
}