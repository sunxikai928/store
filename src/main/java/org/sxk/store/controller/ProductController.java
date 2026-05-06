package org.sxk.store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.sxk.store.common.Result;
import org.sxk.store.entity.Product;
import org.sxk.store.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products")
@Tag(name = "商品管理", description = "商品的增删改查及搜索操作")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "获取所有商品", description = "返回系统中所有商品列表，按更新时间倒序排列")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public ResponseEntity<Result<List<Product>>> getAllProducts() {
        log.info("GET /api/products - 获取所有商品");
        return ResponseEntity.ok(Result.success(productService.getAllProducts()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取商品", description = "通过商品ID查询单个商品详情")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "商品不存在")
    })
    public ResponseEntity<Result<Product>> getProductById(
            @Parameter(description = "商品ID", required = true)
            @PathVariable Long id) {
        log.info("GET /api/products/{} - 根据ID获取商品", id);
        Product product = productService.getProductById(id);
        if (product != null) {
            return ResponseEntity.ok(Result.success(product));
        }
        return ResponseEntity.ok(Result.notFound("商品不存在"));
    }

    @PostMapping
    @Operation(summary = "创建商品", description = "创建新商品，商品名称和价格为必填项")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "500", description = "创建失败")
    })
    public ResponseEntity<Result<String>> createProduct(
            @Parameter(description = "商品对象", required = true)
            @RequestBody Product product) {
        log.info("POST /api/products - 创建商品: {}", product.getName());
        int result = productService.createProduct(product);
        if (result > 0) {
            return ResponseEntity.ok(Result.success("商品创建成功"));
        }
        return ResponseEntity.ok(Result.error("商品创建失败"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新商品", description = "根据ID更新商品信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "500", description = "更新失败")
    })
    public ResponseEntity<Result<String>> updateProduct(
            @Parameter(description = "商品ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "商品对象", required = true)
            @RequestBody Product product) {
        log.info("PUT /api/products/{} - 更新商品", id);
        product.setId(id);
        int result = productService.updateProduct(product);
        if (result > 0) {
            return ResponseEntity.ok(Result.success("商品更新成功"));
        }
        return ResponseEntity.ok(Result.error("商品更新失败"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除商品", description = "根据ID删除商品")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "500", description = "删除失败")
    })
    public ResponseEntity<Result<String>> deleteProduct(
            @Parameter(description = "商品ID", required = true)
            @PathVariable Long id) {
        log.info("DELETE /api/products/{} - 删除商品", id);
        int result = productService.deleteProduct(id);
        if (result > 0) {
            return ResponseEntity.ok(Result.success("商品删除成功"));
        }
        return ResponseEntity.ok(Result.error("商品删除失败"));
    }

    @GetMapping("/search")
    @Operation(summary = "搜索商品", description = "根据商品名称模糊搜索商品，按更新时间倒序排列")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public ResponseEntity<Result<List<Product>>> searchProducts(
            @Parameter(description = "商品名称关键词", required = true)
            @RequestParam String name) {
        log.info("GET /api/products/search?name={} - 搜索商品", name);
        List<Product> products = productService.searchByName(name);
        return ResponseEntity.ok(Result.success(products));
    }

    @PutMapping("/{id}/shelf")
    @Operation(summary = "上架商品", description = "将商品状态改为上架，使其可被搜索和购买。库存为0时不允许上架")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "上架成功"),
            @ApiResponse(responseCode = "400", description = "库存不足，无法上架"),
            @ApiResponse(responseCode = "404", description = "商品不存在")
    })
    public ResponseEntity<Result<String>> shelfProduct(
            @Parameter(description = "商品ID", required = true)
            @PathVariable Long id) {
        log.info("PUT /api/products/{}/shelf - 上架商品", id);
        int result = productService.shelfProduct(id);
        if (result > 0) {
            return ResponseEntity.ok(Result.success("商品上架成功"));
        } else if (result == -1) {
            return ResponseEntity.ok(Result.badRequest("库存不足，无法上架"));
        }
        return ResponseEntity.ok(Result.notFound("商品不存在"));
    }

    @PutMapping("/{id}/unshelf")
    @Operation(summary = "下架商品", description = "将商品状态改为下架，使其不可被搜索和购买")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "下架成功"),
            @ApiResponse(responseCode = "404", description = "商品不存在")
    })
    public ResponseEntity<Result<String>> unshelfProduct(
            @Parameter(description = "商品ID", required = true)
            @PathVariable Long id) {
        log.info("PUT /api/products/{}/unshelf - 下架商品", id);
        int result = productService.unshelfProduct(id);
        if (result > 0) {
            return ResponseEntity.ok(Result.success("商品下架成功"));
        }
        return ResponseEntity.ok(Result.notFound("商品不存在"));
    }
}