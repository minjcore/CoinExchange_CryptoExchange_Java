package com.gtelpay.app.orchestration.payout;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Full RabbitMQ wiring for the payout worker — only loaded when payout.amqp.enabled=true.
 * Spring Boot's RabbitAutoConfiguration is excluded globally so the app starts clean without a broker.
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
    public CachingConnectionFactory payoutConnectionFactory(
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
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory payoutConnectionFactory) {
        RabbitTemplate t = new RabbitTemplate(payoutConnectionFactory);
        t.setMessageConverter(payoutJsonConverter());
        return t;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(CachingConnectionFactory payoutConnectionFactory) {
        return new RabbitAdmin(payoutConnectionFactory);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            CachingConnectionFactory payoutConnectionFactory) {
        SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(payoutConnectionFactory);
        f.setMessageConverter(payoutJsonConverter());
        return f;
    }

    @Bean
    public Jackson2JsonMessageConverter payoutJsonConverter() {
        return new Jackson2JsonMessageConverter();
    }

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
}
