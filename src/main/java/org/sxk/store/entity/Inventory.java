package org.sxk.store.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "库存实体")
public class Inventory {
    @Schema(description = "库存ID", example = "1")
    private Long id;
    
    @Schema(description = "商品ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;
    
    @Schema(description = "库存数量", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer stock;
    
    @Schema(description = "锁定库存", example = "0")
    private Integer lockStock;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}