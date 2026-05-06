package org.sxk.store.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sxk.store.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单实体")
public class Orders {
    @Schema(description = "订单ID", example = "1")
    private Long id;
    
    @Schema(description = "用户ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;
    
    @Schema(description = "订单总金额", example = "17998.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal totalAmount;
    
    @Schema(description = "订单状态：0待支付 1已支付 2已取消", example = "0")
    private Integer status;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public OrderStatus getStatusEnum() {
        return status != null ? OrderStatus.fromCode(status) : null;
    }

    public void setStatusEnum(OrderStatus status) {
        this.status = status != null ? status.getCode() : null;
    }
}