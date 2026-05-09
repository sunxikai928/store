package org.sxk.store.service;

import org.sxk.store.entity.MessageRecord;

public interface MessageRecordService {
    Long saveMessage(String exchange, String routingKey, String messageBody);
    
    void updateStatus(Long id, Integer status, String failReason);
    
    void handleMessageFailure(Long id, String failReason);
    
    void retryFailedMessages();
}