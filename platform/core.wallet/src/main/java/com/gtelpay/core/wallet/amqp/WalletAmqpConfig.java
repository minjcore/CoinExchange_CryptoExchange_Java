package com.gtelpay.core.wallet.amqp;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ wiring for the wallet worker (ADR-041, async-api/core-commands.yaml).
 * Disabled unless {@code wallet.amqp.enabled=true}.
 * Self-contained: owns its own connection factory and listener container factory.
 */
@Configuration
@ConditionalOnProperty(name = "wallet.amqp.enabled", havingValue = "true")
public class WalletAmqpConfig {

    public static final String EXCHANGE             = "core.commands";
    public static final String WALLET_CREDIT_QUEUE  = "wallet.wallet-credit";
    public static final String WALLET_CREDIT_KEY    = "core.commands.wallet-credit";
    public static final String LISTENER_FACTORY     = "walletListenerContainerFactory";

    @Bean
    public CachingConnectionFactory walletConnectionFactory(
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

    @Bean(name = LISTENER_FACTORY)
    public SimpleRabbitListenerContainerFactory walletListenerContainerFactory(
            CachingConnectionFactory walletConnectionFactory) {
        SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(walletConnectionFactory);
        f.setMessageConverter(new Jackson2JsonMessageConverter());
        return f;
    }

    @Bean
    public TopicExchange walletCoreCommandsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue walletCreditQueue() {
        return new Queue(WALLET_CREDIT_QUEUE, true);
    }

    @Bean
    public Binding walletCreditBinding(Queue walletCreditQueue, TopicExchange walletCoreCommandsExchange) {
        return BindingBuilder.bind(walletCreditQueue).to(walletCoreCommandsExchange).with(WALLET_CREDIT_KEY);
    }
}
