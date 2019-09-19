package org.kbastani.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.transaction.Transaction;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

import java.util.HashMap;

/**
 * This configuration defines the setup information for RabbitMQ queues and a command line runner bean
 * that will ensure that a unique constraint is applied to the connected Neo4j database server
 *
 * @author kbastani
 */
@Configuration
public class TwitterCrawlerConfig {

    private final Log logger = LogFactory.getLog(TwitterCrawlerConfig.class);

    @Bean
    Queue follows() {
        return new Queue("twitter.follows", true, false, false);
    }

    @Bean
    Queue followers() {
        return new Queue("twitter.followers", true, false, false);
    }

    @Value("${spring.social.twitter.appId}")
    private String appId;

    @Value("${spring.social.twitter.appSecret}")
    private String appSecret;

    @Value("${spring.social.twitter.accessToken}")
    private String accessToken;

    @Value("${spring.social.twitter.accessTokenSecret}")
    private String accessTokenSecret;

    @Bean
    Twitter twitter(final @Value("${spring.social.twitter.appId}") String appId,
                    final @Value("${spring.social.twitter.appSecret}") String appSecret,
                    final @Value("${spring.social.twitter.accessToken}") String accessToken,
                    final @Value("${spring.social.twitter.accessTokenSecret}") String accessTokenSecret) {
        Twitter twitter = TwitterFactory.getSingleton();
        twitter.setOAuthConsumer(appId, appSecret);
        twitter.setOAuthAccessToken(new AccessToken(accessToken, accessTokenSecret));
        return twitter;
    }

    @Bean
    CommandLineRunner commandLineRunner(SessionFactory sessionFactory) {
        return (args) -> {
            // Make sure that a constraint is created on the Neo4j database
            // This constraint ensures that each profileId is unique per user node
            Session session = sessionFactory.openSession();
            try (Transaction tx = session.beginTransaction()) {
                session.query("CREATE CONSTRAINT ON (user:User) ASSERT user.profileId IS UNIQUE",
                        new HashMap<>());
                session.query("CREATE CONSTRAINT ON (entity:TextEntity) ASSERT entity.name IS UNIQUE",
                        new HashMap<>());
                tx.commit();
            } catch (Exception ex) {
                // The constraint is already created or the database is not available
                logger.error(ex);
            }
        };
    }
}
