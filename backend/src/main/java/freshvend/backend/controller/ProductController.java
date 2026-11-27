package freshvend.backend.controller;

import freshvend.backend.entity.Product;
import freshvend.backend.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<Product> getAllProducts() {
        return service.getAllProducts();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product createProduct(@RequestBody Product product) {
        return service.createProduct(product);
    }

    @PatchMapping("/{productId}/price")
    public Product updatePrice(
            @PathVariable Long productId,
            @RequestBody PriceRequest request) {
        return service.updatePrice(productId, request.priceVnd());
    }

    public record PriceRequest(Long priceVnd) {
    }
}