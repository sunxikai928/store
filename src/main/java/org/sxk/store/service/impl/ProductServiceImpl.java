package org.sxk.store.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.sxk.store.entity.Inventory;
import org.sxk.store.entity.Product;
import org.sxk.store.enums.ProductStatus;
import org.sxk.store.mapper.ProductMapper;
import org.sxk.store.service.InventoryService;
import org.sxk.store.service.ProductService;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final InventoryService inventoryService;

    public ProductServiceImpl(ProductMapper productMapper, InventoryService inventoryService) {
        this.productMapper = productMapper;
        this.inventoryService = inventoryService;
    }

    @Override
    public List<Product> getAllProducts() {
        log.info("Getting all products");
        return productMapper.findAll();
    }

    @Override
    public Product getProductById(Long id) {
        log.info("Getting product by id: {}", id);
        return productMapper.findById(id);
    }

    @Override
    public int createProduct(Product product) {
        log.info("Creating product: {}", product.getName());
        product.setStatusEnum(ProductStatus.OFF_SHELF);
        return productMapper.insert(product);
    }

    @Override
    public int updateProduct(Product product) {
        log.info("Updating product: {}", product.getId());
        return productMapper.update(product);
    }

    @Override
    public int deleteProduct(Long id) {
        log.info("Deleting product: {}", id);
        return productMapper.delete(id);
    }

    @Override
    public List<Product> searchByName(String name) {
        log.info("Searching products by name: {}", name);
        return productMapper.searchByName(name);
    }

    @Override
    public int shelfProduct(Long id) {
        log.info("Shelf product: {}", id);
        
        Product product = productMapper.findById(id);
        if (product == null) {
            log.error("Product not found: {}", id);
            return 0;
        }
        
        Inventory inventory = inventoryService.getByProductId(id);
        if (inventory == null || inventory.getStock() == null || inventory.getStock() <= 0) {
            log.error("Product {} cannot be shelved, insufficient stock", id);
            return -1;
        }
        
        product.setStatusEnum(ProductStatus.ON_SHELF);
        return productMapper.update(product);
    }

    @Override
    public int unshelfProduct(Long id) {
        log.info("Unshelf product: {}", id);
        Product product = productMapper.findById(id);
        if (product == null) {
            log.error("Product not found: {}", id);
            return 0;
        }
        product.setStatusEnum(ProductStatus.OFF_SHELF);
        return productMapper.update(product);
    }
}