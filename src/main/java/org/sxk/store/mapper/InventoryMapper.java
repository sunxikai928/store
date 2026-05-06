package org.sxk.store.mapper;

import org.sxk.store.entity.Inventory;

public interface InventoryMapper {
    Inventory findByProductId(Long productId);
    int insert(Inventory inventory);
    int update(Inventory inventory);
    int lockStock(Long productId, Integer quantity);
    int unlockStock(Long productId, Integer quantity);
    int deductStock(Long productId, Integer quantity);
}