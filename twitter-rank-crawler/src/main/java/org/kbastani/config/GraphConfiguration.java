package org.kbastani.config;

import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.server.Neo4jServer;
import org.springframework.data.neo4j.server.RemoteServer;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Manages the configuration for a Neo4j graph database server
 *
 * @author Kenny Bastani
 */
@EnableNeo4jRepositories(basePackages = "org.kbastani")
@EnableTransactionManagement
@Configuration
public class GraphConfiguration extends Neo4jConfiguration {

    @Value("${spring.neo4j.host}")
    private String host;

    @Value("${spring.neo4j.port}")
    private String port;

    @Bean
    public Neo4jServer neo4jServer() {
        return new RemoteServer(String.format("http://%s:%s", host, port));
    }

    @Bean
    public SessionFactory getSessionFactory() {
        return new SessionFactory("org.kbastani.twitter");
    }

}

