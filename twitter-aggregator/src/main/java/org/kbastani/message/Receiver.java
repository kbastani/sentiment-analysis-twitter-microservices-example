package org.kbastani.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kbastani.scheduling.AnalyticsScheduler;
import org.kbastani.twitter.*;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.RateLimitExceededException;
import org.springframework.social.twitter.api.CursoredList;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Component
public class Receiver {

    private final Log log = LogFactory.getLog(Receiver.class);
    private final ObjectMapper objectMapper;
    private final AmqpTemplate ampqTemplate;
    private final Twitter twitter;
    private final FollowsRepository followsRepository;
    private final TwitterService twitterService;
    private final UserRepository userRepository;

    @Autowired
    public Receiver(ObjectMapper objectMapper, AmqpTemplate ampqTemplate, Twitter twitter, FollowsRepository followsRepository, TwitterService twitterService, UserRepository userRepository) {
        this.objectMapper = objectMapper;
        this.ampqTemplate = ampqTemplate;
        this.twitter = twitter;
        this.followsRepository = followsRepository;
        this.twitterService = twitterService;
        this.userRepository = userRepository;
    }

    @RabbitListener(queues = {"twitter.profiles"})
    public void receiveMessage(String message) {
        User user = null;

        try {
            user = objectMapper.readValue(message, User.class);

            // Add messages for followers and follows
            ampqTemplate.convertAndSend("twitter.followers", objectMapper.writeValueAsString(user));

        } catch (IOException e) {
            log.error(e);
        }

        log.info(user);
    }

    @RabbitListener(queues = {"twitter.followers"})
    public void followers(String message) throws InterruptedException {
        User user = null;

        try {
            user = objectMapper.readValue(message, User.class);
        } catch (IOException e) {
            log.error(e);
        }

        if (user != null) {
            try {
                // Iterate through cursors and import to graph database
                CursoredList<Long> followers = twitter.friendOperations().getFollowerIds(user.getProfileId());

                saveFollowers(user, followers);

                while (followers.hasNext()) {
                    Long cursor = followers.getNextCursor();
                    followers = twitter.friendOperations().getFollowerIdsInCursor(user.getProfileId(), cursor);
                    saveFollowers(user, followers);
                }

                ampqTemplate.convertAndSend("twitter.follows", objectMapper.writeValueAsString(user));
            } catch (RateLimitExceededException rateLimitException) {
                AnalyticsScheduler.resetTimer = true;
                Thread.sleep(40000L);
                ampqTemplate.convertAndSend("twitter.followers", message);
            } catch (Exception ex) {
                log.info(ex);
            }
        }


        log.info(user);
    }

    private void saveFollowers(User user, CursoredList<Long> followers) {
        final User finalUser = user;

        List<User> users = followers.stream().map(a -> new User(a,
                Collections.singletonList(new User(finalUser.getId(), finalUser.getProfileId())).stream().collect(Collectors.toList()), null))
                .collect(Collectors.toList());

        Integer pointer = 0;
        Integer batchSize = 400;
        Integer retryCount = 0;

        while ((batchSize * pointer) < users.size()) {
            try {
                followsRepository.saveFollows(users.subList((batchSize * pointer), ((batchSize * pointer) + batchSize) < users.size() ? ((batchSize * pointer) + batchSize) : users.size()).stream()
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
                CursoredList<Long> follows = twitter.friendOperations().getFriendIds(user.getProfileId());

                saveFollows(user, follows);

                while (follows.hasNext()) {
                    Long cursor = follows.getNextCursor();
                    follows = twitter.friendOperations().getFriendIdsInCursor(user.getProfileId(), cursor);
                    saveFollows(user, follows);
                }

                log.info(user);

                // Reset the timer
                AnalyticsScheduler.resetTimer = true;

                // Queue next user
                User nextUser = userRepository.findRankedUserToCrawl();

                if(nextUser == null) {
                    nextUser = userRepository.findNextUserToCrawl();
                }

                if (nextUser != null) {
                    twitterService.discoverUserByProfileId(nextUser.getProfileId());
                }
            }
        } catch (RateLimitExceededException rateLimitException) {
            AnalyticsScheduler.resetTimer = true;
            Thread.sleep(40000L);
            ampqTemplate.convertAndSend("twitter.follows", message);
        } catch (Exception ex) {
            log.info(ex);
        }
    }

    private void saveFollows(User user, CursoredList<Long> followers) {
        final User finalUser = user;

        List<User> users = followers.stream().map(a -> new User(a,
                null, Collections.singletonList(new User(finalUser.getId(), finalUser.getProfileId())).stream().collect(Collectors.toList())))
                .collect(Collectors.toList());

        Integer pointer = 0;
        Integer batchSize = 400;
        Integer retryCount = 0;

        while ((batchSize * pointer) < users.size()) {
            try {
                followsRepository.saveFollows(users.subList((batchSize * pointer), ((batchSize * pointer) + batchSize) < users.size() ? ((batchSize * pointer) + batchSize) : users.size()).stream()
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
