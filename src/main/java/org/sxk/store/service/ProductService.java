package org.sxk.store.service;

import org.sxk.store.entity.Product;

import java.util.List;

public interface ProductService {
    List<Product> getAllProducts();
    Product getProductById(Long id);
    int createProduct(Product product);
    int updateProduct(Product product);
    int deleteProduct(Long id);
    List<Product> searchByName(String name);
    int shelfProduct(Long id);
    int unshelfProduct(Long id);
}