package org.sxk.store.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "order-exchange";
    public static final String ORDER_QUEUE = "order-queue";
    public static final String ORDER_ROUTING_KEY = "order.create";

    public static final String ORDER_DELAY_EXCHANGE = "order-delay-exchange";
    public static final String ORDER_DELAY_QUEUE = "order-delay-queue";
    public static final String ORDER_DELAY_ROUTING_KEY = "order.delay.cancel";

    public static final long ORDER_DELAY_TIME = 15 * 60 * 1000L;

    @Bean
    public Queue orderQueue() {
        return new Queue(ORDER_QUEUE, true);
    }

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Binding orderBinding(Queue orderQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderQueue).to(orderExchange).with(ORDER_ROUTING_KEY);
    }

    @Bean
    public Queue orderDelayQueue() {
        return new Queue(ORDER_DELAY_QUEUE, true);
    }

    @Bean
    public CustomExchange orderDelayExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(ORDER_DELAY_EXCHANGE, "x-delayed-message", true, false, args);
    }

    @Bean
    public Binding orderDelayBinding(Queue orderDelayQueue, CustomExchange orderDelayExchange) {
        return BindingBuilder.bind(orderDelayQueue).to(orderDelayExchange).with(ORDER_DELAY_ROUTING_KEY).noargs();
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}