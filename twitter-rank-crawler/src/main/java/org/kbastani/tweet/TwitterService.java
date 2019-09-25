package org.kbastani.tweet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.language.v1.Entity;
import com.google.cloud.language.v1.Sentiment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kbastani.math.Statistics;
import org.kbastani.nlp.TextAnalysis;
import org.kbastani.text.HasEntity;
import org.kbastani.text.HasEntityRepository;
import org.kbastani.text.TextEntity;
import org.kbastani.user.SentimentResult;
import org.kbastani.user.User;
import org.kbastani.user.UserRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.util.IterableUtils;
import org.springframework.stereotype.Service;
import twitter4j.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * This class implements the service contract for {@link TwitterService} and
 * is responsible for discovering users by screen name or profile ID.
 *
 * @author kbastani
 */
@Service
public class TwitterService {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    private final Log log = LogFactory.getLog(TwitterService.class);
    private static final String QUEUE_NAME = "twitter.followers";
    private final Twitter twitter;
    private final UserRepository userRepository;
    private final TweetRepository tweetRepository;
    private final TweetedRepository tweetedRepository;
    private final HasEntityRepository hasEntityRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    // These two fields are constants that target users below follows/following thresholds
    private static final Integer MAX_FOLLOWS = 50000;
    private static final Integer MAX_FOLLOWERS = 50000;

