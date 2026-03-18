package com.valuecomparison.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProductDTO {
    private String name;
    private String originalPrice;
    private String link;
    private String store;

    public ProductDTO(String name, String originalPrice, String link, String store) {
        this.name = name;
        this.originalPrice = originalPrice;
        this.link = link;
        this.store = store;
    }

    public String getName() {
        return name;
    }
    public String getOriginalPrice() {
        return originalPrice;
    }
    public String getLink() {
        return link;
    }
    public String getStore() {
        return store;
    }
}