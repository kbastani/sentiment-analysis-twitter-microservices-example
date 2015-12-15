package org.kbastani;

import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.transaction.Transaction;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.impl.TwitterTemplate;

@SpringBootApplication
@EnableScheduling
public class TwitterAnalyticsApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        new SpringApplicationBuilder(TwitterAnalyticsApplication.class).web(true).run(args);
    }

    @Bean
    public EmbeddedServletContainerFactory servletContainer() {
        return new TomcatEmbeddedServletContainerFactory();
    }

    @Bean
    Queue profiles() {
        return new Queue("twitter.profiles", true, true, true);
    }

    @Bean
    Queue follows() {
        return new Queue("twitter.follows", true, true, true);
    }

    @Bean
    Queue followers() {
        return new Queue("twitter.followers", true, true, true);
    }

    @Bean
    TopicExchange exchange() {
        return new TopicExchange("twitter.exchange", true, true);
    }

    @Bean
    public Twitter twitter(final @Value("${spring.social.twitter.appId}") String appId,
                           final @Value("${spring.social.twitter.appSecret}") String appSecret,
                           final @Value("${spring.social.twitter.accessToken}") String accessToken,
                           final @Value("${spring.social.twitter.accessTokenSecret}") String accessTokenSecret) {
        return new TwitterTemplate(appId, appSecret, accessToken, accessTokenSecret);
    }

    @Bean
    public AmqpTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(500);
        backOffPolicy.setMultiplier(10.0);
        backOffPolicy.setMaxInterval(10000);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        template.setRetryTemplate(retryTemplate);
        return template;
    }

    @Profile("production")
    @Bean
    CommandLineRunner commandLineRunner(Neo4jSession neo4jSession) {
        return (args) -> {
            // Make sure that a constraint is created on the Neo4j database
            try {
                Transaction tx = neo4jSession.beginTransaction();
                neo4jSession.execute("CREATE CONSTRAINT ON (user:User) ASSERT user.profileId IS UNIQUE");
                tx.commit();
                tx.close();
            } catch (Exception ex) {
                // The constraint is already created
                logger.error(ex);
            }

        };
    }
}
