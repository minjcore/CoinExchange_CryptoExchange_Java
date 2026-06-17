package com.gtelpay.core.accounting.amqp;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * S6 RabbitMQ wiring for the accounting worker (async-api/core-commands.yaml).
 * Disabled unless {@code accounting.amqp.enabled=true} so tests / brokerless runs don't
 * open a connection. Exchange {@code core.commands} (topic), queue bound on the
 * bank-deposit routing key.
 */
@Configuration
@ConditionalOnProperty(name = "accounting.amqp.enabled", havingValue = "true")
public class AccountingAmqpConfig {

    public static final String EXCHANGE = "core.commands";
    public static final String BANK_DEPOSIT_QUEUE = "accounting.bank-deposit";
    public static final String BANK_DEPOSIT_ROUTING_KEY = "core.commands.bank-deposit";

    @Bean
    public TopicExchange coreCommandsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue bankDepositQueue() {
        return new Queue(BANK_DEPOSIT_QUEUE, true);
    }

    @Bean
    public Binding bankDepositBinding(Queue bankDepositQueue, TopicExchange coreCommandsExchange) {
        return BindingBuilder.bind(bankDepositQueue).to(coreCommandsExchange).with(BANK_DEPOSIT_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
