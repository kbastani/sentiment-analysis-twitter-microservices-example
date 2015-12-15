package org.kbastani.scheduling;

import org.kbastani.twitter.TwitterService;
import org.kbastani.twitter.User;
import org.kbastani.twitter.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.server.Neo4jServer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class AnalyticsScheduler {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    private final RestTemplate restTemplate;
    private final Neo4jServer neo4jServer;
    private final TwitterService twitterService;
    private final UserRepository userRepository;
    public static Boolean resetTimer = false;

    @Autowired
    public AnalyticsScheduler(Neo4jServer neo4jServer, TwitterService twitterService, UserRepository userRepository) {
        this.twitterService = twitterService;
        this.userRepository = userRepository;
        this.restTemplate = new RestTemplate();
        this.neo4jServer = neo4jServer;
    }

    @Scheduled(fixedRate = 300000, initialDelay = 300000)
    public void schedulePageRank() {
        System.out.println("PageRank scheduled on follows graph " + dateFormat.format(new Date()));

        if(userRepository.findNextUserToCrawl() != null) {
            // Make sure Neo4j is available
            restTemplate.getForEntity(String.format("%s/service/mazerunner/analysis/pagerank/FOLLOWS", neo4jServer.url()), null);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void scheduleDiscoverUser() {
        if(!resetTimer) {
            System.out.println("Discover user scheduled on follows graph " + dateFormat.format(new Date()));

            // Use ranked users when possible
            User user = userRepository.findRankedUserToCrawl();

            if(user == null) {
                user = userRepository.findNextUserToCrawl();
            }

            if (user != null) {
                twitterService.discoverUserByProfileId(user.getProfileId());
            }
        } else {
            resetTimer = false;
        }
    }
}