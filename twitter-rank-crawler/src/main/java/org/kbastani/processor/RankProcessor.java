package org.kbastani.processor;

import com.google.cloud.language.v1.ClassificationCategory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kbastani.nlp.TextAnalysis;
import org.kbastani.text.TextEntity;
import org.kbastani.text.TextEntityRepository;
import org.kbastani.tweet.Tweet;
import org.kbastani.tweet.TweetRepository;
import org.kbastani.tweet.TwitterService;
import org.kbastani.user.User;
import org.kbastani.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * This class is the scheduler that makes sure that jobs are scheduled on a fixed
 * interval. The first of the two jobs is discovery of new users based on
 * the most relevant next user to import determined by PageRank. The second
 * job is to schedule a PageRank analysis of all data every five minutes.
 *
 * @author kbastani
 */
@Component
public class RankProcessor {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    private final ExecutorService executorService = Executors.newFixedThreadPool(25);
    private final Log logger = LogFactory.getLog(RankProcessor.class);
    private final TwitterService twitterService;
    private final UserRepository userRepository;
    private final TextEntityRepository textEntityRepository;
    private final TweetRepository tweetRepository;

    public static Boolean resetTimer = false;

    @Value("${neo4j.mazerunner.host:graphdb:7474}")
    private String mazerunnerHost;


    public RankProcessor(TwitterService twitterService, UserRepository userRepository,
                         TextEntityRepository textEntityRepository, TweetRepository tweetRepository) {
        this.twitterService = twitterService;
        this.userRepository = userRepository;
        this.textEntityRepository = textEntityRepository;
        this.tweetRepository = tweetRepository;
    }

    /**
     * Every five minutes a PageRank job is scheduled on the follower graph
     */
    @Scheduled(fixedRate = 100000, initialDelay = 20000)
    public void scheduleFollowerPageRank() {
        logger.info(String.format("FOLLOWS PageRank scheduled on user graph %s", dateFormat.format(new Date())));
        userRepository.updatePageRankForFollowGraph();
    }

    /**
     * Every 3 minutes a PageRank job is scheduled on text entities
     */
    @Scheduled(fixedRate = 90000, initialDelay = 20000)
    public void scheduleEntityPageRank() {
        logger.info(String.format("HAS_ENTITY PageRank scheduled on semantic graph %s", dateFormat.format(new Date())));
        textEntityRepository.updatePageRankForEntityGraph();
    }

    /**
     * Every minute, an attempt to discover a new user to be imported is attempted. This only succeeds if
     * the API is not restricted by a temporary rate limit. This makes sure that only relevant users are
     * discovered over time, to keep the API crawling relevant.
     */
    @Scheduled(fixedRate = 60000)
    public void scheduleDiscoverUser() {

        executorService.execute(() -> {
            if (!resetTimer) {
                // Use ranked users when possible
                User user = userRepository.findRankedUserToCrawl();

                if (user == null) {
                    user = userRepository.findNextUserToCrawl();
                }

                if (user != null) {
                    twitterService.discoverUserByProfileId(user.getProfileId());
                }
            } else resetTimer = false;
            // Update rankings
            logger.info("Updating last ranks...");
            userRepository.setLastPageRank();
            logger.info("Updating current rank...");
            userRepository.updateUserCurrentRank();
            logger.info("Current ranks updated!");
        });


    }

    @Scheduled(fixedRate = 20000, initialDelay = 20000)
    public void scheduleUserActivityScan() {
        logger.info("Importing user activity for sentiment analysis...");
        // Get a user that is ready for an activity scan
        User user = userRepository.findNextUserActivityScan();

        // Wait 60 seconds in-between reads
        if (user.getLastActivityScan() == null ||
                Duration.ofMillis(new Date().getTime() -
                        new Date(user.getLastActivityScan()).getTime()).getSeconds() > 60) {
            user.setLastActivityScan(new Date().getTime());
            userRepository.save(user);
            // Only scan a user's activity feed every 60 seconds at the most
            executorService.submit(() -> twitterService.scanUserActivity(user));
        } else {
            logger.info("No users are available for activity scan at this time...");
        }
    }

    @Scheduled(fixedRate = 5000)
    public void scheduleEntityClassification() {
        // The entity graph abstracts tweets by extracting salient parts of speech.
        //
        // Entities are still too generic to be able to classify the influence of users based on topic.
        //
        // To enable proper content classification, each entity should group together all tweets into a document
        // that is sent to Google's category classifier.
        //
        // By doing this, multiple entities will eventually find themselves grouped together, which should
        // further abstract tweets and users into a topical group with a sentiment ranking.

        logger.info("Finding uncategorized text entity for analysis...");

        executorService.execute(this::entityClassification);
    }

    private void entityClassification() {
        // Query the next top ranked semantic entity that has yet to be classified and submit all tweet text
        // to the classification API
        TextEntity entity = textEntityRepository.findUncategorizedTextEntity(new Date().getTime());

        if (entity != null) {
            logger.info(String.format("Classifying text entity with name: %s...", entity.getName()));

            List<Tweet> tweets = tweetRepository.findTweetsForTextEntity(entity.getName());

            List<ClassificationCategory> classificationCategories = new ArrayList<>();

            if (tweets.size() >= 3) {
                try {
                    // Compose a single string of text from multiple tweets and send it to the NLP classifier
                    classificationCategories =
                            TextAnalysis.classifyText(tweets.stream().map(Tweet::getText)
                                    .collect(Collectors.joining("\n")));
                } catch (Exception ex) {
                    logger.info(String.format("Error classifying tweets: %s", ex.getMessage()));
                }
            }

            // Create new Category labeled nodes and connect any retrieved topics from the NLP API
            // under the relationship: (:TextEntity)-[:HAS_CATEGORY]->(:Category)
            if (classificationCategories.size() > 0) {

                logger.info(String.format("Text entity categories found for %s: %s", entity.getName(),
                        classificationCategories.stream().map(c -> c.getName()
                                .toLowerCase()).collect(Collectors.joining(", "))));

                // Attach the categories to the entity
                textEntityRepository.saveTextEntityCategories(entity.getName(),
                        classificationCategories.stream().map(c -> c.getName().toLowerCase())
                                .collect(Collectors.toList()));
            } else {
                logger.info(String.format("No categories for text entity with name: %s...", entity.getName()));

                // Marks the text entity as processed so that the next scheduled job will skip it
                textEntityRepository.saveTextEntityCategories(entity.getName(), new ArrayList<>());
            }
        }
    }
}