package org.sxk.store.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sxk.store.enums.MessageStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageRecord {
    private Long id;
    
    private String exchange;
    
    private String routingKey;
    
    private String messageBody;
    
    private String messageType;
    
    private Integer retryCount;
    
    private LocalDateTime nextRetryTime;
    
    private Integer status;
    
    private String failReason;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;

    public MessageStatus getStatusEnum() {
        return status != null ? MessageStatus.fromCode(status) : null;
    }

    public void setStatusEnum(MessageStatus status) {
        this.status = status != null ? status.getCode() : null;
    }
}