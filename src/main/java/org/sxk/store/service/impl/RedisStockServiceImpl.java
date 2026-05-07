package org.sxk.store.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.sxk.store.service.RedisStockService;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class RedisStockServiceImpl implements RedisStockService {

    private static final String STOCK_KEY_PREFIX = "stock:product:";
    
    private final StringRedisTemplate redisTemplate;
    
    private static final String DECREASE_STOCK_BATCH_LUA = 
        "local keys = KEYS\n" +
        "local args = ARGV\n" +
        "local len = #keys\n" +
        "\n" +
        "for i = 1, len do\n" +
        "    local key = keys[i]\n" +
        "    local quantity = tonumber(args[i])\n" +
        "\n" +
        "    local current = redis.call('get', key)\n" +
        "    if not current then\n" +
        "        return 0\n" +
        "    end\n" +
        "\n" +
        "    current = tonumber(current)\n" +
        "    if current < quantity then\n" +
        "        return 0\n" +
        "    end\n" +
        "end\n" +
        "\n" +
        "for i = 1, len do\n" +
        "    local key = keys[i]\n" +
        "    local quantity = tonumber(args[i])\n" +
        "    redis.call('decrby', key, quantity)\n" +
        "end\n" +
        "\n" +
        "return 1";
    
    private final RedisScript<Long> decreaseStockBatchScript;

    public RedisStockServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.decreaseStockBatchScript = new DefaultRedisScript<>(DECREASE_STOCK_BATCH_LUA, Long.class);
    }

    private String getStockKey(Long productId) {
        return STOCK_KEY_PREFIX + productId;
    }

    @Override
    public void setStock(Long productId, Integer stock) {
        String key = getStockKey(productId);
        redisTemplate.opsForValue().set(key, String.valueOf(stock));
        log.info("Set stock for product {}: {}", productId, stock);
    }

    @Override
    public Integer getStock(Long productId) {
        String key = getStockKey(productId);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        return Integer.parseInt(value);
    }

    @Override
    public boolean decreaseStock(Long productId, Integer quantity) {
        String key = getStockKey(productId);
        
        Boolean result = redisTemplate.execute((RedisCallback<Boolean>) connection -> {
            byte[] keyBytes = key.getBytes();
            byte[] valueBytes = connection.get(keyBytes);
            
            if (valueBytes == null) {
                return false;
            }
            
            int currentStock = Integer.parseInt(new String(valueBytes));
            if (currentStock < quantity) {
                return false;
            }
            
            connection.decrBy(keyBytes, quantity);
            return true;
        });
        
        if (Boolean.TRUE.equals(result)) {
            log.info("Decreased stock for product {} by {}, remaining: {}", 
                    productId, quantity, getStock(productId));
        } else {
            log.warn("Failed to decrease stock for product {} by {}, insufficient stock", 
                    productId, quantity);
        }
        
        return Boolean.TRUE.equals(result);
    }

    @Override
    public boolean decreaseStockBatch(List<Long> productIds, List<Integer> quantities) {
        if (productIds == null || quantities == null || productIds.size() != quantities.size()) {
            log.error("Invalid parameters for batch decrease");
            return false;
        }
        
        List<String> keys = new ArrayList<>();
        String[] args = new String[quantities.size()];
        
        for (int i = 0; i < productIds.size(); i++) {
            keys.add(getStockKey(productIds.get(i)));
            args[i] = String.valueOf(quantities.get(i));
        }
        
        Long result = redisTemplate.execute(decreaseStockBatchScript, keys, (Object[]) args);
        
        boolean success = result != null && result == 1;
        
        if (success) {
            log.info("Batch decreased stock for {} products", productIds.size());
        } else {
            log.warn("Batch decrease failed for products: {}", productIds);
        }
        
        return success;
    }

    @Override
    public void deleteStock(Long productId) {
        String key = getStockKey(productId);
        Boolean deleted = redisTemplate.delete(key);
        log.info("Deleted stock for product {}: {}", productId, deleted);
    }

    @Override
    public boolean hasStock(Long productId) {
        String key = getStockKey(productId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}