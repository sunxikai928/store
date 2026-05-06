package org.sxk.store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.sxk.store.common.Result;
import org.sxk.store.dto.PlaceOrderRequest;
import org.sxk.store.entity.OrderItem;
import org.sxk.store.entity.Orders;
import org.sxk.store.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@Tag(name = "订单管理", description = "订单的创建、查询、支付和取消操作")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "获取所有订单", description = "返回系统中所有订单列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public ResponseEntity<Result<List<Orders>>> getAllOrders() {
        log.info("GET /api/orders - 获取所有订单");
        return ResponseEntity.ok(Result.success(orderService.getAllOrders()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取订单", description = "通过订单ID查询单个订单详情")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "订单不存在")
    })
    public ResponseEntity<Result<Orders>> getOrderById(
            @Parameter(description = "订单ID", required = true)
            @PathVariable Long id) {
        log.info("GET /api/orders/{} - 根据ID获取订单", id);
        Orders order = orderService.getOrderById(id);
        if (order != null) {
            return ResponseEntity.ok(Result.success(order));
        }
        return ResponseEntity.ok(Result.notFound("订单不存在"));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户订单", description = "根据用户ID查询该用户的所有订单")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public ResponseEntity<Result<List<Orders>>> getOrdersByUserId(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        log.info("GET /api/orders/user/{} - 获取用户订单", userId);
        return ResponseEntity.ok(Result.success(orderService.getOrdersByUserId(userId)));
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取订单商品列表", description = "根据订单ID查询订单中的商品列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public ResponseEntity<Result<List<OrderItem>>> getOrderItems(
            @Parameter(description = "订单ID", required = true)
            @PathVariable Long id) {
        log.info("GET /api/orders/{}/items - 获取订单商品列表", id);
        return ResponseEntity.ok(Result.success(orderService.getOrderItems(id)));
    }

    @PostMapping
    @Operation(summary = "创建订单", description = "手动创建订单（通常使用下单接口）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "500", description = "创建失败")
    })
    public ResponseEntity<Result<String>> createOrder(
            @Parameter(description = "订单对象", required = true)
            @RequestBody Orders order) {
        log.info("POST /api/orders - 创建订单: userId={}", order.getUserId());
        int result = orderService.createOrder(order);
        if (result > 0) {
            return ResponseEntity.ok(Result.success("订单创建成功"));
        }
        return ResponseEntity.ok(Result.error("订单创建失败"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新订单", description = "根据ID更新订单信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "500", description = "更新失败")
    })
    public ResponseEntity<Result<String>> updateOrder(
            @Parameter(description = "订单ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "订单对象", required = true)
            @RequestBody Orders order) {
        log.info("PUT /api/orders/{} - 更新订单", id);
        order.setId(id);
        int result = orderService.updateOrder(order);
        if (result > 0) {
            return ResponseEntity.ok(Result.success("订单更新成功"));
        }
        return ResponseEntity.ok(Result.error("订单更新失败"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除订单", description = "根据ID删除订单")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "500", description = "删除失败")
    })
    public ResponseEntity<Result<String>> deleteOrder(
            @Parameter(description = "订单ID", required = true)
            @PathVariable Long id) {
        log.info("DELETE /api/orders/{} - 删除订单", id);
        int result = orderService.deleteOrder(id);
        if (result > 0) {
            return ResponseEntity.ok(Result.success("订单删除成功"));
        }
        return ResponseEntity.ok(Result.error("订单删除失败"));
    }

    @PostMapping("/place")
    @Operation(summary = "下单", description = "创建订单并锁定库存，订单状态为待支付")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "下单成功"),
            @ApiResponse(responseCode = "400", description = "下单失败，库存不足或商品不存在")
    })
    public ResponseEntity<Result<String>> placeOrder(
            @Parameter(description = "下单请求", required = true)
            @RequestBody PlaceOrderRequest request) {
        log.info("POST /api/orders/place - 下单");
        try {
            Long orderId = orderService.placeOrder(request);
            return ResponseEntity.ok(Result.success("下单成功，订单ID: " + orderId));
        } catch (Exception e) {
            log.error("下单失败", e);
            return ResponseEntity.ok(Result.badRequest("下单失败: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "支付订单", description = "支付成功后扣减库存，订单状态改为已支付")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "支付成功"),
            @ApiResponse(responseCode = "400", description = "支付失败，订单状态不正确")
    })
    public ResponseEntity<Result<String>> payOrder(
            @Parameter(description = "订单ID", required = true)
            @PathVariable Long id) {
        log.info("POST /api/orders/{}/pay - 支付订单", id);
        boolean success = orderService.payOrder(id);
        if (success) {
            return ResponseEntity.ok(Result.success("支付成功"));
        }
        return ResponseEntity.ok(Result.badRequest("支付失败，订单状态不正确"));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消订单", description = "取消订单并释放锁定的库存")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "取消成功"),
            @ApiResponse(responseCode = "400", description = "取消失败，订单状态不正确")
    })
    public ResponseEntity<Result<String>> cancelOrder(
            @Parameter(description = "订单ID", required = true)
            @PathVariable Long id) {
        log.info("POST /api/orders/{}/cancel - 取消订单", id);
        boolean success = orderService.cancelOrder(id);
        if (success) {
            return ResponseEntity.ok(Result.success("订单已取消"));
        }
        return ResponseEntity.ok(Result.badRequest("取消失败，订单状态不正确"));
    }
}