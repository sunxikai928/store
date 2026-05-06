package org.sxk.store.service;

import org.sxk.store.entity.Inventory;

public interface InventoryService {
    Inventory getByProductId(Long productId);
    int createInventory(Inventory inventory);
    int updateInventory(Inventory inventory);
    boolean lockStock(Long productId, Integer quantity);
    boolean unlockStock(Long productId, Integer quantity);
    boolean deductStock(Long productId, Integer quantity);
}