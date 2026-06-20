package com.gtelpay.app.orchestration.deposit;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ publisher wiring for deposit notify — only active when deposit.amqp.enabled=true.
 * Orchestration publishes to {@code core.commands} exchange; no queue binding needed here
 * (queue is declared by the accounting worker, ADR-041).
 */
@Configuration
@ConditionalOnProperty(name = "deposit.amqp.enabled", havingValue = "true")
public class DepositAmqpConfig {

    public static final String EXCHANGE = "core.commands";
    public static final String BANK_DEPOSIT_ROUTING_KEY = "core.commands.bank-deposit";

    @Bean
    public CachingConnectionFactory depositConnectionFactory(
            @Value("${spring.rabbitmq.host:localhost}") String host,
            @Value("${spring.rabbitmq.port:5672}") int port,
            @Value("${spring.rabbitmq.username:guest}") String username,
            @Value("${spring.rabbitmq.password:guest}") String password) {
        ConnectionFactory cf = new ConnectionFactory();
        cf.setHost(host);
        cf.setPort(port);
        cf.setUsername(username);
        cf.setPassword(password);
        return new CachingConnectionFactory(cf);
    }

    @Bean
    public RabbitTemplate depositRabbitTemplate(CachingConnectionFactory depositConnectionFactory) {
        RabbitTemplate t = new RabbitTemplate(depositConnectionFactory);
        t.setMessageConverter(new Jackson2JsonMessageConverter());
        return t;
    }
}
