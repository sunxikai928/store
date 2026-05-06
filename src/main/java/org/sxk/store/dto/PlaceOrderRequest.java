package org.sxk.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sxk.store.entity.OrderItem;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "下单请求")
public class PlaceOrderRequest {
    
    @Schema(description = "用户ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;
    
    @Schema(description = "商品列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<OrderItemDTO> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "订单项")
    public static class OrderItemDTO {
        
        @Schema(description = "商品ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long productId;
        
        @Schema(description = "数量", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer quantity;
    }
}