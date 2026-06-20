package com.gtelpay.core.accounting.amqp;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ wiring for the accounting worker (ADR-041, async-api/core-commands.yaml).
 * Disabled unless {@code accounting.amqp.enabled=true}.
 * Self-contained: owns its own connection factory, template, and listener container factory
 * so it does not depend on payout AMQP being enabled.
 */
@Configuration
@ConditionalOnProperty(name = "accounting.amqp.enabled", havingValue = "true")
public class AccountingAmqpConfig {

    public static final String EXCHANGE = "core.commands";
    public static final String BANK_DEPOSIT_QUEUE = "accounting.bank-deposit";
    public static final String BANK_DEPOSIT_ROUTING_KEY = "core.commands.bank-deposit";
    public static final String WALLET_CREDIT_ROUTING_KEY = "core.commands.wallet-credit";
    public static final String LISTENER_FACTORY = "accountingListenerContainerFactory";

    @Bean
    public CachingConnectionFactory accountingConnectionFactory(
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
    public RabbitTemplate accountingRabbitTemplate(CachingConnectionFactory accountingConnectionFactory) {
        RabbitTemplate t = new RabbitTemplate(accountingConnectionFactory);
        t.setMessageConverter(new Jackson2JsonMessageConverter());
        return t;
    }

    @Bean(name = LISTENER_FACTORY)
    public SimpleRabbitListenerContainerFactory accountingListenerContainerFactory(
            CachingConnectionFactory accountingConnectionFactory) {
        SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(accountingConnectionFactory);
        f.setMessageConverter(new Jackson2JsonMessageConverter());
        return f;
    }

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
}