    @Autowired
    public TwitterService(Twitter twitter, UserRepository userRepository, TweetRepository tweetRepository,
                          TweetedRepository tweetedRepository, HasEntityRepository hasEntityRepository,
                          RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.twitter = twitter;
        this.userRepository = userRepository;
        this.tweetRepository = tweetRepository;
        this.tweetedRepository = tweetedRepository;
        this.hasEntityRepository = hasEntityRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Discover a user on Twitter using only their screen name
     *
     * @param screenName is the screen name of the user on Twitter
     * @return a user that has been retrieved from the Twitter API and saved to Neo4j
     */
    public User discoverUserByScreenName(String screenName) {
        User user;

        try {
            user = Optional.of(twitter.users().showUser(screenName))
                    .map(User::new)
                    .get();
        } catch (TwitterException e) {
            throw new RuntimeException("Error discovering new user...", e);
        }

        // Set the user's default values
        user.setPagerank(0f);
        user.setImported(true);

        user = getUser(user);

        return user;
    }

    /**
     * Discover a user on Twitter using their profile ID
     *
     * @param profileId is the profile ID of the user on thw Twitter API
     * @return a user that has been retrieved from the Twitter API and saved to Neo4j
     */
    public User discoverUserByProfileId(Long profileId) {
        User user;

        try {
            user = Optional.of(twitter.users().showUser(profileId))
                    .map(User::new)
                    .get();
            user = getUser(user);
            log.info(String.format("Discover user: %s", user.getScreenName()));
        } catch (TwitterException e) {
            throw new RuntimeException("User discovery failed...", e);
        }

        return user;
    }

    /**
     * Scans user activity and imports data into the graph.
     *
     * @param user is the user that is being scanned.
     * @return the user that has been scanned, imported, and updated.
     */
    public User scanUserActivity(User user) {

        log.info(String.format("Scanning user activity for %s...", user.getScreenName()));
        List<Tweet> tweets = new ArrayList<>();
        List<Status> unfilteredTweets = new ArrayList<>();
        try {
            if (user.getLastImportedTweetId() == null) {
                log.info(String.format("Getting initial user tweets for %s...", user.getScreenName()));
                User finalUser2 = user;
                unfilteredTweets = Optional.of(twitter.timelines().getUserTimeline(user.getScreenName(),
                        new Paging(1, 20))).get();

                tweets = unfilteredTweets.stream()
                        .filter(t -> !t.isRetweet() && (!t.getText().startsWith("@")))
                        .map(t -> new Tweet(t.getId(), t.getText(), finalUser2.getProfileId(), t.getCreatedAt()))
                        .collect(Collectors.toList());
            } else {
                log.info(String.format("Getting subsequent user tweets for %s...", user.getScreenName()));
                User finalUser1 = user;
                unfilteredTweets = Optional.of(twitter.timelines().getUserTimeline(user.getScreenName(),
                        new Paging(1, 20,
                                Integer.MAX_VALUE, user.getLastImportedTweetId()))).get();

                tweets = unfilteredTweets.stream()
                        .filter(t -> !t.isRetweet() && (!t.getText().startsWith("@")))
                        .map(t -> new Tweet(t.getId(), t.getText(), finalUser1.getProfileId(), t.getCreatedAt()))
                        .collect(Collectors.toList());

            }
        } catch (Exception ex) {
            log.error("Error fetching timeline for user", ex);
        }

        List<Tweet> newTweets = new ArrayList<>();

        if (tweets.size() > 0) {
            try {
                log.info(String.format("Analyzing sentiment for %s tweets by %s", tweets.size(), user.getScreenName()));
                List<Tweet> finalTweets = tweets;
                User finalUser3 = user;
                IntStream.range(0, finalTweets.size()).forEach(i -> {
                    Tweet t = finalTweets.get(i);
                    Sentiment sentiment = null;
                    try {
                        sentiment = TextAnalysis.analyzeSentimentText(t.getText());
                    } catch (Exception ex) {
                        log.info(String.format("GCP sentiment analysis error: %s...", ex.getMessage()
                                .substring(0, Math.min(ex.getMessage().length() - 1, 150))));
                    }
                    if (sentiment != null) {
                        t.setSentiment(Float.valueOf(sentiment.getScore()).doubleValue());
                        t.setMagnitude(Float.valueOf(sentiment.getMagnitude()).doubleValue());
                    } else {
                        t.setSentiment(0.0);
                        t.setMagnitude(0.0);
                    }

                    log.info(String.format("%s of %s tweets analyzed for user %s...", i + 1, finalTweets.size(),
                            finalUser3.getScreenName()));
                });

                log.info(String.format("Saving %s analyzed tweets for user %s...", finalTweets.size(),
                        user.getScreenName()));

                newTweets = IterableUtils.toList(Optional.of(tweetRepository
                        .saveAll(new ArrayList<>(finalTweets))).orElse(finalTweets));

                User finalUser = user;

                log.info(String.format("Saving %s new tweet relationships for user %s...", newTweets.size(),
                        user.getScreenName()));

                tweetedRepository.saveTweetedRelationships(newTweets.stream()
                        .map(t -> new Tweeted(finalUser, t)).collect(Collectors.toSet()));

                user.setLastImportedTweetId(newTweets.stream()
                        .sorted(Comparator.comparing(Tweet::getTweetId))
                        .limit(1)
                        .map(Tweet::getTweetId)
                        .findFirst().orElse(null));
            } catch (Exception ex) {
                throw new RuntimeException("Error saving tweets to Neo4j", ex);
            }

            try {
                log.info(String.format("Analyzing extracted entities for %s tweets by %s...", newTweets.size(),
                        user.getScreenName()));
                // Entity name recognition using GCP NLP API and import connections to database
                List<HasEntity> entityList = newTweets.stream().flatMap(t -> {
                    List<Entity> entities = new ArrayList<>();
                    try {
                        entities = TextAnalysis.entitySentimentText(t.getText().toLowerCase());
                    } catch (Exception ex) {
                        log.info(String.format("GCP sentiment analysis error: %s...", ex.getMessage()
                                .substring(0, Math.min(ex.getMessage().length() - 1, 150))));
                    }
                    return entities.stream().map(e -> new HasEntity(t, new TextEntity(e.getName()),
                            Float.valueOf(e.getSalience()).doubleValue(),
                            Optional.of(e.getSentiment().getScore()).orElse(0f).doubleValue(),
                            Optional.of(e.getSentiment().getMagnitude()).orElse(0f).doubleValue()));
                }).collect(Collectors.toList());

                // Save all entities to database
                hasEntityRepository.saveHasEntityRelationships(new HashSet<>(entityList));
            } catch (Exception ex) {
                log.error("Error contacting the GCP NLP API to extract sentiment", ex);
            }
        }

        user.setLastImportedTweetId(unfilteredTweets.stream()
                .sorted(Comparator.comparing(Status::getId))
                .limit(1)
                .map(Status::getId)
                .findFirst().orElse(user.getLastImportedTweetId()));

        log.info(String.format("Activity scan completed for %s. %s tweets imported.", user.getScreenName(),
                newTweets.size()));
        user.setLastActivityScan(new Date().getTime());
        user = userRepository.save(user);

        return user;
    }

    /**
     * Submit a job to crawl this user only if their follows/follower counts are within limits
     *
     * @param user is the {@link User} that is to potentially be requested for crawling
     * @return the saved {@link User} with full profile information now updated on the Neo4j node
     */
    private User getUser(User user) {
        Long userId = userRepository.getUserIdByProfileId(user.getProfileId());

        if (userId != null) {
            user.setId(userId);
        }

        user = userRepository.save(user, 0);

        try {
            // Only crawl users that have manageable follows/follower counts
            if (user.getFollowerCount() < MAX_FOLLOWERS && user.getFollowsCount() < MAX_FOLLOWS) {
                log.info("Discover user scheduled on follows graph " + dateFormat.format(new Date()));
                user.setDiscoveredTime(new Date().getTime());

                // Update discovery time
                userRepository.save(user, 0);

                // Update the discovery chain
                userRepository.updateDiscoveryChain();

                rabbitTemplate.convertAndSend(QUEUE_NAME, objectMapper.writeValueAsString(user));
            } else {
                // Retry
                User nextUserToCrawl = userRepository.findNextUserToCrawl();

                if (nextUserToCrawl != null) {
                    this.discoverUserByProfileId(nextUserToCrawl.getProfileId());
                }
            }
        } catch (JsonProcessingException e) {
            log.error(e);
        }
        return user;
    }

    public void updateUserSentimentStatistics(Set<SentimentResult> sentimentResults) {

        Set<User> users = sentimentResults.stream().map(result -> {
            User user = new User();
            user.setProfileId(result.getUserProfileId());
            List<Double> sentiment = result.getSentiment();

            // Update statistics
            DoubleSummaryStatistics stats = DoubleStream.of(sentiment.stream()
                    .mapToDouble(Double::doubleValue).toArray())
                    .summaryStatistics();

            // Calculate cumulative sentiment
            user.setCumulativeSentiment(stats.getSum());

            // Calculate average sentiment
            user.setAverageSentiment(stats.getAverage());

            // Calculate standard deviation sentiment
            double stdDev = Statistics.standardDeviation(sentiment.stream()
                    .mapToDouble(d -> d).toArray());
            stdDev = Double.isNaN(stdDev) ? 0.0 : stdDev;
            user.setStdSentiment(stdDev);

            return user;
        }).collect(Collectors.toSet());

        log.info("Saving user sentiment statistics...");

        // Save updates for all users
        userRepository.updateUserStatistics(users);
    }


}
