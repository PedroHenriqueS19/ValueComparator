package com.valuecomparison.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.valuecomparison.dto.ProductDTO;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ScraperService {

    private static final Logger logger = LoggerFactory.getLogger(ScraperService.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String API_KEY = "dc4244ef297c0e83b1f4b3eede9ff18df1fa66e3f61f15f6498ff53cd2df1fb4";

    public List<ProductDTO> searchProducts(String searchedProduct) {
        logger.info("--- CONSULTING SERPAPI AND ORDERING '{}' ---", searchedProduct);
        try {
            String url = buildApiUrl(searchedProduct);
            String jsonAnswer = fetchJsonFromApi(url);
            List<ProductDTO> products = extractProductsFromJson(jsonAnswer);
            sortProductsByPrice(products);
            logger.info("Consultation completed. {} products found and sorted.", products.size());
            return products;

        } catch (Exception e) {
            logger.error("Fatal error when searching for products on SerpAPI for the term '{}'", searchedProduct, e);
            throw new RuntimeException("Failure in communication with the price search API: " + e.getMessage(), e);
        }
    }

    private String buildApiUrl(String searchedProduct) {
        return "https://serpapi.com/search.json?engine=google_shopping&q="
                + searchedProduct.replace(" ", "+")
                + "&google_domain=google.com.br&gl=br&hl=pt&api_key=" + API_KEY;
    }

    private String fetchJsonFromApi(String url) throws Exception {
        return Jsoup.connect(url)
                .ignoreContentType(true)
                .timeout(30000)
                .execute()
                .body();
    }

    private List<ProductDTO> extractProductsFromJson(String jsonAnswer) throws Exception {
        List<ProductDTO> products = new ArrayList<>();
        JsonNode rootNode = mapper.readTree(jsonAnswer);
        JsonNode results = rootNode.path("shopping_results");
        if (results.isArray()) {
            for (JsonNode item : results) {
                if (products.size() >= 10) break;
                extractSingleProduct(item, products);
            }
        } else {
            logger.warn("The JSON returned by SerpAPI does not contain the array 'shopping_results'.");
        }
        return products;
    }

    private void extractSingleProduct(JsonNode item, List<ProductDTO> products) {
        try {
            String title = item.path("title").asText();
            String price = item.path("price").asText();
            String store = item.path("source").asText();
            String link = item.path("link").asText();
            if (store.isEmpty()) store = "Google Shopping";
            products.add(new ProductDTO(title, price, link, store));
        } catch (Exception e) {
            logger.warn("Failed to extract data for a specific product. Reason: {}", e.getMessage());
        }
    }

    private void sortProductsByPrice(List<ProductDTO> products) {
        products.sort(Comparator.comparingDouble(this::parsePriceToDouble));
    }

    private double parsePriceToDouble(ProductDTO p) {
        try {
            if (p.getOriginalPrice() == null || p.getOriginalPrice().isEmpty()) {
                logger.debug("Product '{}' It has a zero or empty price.", p.getName());
                return Double.MAX_VALUE;
            }
            String cleanPrice = p.getOriginalPrice()
                    .replaceAll("[^\\d,]", "")
                    .replace(",", ".");

            return Double.parseDouble(cleanPrice);
        } catch (NumberFormatException e) {
            logger.debug("Error converting price '{}' of the product '{}'. Moving to the end of the list.", p.getOriginalPrice(), p.getName());
            return Double.MAX_VALUE;
        } catch (Exception e) {
            logger.debug("Unexpected error in product sorting '{}': {}", p.getName(), e.getMessage());
            return Double.MAX_VALUE;
        }
    }
}