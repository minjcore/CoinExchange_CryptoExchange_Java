package com.gtelpay.app.orchestration.payout;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * S6 RabbitMQ wiring for the payout worker (withdraw + IBFT async settlement).
 * Exchange: core.commands (topic, durable). Feature-flagged via payout.amqp.enabled.
 */
@Configuration
@ConditionalOnProperty(name = "payout.amqp.enabled", havingValue = "true")
public class PayoutAmqpConfig {

    public static final String EXCHANGE = "core.commands";

    public static final String WITHDRAW_PAYOUT_QUEUE       = "orchestration.withdraw-payout";
    public static final String WITHDRAW_PAYOUT_ROUTING_KEY = "core.commands.withdraw-payout";

    public static final String IBFT_PAYOUT_QUEUE           = "orchestration.ibft-payout";
    public static final String IBFT_PAYOUT_ROUTING_KEY     = "core.commands.ibft-payout";

    @Bean
    public TopicExchange payoutCommandsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue withdrawPayoutQueue() {
        return new Queue(WITHDRAW_PAYOUT_QUEUE, true);
    }

    @Bean
    public Queue ibftPayoutQueue() {
        return new Queue(IBFT_PAYOUT_QUEUE, true);
    }

    @Bean
    public Binding withdrawPayoutBinding(@Qualifier("withdrawPayoutQueue") Queue withdrawPayoutQueue,
                                         TopicExchange payoutCommandsExchange) {
        return BindingBuilder.bind(withdrawPayoutQueue).to(payoutCommandsExchange).with(WITHDRAW_PAYOUT_ROUTING_KEY);
    }

    @Bean
    public Binding ibftPayoutBinding(@Qualifier("ibftPayoutQueue") Queue ibftPayoutQueue,
                                     TopicExchange payoutCommandsExchange) {
        return BindingBuilder.bind(ibftPayoutQueue).to(payoutCommandsExchange).with(IBFT_PAYOUT_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter payoutJsonConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
