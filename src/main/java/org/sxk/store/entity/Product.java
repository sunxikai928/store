package org.sxk.store.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sxk.store.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商品实体")
public class Product {
    @Schema(description = "商品ID", example = "1")
    private Long id;
    
    @Schema(description = "商品名称", example = "iPhone 15 Pro", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    
    @Schema(description = "价格", example = "8999.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;
    
    @Schema(description = "商品描述", example = "苹果最新款手机")
    private String description;
    
    @Schema(description = "状态：1上架 0下架", example = "1")
    private Integer status;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public ProductStatus getStatusEnum() {
        return status != null ? ProductStatus.fromCode(status) : null;
    }

    public void setStatusEnum(ProductStatus status) {
        this.status = status != null ? status.getCode() : null;
    }
}