package com.gtelpay.core.wallet.amqp;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * S6 RabbitMQ wiring for the wallet worker (async-api/core-commands.yaml).
 * Disabled unless {@code wallet.amqp.enabled=true}.
 * Exchange {@code core.commands} (topic), queue bound on the wallet-credit routing key.
 */
@Configuration
@ConditionalOnProperty(name = "wallet.amqp.enabled", havingValue = "true")
public class WalletAmqpConfig {

    public static final String EXCHANGE             = "core.commands";
    public static final String WALLET_CREDIT_QUEUE  = "wallet.wallet-credit";
    public static final String WALLET_CREDIT_KEY    = "core.commands.wallet-credit";

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

    @Bean
    public Jackson2JsonMessageConverter walletJsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
