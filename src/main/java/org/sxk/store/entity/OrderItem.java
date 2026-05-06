package org.sxk.store.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单项")
public class OrderItem {
    @Schema(description = "订单项ID", example = "1")
    private Long id;
    
    @Schema(description = "订单ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orderId;
    
    @Schema(description = "商品ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;
    
    @Schema(description = "数量", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;
    
    @Schema(description = "单价", example = "8999.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}