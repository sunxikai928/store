package org.sxk.store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.sxk.store.common.Result;
import org.sxk.store.entity.Inventory;
import org.sxk.store.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
@Tag(name = "库存管理", description = "库存查询、锁定、解锁和扣减操作")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "查询商品库存", description = "根据商品ID查询库存信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "库存不存在")
    })
    public ResponseEntity<Result<Inventory>> getByProductId(
            @Parameter(description = "商品ID", required = true)
            @PathVariable Long productId) {
        log.info("GET /api/inventory/product/{} - 查询商品库存", productId);
        Inventory inventory = inventoryService.getByProductId(productId);
        if (inventory != null) {
            return ResponseEntity.ok(Result.success(inventory));
        }
        return ResponseEntity.ok(Result.notFound("库存不存在"));
    }

    @PostMapping
    @Operation(summary = "创建库存", description = "为商品创建库存记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "500", description = "创建失败")
    })
    public ResponseEntity<Result<String>> createInventory(
            @Parameter(description = "库存对象", required = true)
            @RequestBody Inventory inventory) {
        log.info("POST /api/inventory - 创建库存: productId={}", inventory.getProductId());
        int result = inventoryService.createInventory(inventory);
        if (result > 0) {
            return ResponseEntity.ok(Result.success("库存创建成功"));
        }
        return ResponseEntity.ok(Result.error("库存创建失败"));
    }

    @PutMapping
    @Operation(summary = "更新库存", description = "更新库存数量")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "500", description = "更新失败")
    })
    public ResponseEntity<Result<String>> updateInventory(
            @Parameter(description = "库存对象", required = true)
            @RequestBody Inventory inventory) {
        log.info("PUT /api/inventory - 更新库存: id={}", inventory.getId());
        int result = inventoryService.updateInventory(inventory);
        if (result > 0) {
            return ResponseEntity.ok(Result.success("库存更新成功"));
        }
        return ResponseEntity.ok(Result.error("库存更新失败"));
    }

    @PostMapping("/lock")
    @Operation(summary = "锁定库存", description = "下单时锁定商品库存，确保库存不被其他订单占用")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "锁定成功"),
            @ApiResponse(responseCode = "400", description = "库存不足，锁定失败")
    })
    public ResponseEntity<Result<String>> lockStock(
            @Parameter(description = "商品ID", required = true)
            @RequestParam Long productId,
            @Parameter(description = "锁定数量", required = true)
            @RequestParam Integer quantity) {
        log.info("POST /api/inventory/lock - 锁定库存: productId={}, quantity={}", productId, quantity);
        boolean success = inventoryService.lockStock(productId, quantity);
        if (success) {
            return ResponseEntity.ok(Result.success("库存锁定成功"));
        }
        return ResponseEntity.ok(Result.badRequest("库存不足，锁定失败"));
    }

    @PostMapping("/unlock")
    @Operation(summary = "解锁库存", description = "取消订单时释放锁定的库存")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "解锁成功"),
            @ApiResponse(responseCode = "400", description = "解锁失败")
    })
    public ResponseEntity<Result<String>> unlockStock(
            @Parameter(description = "商品ID", required = true)
            @RequestParam Long productId,
            @Parameter(description = "解锁数量", required = true)
            @RequestParam Integer quantity) {
        log.info("POST /api/inventory/unlock - 解锁库存: productId={}, quantity={}", productId, quantity);
        boolean success = inventoryService.unlockStock(productId, quantity);
        if (success) {
            return ResponseEntity.ok(Result.success("库存解锁成功"));
        }
        return ResponseEntity.ok(Result.badRequest("库存解锁失败"));
    }

    @PostMapping("/deduct")
    @Operation(summary = "扣减库存", description = "支付成功后扣减实际库存")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "扣减成功"),
            @ApiResponse(responseCode = "400", description = "扣减失败")
    })
    public ResponseEntity<Result<String>> deductStock(
            @Parameter(description = "商品ID", required = true)
            @RequestParam Long productId,
            @Parameter(description = "扣减数量", required = true)
            @RequestParam Integer quantity) {
        log.info("POST /api/inventory/deduct - 扣减库存: productId={}, quantity={}", productId, quantity);
        boolean success = inventoryService.deductStock(productId, quantity);
        if (success) {
            return ResponseEntity.ok(Result.success("库存扣减成功"));
        }
        return ResponseEntity.ok(Result.badRequest("库存扣减失败"));
    }
}