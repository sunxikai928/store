package org.sxk.store.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.sxk.store.config.RabbitMQConfig;
import org.sxk.store.entity.MessageRecord;
import org.sxk.store.enums.MessageStatus;
import org.sxk.store.mapper.MessageRecordMapper;
import org.sxk.store.service.MessageRecordService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class MessageRecordServiceImpl implements MessageRecordService {

    private static final int MAX_RETRY_COUNT = 5;
    private static final int INITIAL_RETRY_DELAY_SECONDS = 60;
    
    private final MessageRecordMapper messageRecordMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public MessageRecordServiceImpl(MessageRecordMapper messageRecordMapper, 
                                   RabbitTemplate rabbitTemplate,
                                   ObjectMapper objectMapper) {
        this.messageRecordMapper = messageRecordMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveMessage(String exchange, String routingKey, String messageBody) {
        MessageRecord record = new MessageRecord();
        record.setExchange(exchange);
        record.setRoutingKey(routingKey);
        record.setMessageBody(messageBody);
        record.setRetryCount(0);
        record.setNextRetryTime(LocalDateTime.now());
        record.setStatusEnum(MessageStatus.SENT);
        
        messageRecordMapper.insert(record);
        log.info("Message record saved: id={}, exchange={}, routingKey={}", 
                record.getId(), exchange, routingKey);
        
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status, String failReason) {
        messageRecordMapper.updateStatus(id, status, failReason);
        log.info("Message record status updated: id={}, status={}, failReason={}", id, status, failReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleMessageFailure(Long id, String failReason) {
        MessageRecord record = messageRecordMapper.findById(id);
        if (record == null) {
            log.warn("Message record not found: id={}", id);
            return;
        }

        if (record.getRetryCount() >= MAX_RETRY_COUNT) {
            log.warn("Message record reached max retry count: id={}, retryCount={}", id, record.getRetryCount());
            messageRecordMapper.updateStatus(id, MessageStatus.CONSUMED_FAILED.getCode(), failReason);
            return;
        }

        int delaySeconds = INITIAL_RETRY_DELAY_SECONDS * (int) Math.pow(2, record.getRetryCount());
        LocalDateTime nextRetryTime = LocalDateTime.now().plusSeconds(delaySeconds);
        
        messageRecordMapper.incrementRetryCount(id, nextRetryTime);
        messageRecordMapper.updateStatus(id, MessageStatus.CONSUMED_FAILED.getCode(), failReason);
        
        log.info("Message record failure handled: id={}, retryCount={}, nextRetryTime={}", 
                id, record.getRetryCount() + 1, nextRetryTime);
    }

    @Override
    @Scheduled(fixedDelay = 30000)
    public void retryFailedMessages() {
        List<MessageRecord> failedMessages = messageRecordMapper.findFailedMessagesToRetry(LocalDateTime.now());
        
        if (failedMessages.isEmpty()) {
            return;
        }
        
        log.info("Found {} failed messages to retry", failedMessages.size());
        
        for (MessageRecord record : failedMessages) {
            try {
                Object message = objectMapper.readValue(record.getMessageBody(), Object.class);
                
                rabbitTemplate.convertAndSend(record.getExchange(), record.getRoutingKey(), message);
                
                messageRecordMapper.updateStatus(record.getId(), MessageStatus.SENT.getCode(), null);
                log.info("Retried message: id={}, exchange={}, routingKey={}", 
                        record.getId(), record.getExchange(), record.getRoutingKey());
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize message body: id={}", record.getId(), e);
            } catch (Exception e) {
                log.error("Failed to send retry message: id={}", record.getId(), e);
            }
        }
    }
}