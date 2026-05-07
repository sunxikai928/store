package org.sxk.store.service;

import java.util.List;

public interface RedisStockService {
    void setStock(Long productId, Integer stock);
    Integer getStock(Long productId);
    boolean decreaseStock(Long productId, Integer quantity);
    boolean decreaseStockBatch(List<Long> productIds, List<Integer> quantities);
    void deleteStock(Long productId);
    boolean hasStock(Long productId);
}