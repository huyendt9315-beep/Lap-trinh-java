package freshvend.backend.service;

import freshvend.backend.entity.Product;
import freshvend.backend.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<Product> getAllProducts() {
        return repository.findAll();
    }

    public List<Product> getActiveProducts() {
        return repository.findByActiveTrue();
    }

    public Product getProductById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found"));
    }

    @Transactional
    public Product createProduct(Product product) {
        if (product.getSku() == null || product.getSku().isBlank()
                || product.getName() == null || product.getName().isBlank()
                || product.getImageUrl() == null
                || product.getPriceVnd() == null
                || product.getPriceVnd() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid product data");
        }

        if (repository.existsBySku(product.getSku())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "SKU already exists");
        }

        product.setId(null);
        return repository.save(product);
    }

    @Transactional
    public Product updatePrice(Long id, Long priceVnd) {
        if (priceVnd == null || priceVnd <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid price");
        }

        Product product = getProductById(id);
        product.setPriceVnd(priceVnd);
        return repository.save(product);
    }
}