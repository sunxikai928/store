package org.sxk.store.mapper;

import org.sxk.store.entity.Product;

import java.util.List;

public interface ProductMapper {
    List<Product> findAll();
    Product findById(Long id);
    int insert(Product product);
    int update(Product product);
    int delete(Long id);
    List<Product> searchByName(String name);
}