package org.sxk.store.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.sxk.store.entity.Inventory;
import org.sxk.store.mapper.InventoryMapper;
import org.sxk.store.service.InventoryService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryMapper inventoryMapper;

    public InventoryServiceImpl(InventoryMapper inventoryMapper) {
        this.inventoryMapper = inventoryMapper;
    }

    @Override
    public Inventory getByProductId(Long productId) {
        log.info("Getting inventory for product: {}", productId);
        return inventoryMapper.findByProductId(productId);
    }

    @Override
    public int createInventory(Inventory inventory) {
        log.info("Creating inventory for product: {}", inventory.getProductId());
        return inventoryMapper.insert(inventory);
    }

    @Override
    public int updateInventory(Inventory inventory) {
        log.info("Updating inventory: {}", inventory.getId());
        return inventoryMapper.update(inventory);
    }

    @Override
    public boolean lockStock(Long productId, Integer quantity) {
        log.info("Locking stock for product {}: {}", productId, quantity);
        int result = inventoryMapper.lockStock(productId, quantity);
        boolean success = result > 0;
        if (success) {
            log.info("Stock locked successfully");
        } else {
            log.warn("Failed to lock stock, insufficient inventory");
        }
        return success;
    }

    @Override
    public boolean unlockStock(Long productId, Integer quantity) {
        log.info("Unlocking stock for product {}: {}", productId, quantity);
        int result = inventoryMapper.unlockStock(productId, quantity);
        boolean success = result > 0;
        if (success) {
            log.info("Stock unlocked successfully");
        } else {
            log.warn("Failed to unlock stock");
        }
        return success;
    }

    @Override
    public boolean deductStock(Long productId, Integer quantity) {
        log.info("Deducting stock for product {}: {}", productId, quantity);
        int result = inventoryMapper.deductStock(productId, quantity);
        boolean success = result > 0;
        if (success) {
            log.info("Stock deducted successfully");
        } else {
            log.warn("Failed to deduct stock");
        }
        return success;
    }
}