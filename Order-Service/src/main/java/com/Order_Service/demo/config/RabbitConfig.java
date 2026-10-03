package com.Order_Service.demo.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RabbitConfig {

    public static final String SAGA_EXCHANGE = "saga.exchange";
    public static final String ORDER_SAGA_QUEUE = "order.saga.queue";
    public static final String ORDER_SAGA_DLQ = "order.saga.dlq";
    public static final String ORDER_SAGA_DLX = "saga.exchange.dlx";

    public static final String RK_ORDER_CREATED = "order.created";
    public static final String RK_PAYMENT_COMPLETED = "payment.completed";
    public static final String RK_PAYMENT_FAILED = "payment.failed";
    public static final String RK_PAYMENT_REFUNDED = "payment.refunded";
    public static final String RK_INVENTORY_RESERVED = "inventory.reserved";
    public static final String RK_INVENTORY_RESERVATION_FAILED = "inventory.reservation.failed";
    public static final String RK_INVENTORY_RELEASED = "inventory.released";
    public static final String RK_SHIPPING_SCHEDULED = "shipping.scheduled";
    public static final String RK_SHIPPING_FAILED = "shipping.failed";

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public TopicExchange sagaExchange() {
        return new TopicExchange(SAGA_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(ORDER_SAGA_DLX, true, false);
    }

    @Bean
    public Queue orderSagaQueue() {
        return QueueBuilder.durable(ORDER_SAGA_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_SAGA_DLX)
                .withArgument("x-dead-letter-routing-key", "order.saga.dead")
                .build();
    }

    @Bean
    public Queue orderSagaDlq() {
        return QueueBuilder.durable(ORDER_SAGA_DLQ).build();
    }

    @Bean
    public Binding dlqBinding(Queue orderSagaDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(orderSagaDlq).to(deadLetterExchange).with("order.saga.dead");
    }

    @Bean
    public Declarables orderSagaBindings(Queue orderSagaQueue, TopicExchange sagaExchange) {
        return new Declarables(
                BindingBuilder.bind(orderSagaQueue).to(sagaExchange).with(RK_PAYMENT_COMPLETED),
                BindingBuilder.bind(orderSagaQueue).to(sagaExchange).with(RK_PAYMENT_FAILED),
                BindingBuilder.bind(orderSagaQueue).to(sagaExchange).with(RK_PAYMENT_REFUNDED),
                BindingBuilder.bind(orderSagaQueue).to(sagaExchange).with(RK_INVENTORY_RESERVED),
                BindingBuilder.bind(orderSagaQueue).to(sagaExchange).with(RK_INVENTORY_RESERVATION_FAILED),
                BindingBuilder.bind(orderSagaQueue).to(sagaExchange).with(RK_INVENTORY_RELEASED),
                BindingBuilder.bind(orderSagaQueue).to(sagaExchange).with(RK_SHIPPING_SCHEDULED),
                BindingBuilder.bind(orderSagaQueue).to(sagaExchange).with(RK_SHIPPING_FAILED));
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}