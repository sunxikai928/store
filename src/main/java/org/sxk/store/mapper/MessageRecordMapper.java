package org.sxk.store.mapper;

import org.sxk.store.entity.MessageRecord;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRecordMapper {
    int insert(MessageRecord record);
    
    int update(MessageRecord record);
    
    MessageRecord findById(Long id);
    
    List<MessageRecord> findByStatus(Integer status);
    
    List<MessageRecord> findFailedMessagesToRetry(LocalDateTime now);
    
    int updateStatus(Long id, Integer status, String failReason);

    int updateMessage(Long id, String messageBody);

    int incrementRetryCount(Long id, LocalDateTime nextRetryTime);
}