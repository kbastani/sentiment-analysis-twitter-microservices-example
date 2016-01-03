package org.kbastani.scheduling;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

/**
 * This class is the scheduler that makes sure that jobs are scheduled on a fixed
 * interval. The first of the two jobs is discovery of new users based on
 * the most relevant next user to import determined by PageRank. The second
 * job is to schedule a PageRank analysis of all data every five minutes.
 *
 * @author kbastani
 */
@Component
public class AnalyticsScheduler {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    private static final String PAGERANK_JOB_URL = "%s/service/mazerunner/analysis/pagerank/FOLLOWS";
    private final Log logger = LogFactory.getLog(AnalyticsScheduler.class);
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

    /**
     * Every five minutes a PageRank job is scheduled with Neo4j's Mazerunner service
     */
    @Scheduled(fixedRate = 300000, initialDelay = 20000)
    public void schedulePageRank() {
        logger.info("PageRank scheduled on follows graph " + dateFormat.format(new Date()));

        if (userRepository.findNextUserToCrawl() != null) {
            String jobUrl = String.format(PAGERANK_JOB_URL, neo4jServer.url());
            restTemplate.getForEntity(jobUrl, null);
        }
    }

    /**
     * Every minute, an attempt to discover a new user to be imported is attempted. This only succeeds if
     * the API is not restricted by a temporary rate limit. This makes sure that only relevant users are
     * discovered over time, to keep the API crawling relevant.
     */
    @Scheduled(fixedRate = 60000)
    public void scheduleDiscoverUser() {
        if (!resetTimer) {
            // Use ranked users when possible
            User user = userRepository.findRankedUserToCrawl();

            if (user == null) {
                user = userRepository.findNextUserToCrawl();
            }

            if (user != null) {
                twitterService.discoverUserByProfileId(user.getProfileId());
            }
        } else {
            resetTimer = false;
        }

        // Update rankings
        logger.info("Updating last ranks...");
        userRepository.setLastPageRank();
        logger.info("Updating current rank...");
        userRepository.updateUserCurrentRank();
        logger.info("Current ranks updated!");
    }
}