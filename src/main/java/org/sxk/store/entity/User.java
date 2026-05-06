package org.sxk.store.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sxk.store.enums.UserStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户实体")
public class User {
    @Schema(description = "用户ID", example = "1")
    private Long id;
    
    @Schema(description = "用户名", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;
    
    @Schema(description = "密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
    
    @Schema(description = "手机号", example = "13800138000")
    private String phone;
    
    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;
    
    @Schema(description = "状态：1正常 0禁用", example = "1")
    private Integer status;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public UserStatus getStatusEnum() {
        return status != null ? UserStatus.fromCode(status) : null;
    }

    public void setStatusEnum(UserStatus status) {
        this.status = status != null ? status.getCode() : null;
    }
}