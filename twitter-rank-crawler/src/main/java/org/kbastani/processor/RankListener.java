package org.kbastani.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kbastani.tweet.TwitterService;
import org.kbastani.user.Follows;
import org.kbastani.user.FollowsRepository;
import org.kbastani.user.User;
import org.kbastani.user.UserRepository;
import org.springframework.amqp.AmqpIllegalStateException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import twitter4j.IDs;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;


/**
 * This class is the crawler that receives messages from three different queues and performs
 * work serially to import the graph of users received from the Twitter API. If the rate limit
 * of the Twitter API is succeeded, the message will be re-inserted back into the queue and
 * the operation will retry once the rate limit has been reset.
 *
 * @author kbastani
 */
@Service
public class RankListener {

    private final Log log = LogFactory.getLog(RankListener.class);
    private final ObjectMapper objectMapper;
    private final AmqpTemplate amqpTemplate;
    private final Twitter twitter;
    private final FollowsRepository followsRepository;
    private final TwitterService twitterService;
    private final UserRepository userRepository;

    @Autowired
    public RankListener(ObjectMapper objectMapper, AmqpTemplate amqpTemplate, Twitter twitter,
                        FollowsRepository followsRepository, TwitterService twitterService,
                        UserRepository userRepository) {
        this.objectMapper = objectMapper;
        this.amqpTemplate = amqpTemplate;
        this.twitter = twitter;
        this.followsRepository = followsRepository;
        this.twitterService = twitterService;
        this.userRepository = userRepository;
    }

    /**
     * Receives a message containing a user profile that should have their followers imported into Neo4j.
     * On successful completion, a message is sent to the next queue to import the users that this profile
     * follows.
     *
     * @param message is the message containing information about the user profile
     */
    @RabbitListener(queues = {"twitter.followers"})
    public void followers(String message) throws InterruptedException, IOException {

        User user = objectMapper.readValue(message, User.class);

        log.info(String.format("Import follower network message received for %s...", user.getScreenName()));
        try {
            // Iterate through cursors and import to graph database
            IDs followers = twitter.getFollowersIDs(user.getProfileId(), -1L);

            saveFollowers(user, followers);

            while (followers.hasNext()) {
                long cursor = followers.getNextCursor();
                followers = twitter.getFollowersIDs(user.getProfileId(), cursor);
                saveFollowers(user, followers);
            }

            log.info(String.format("%s followers imported for user: %s", user.getFollowerCount(),
                    user.getScreenName()));

            amqpTemplate.convertAndSend("twitter.follows", objectMapper.writeValueAsString(user));
        } catch (Exception rateLimitException) {
            RankProcessor.resetTimer = true;
            Thread.sleep(40000L);
            log.info(String.format("Rate limit exceeded while importing followers for user: %s",
                    user.getScreenName()));

            // Throw AMQP exception and redeliver the message
            throw new AmqpIllegalStateException(rateLimitException.getMessage());
        }
    }

    /**
     * Saves a cursored list of followers from the Twitter API in batches to Neo4j
     *
     * @param user      is the {@link User} that is the owner of the relationships being imported
     * @param followers are the profiles that the @{link User} is being followed by
     */
    private void saveFollowers(User user, IDs followers) {
        final User finalUser = user;

        List<User> users = LongStream.of(followers.getIDs()).mapToObj(a -> new User(a,
                new ArrayList<User>(Collections.singletonList(new User(finalUser.getId(), finalUser.getProfileId()))),
                null))
                .collect(Collectors.toList());

        Integer pointer = 0;
        Integer batchSize = 400;
        Integer retryCount = 0;

        while ((batchSize * pointer) < users.size()) {
            try {
                followsRepository.saveFollows(users.subList((batchSize * pointer),
                        Math.min(((batchSize * pointer) + batchSize), users.size())).stream()
                        .map(follower -> new Follows(follower, user)).collect(Collectors.toSet()));
                pointer++;
            } catch (Exception ex) {
                if (retryCount <= 4) {
                    // retry
                    retryCount++;
                } else {
                    throw ex;
                }
            }
        }
    }

    /**
     * Receives a message containing a user profile that should have the users they follow imported into Neo4j
     *
     * @param message is the message containing information about the user profile
     */
    @RabbitListener(queues = {"twitter.follows"})
    public void follows(String message) throws InterruptedException {
        User user = null;

        try {
            user = objectMapper.readValue(message, User.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            // Iterate through cursors and import to graph database
            if (user != null) {

                IDs follows = twitter.getFriendsIDs(user.getProfileId(), -1);

                saveFollows(user, follows);

                while (follows.hasNext()) {
                    long cursor = follows.getNextCursor();
                    follows = twitter.getFriendsIDs(user.getProfileId(), cursor);
                    saveFollows(user, follows);
                }

                log.info(String.format("%s friends imported for user: %s", user.getFollowsCount(),
                        user.getScreenName()));

                // Prepares the user to be ranked on the leader board
                prepareUserForRanking(user);

                // Reset the timer
                RankProcessor.resetTimer = true;

                // Queue next user
                User nextUser = userRepository.findRankedUserToCrawl();

                if (nextUser == null) {
                    nextUser = userRepository.findNextUserToCrawl();
                }

                if (nextUser != null) {
                    twitterService.discoverUserByProfileId(nextUser.getProfileId());
                }
            }
        } catch (TwitterException rateLimitException) {
            RankProcessor.resetTimer = true;
            Thread.sleep(40000L);

            log.info(String.format("Rate limit exceeded while importing friends for user: %s", user.getScreenName()));

            // Throw AMQP exception and redeliver the message
            throw new AmqpIllegalStateException(rateLimitException.getMessage());
        } catch (Exception ex) {
            log.info(ex);
        }
    }

    /**
     * Prepares a user to be ranked for the first time after having follower
     * data imported, resetting the previous PageRanks and preparing for a new
     * calculation and ranking.
     *
     * @param user is the user that should be prepared for being ranked
     */
    private void prepareUserForRanking(User user) {
        // Update user's imported flag and reset the pagerank fields
        User updatedUser = userRepository.findById(user.getId()).orElse(null);

        if (updatedUser != null && (updatedUser.getImported() == null || !updatedUser.getImported())) {
            updatedUser.setImported(true);
            updatedUser.setPagerank(null);
            updatedUser.setLastPageRank(null);
            updatedUser.setCurrentRank(null);
            updatedUser.setPreviousRank(null);
            userRepository.save(updatedUser, 0);
        }
    }

    /**
     * Saves a cursored list of followers from the Twitter API in batches to Neo4j
     *
     * @param user    is the {@link User} that is the owner of the relationships being imported
     * @param follows are the profiles that the @{link User} follows
     */
    private void saveFollows(User user, IDs follows) {
        final User finalUser = user;

        List<User> users = LongStream.of(follows.getIDs()).mapToObj(a -> new User(a,
                null, new ArrayList<User>(Collections.singletonList(new User(finalUser.getId(),
                finalUser.getProfileId())))))
                .collect(Collectors.toList());

        Integer pointer = 0;
        Integer batchSize = 400;
        int retryCount = 0;

        while ((batchSize * pointer) < users.size()) {
            try {
                followsRepository.saveFollows(users.subList((batchSize * pointer),
                        Math.min(((batchSize * pointer) + batchSize), users.size())).stream()
                        .map(follower -> new Follows(user, follower)).collect(Collectors.toSet()));
                pointer++;
            } catch (Exception ex) {
                if (retryCount <= 3) {
                    // retry
                    retryCount++;
                } else {
                    throw ex;
                }
            }
        }
    }

}
