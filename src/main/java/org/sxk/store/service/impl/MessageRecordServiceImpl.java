package org.sxk.store.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sxk.store.dto.OrderMessageDTO;
import org.sxk.store.entity.MessageRecord;
import org.sxk.store.enums.MessageStatus;
import org.sxk.store.mapper.MessageRecordMapper;
import org.sxk.store.service.MessageRecordService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MessageRecordServiceImpl implements MessageRecordService {

    private static final int MAX_RETRY_COUNT = 5;
    private static final int INITIAL_RETRY_DELAY_SECONDS = 60;
    private static final String RETRY_LOCK_KEY = "message:retry:lock";

    private final MessageRecordMapper messageRecordMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;

    public MessageRecordServiceImpl(MessageRecordMapper messageRecordMapper,
                                    RabbitTemplate rabbitTemplate,
                                    ObjectMapper objectMapper,
                                    RedissonClient redissonClient) {
        this.messageRecordMapper = messageRecordMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.redissonClient = redissonClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveMessage(String exchange, String routingKey, String messageBody, String messageType) {
        MessageRecord record = new MessageRecord();
        record.setExchange(exchange);
        record.setRoutingKey(routingKey);
        record.setMessageBody(messageBody);
        record.setMessageType(messageType);
        record.setRetryCount(0);
        record.setNextRetryTime(LocalDateTime.now());
        record.setStatusEnum(MessageStatus.SENT);

        messageRecordMapper.insert(record);
        log.info("Message record saved: id={}, exchange={}, routingKey={}, messageType={}",
                record.getId(), exchange, routingKey, messageType);

        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status, String failReason) {
        messageRecordMapper.updateStatus(id, status, failReason);
        log.info("Message record status updated: id={}, status={}, failReason={}", id, status, failReason);
    }

    @Override
    public void updateMessage(Long id, String messageBody) {
        messageRecordMapper.updateMessage(id, messageBody);
        log.info("Message record message updated: id={}, messageBody={}", id, messageBody);
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
        RLock lock = redissonClient.getLock(RETRY_LOCK_KEY);

        try {
            if (!lock.tryLock(0, 5, TimeUnit.SECONDS)) {
                log.debug("Another instance is processing failed messages, skipping this run");
                return;
            }

            List<MessageRecord> failedMessages = messageRecordMapper.findFailedMessagesToRetry(LocalDateTime.now());

            if (failedMessages.isEmpty()) {
                return;
            }

            log.info("Found {} failed messages to retry", failedMessages.size());

            for (MessageRecord record : failedMessages) {
                try {
                    Object message = objectMapper.readValue(record.getMessageBody(), Object.class);
                    rabbitTemplate.convertAndSend(record.getExchange(), record.getRoutingKey(),  message);
                    messageRecordMapper.updateStatus(record.getId(), MessageStatus.SENT.getCode(), null);
                    log.info("Retried message: id={}, exchange={}, routingKey={}",
                            record.getId(), record.getExchange(), record.getRoutingKey());
                } catch (JsonProcessingException e) {
                    log.error("Failed to deserialize message body: id={}", record.getId(), e);
                } catch (Exception e) {
                    log.error("Failed to send retry message: id={}", record.getId(), e);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Retry failed messages interrupted", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}