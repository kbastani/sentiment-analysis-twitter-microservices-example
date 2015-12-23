package org.kbastani.twitter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * This class implements the service contract for {@link TwitterService} and
 * is responsible for discovering users by screen name or profile ID.
 *
 * @author kbastani
 */
@Service
public class TwitterServiceImpl implements TwitterService {

    private final Log log = LogFactory.getLog(TwitterService.class);
    private static final String QUEUE_NAME = "twitter.profiles";
    private final Twitter twitter;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    // These two fields are constants that target users below follows/following thresholds
    private final Integer MAX_FOLLOWS = 50000;
    private final Integer MAX_FOLLOWERS = 50000;

    @Autowired
    public TwitterServiceImpl(Twitter twitter, UserRepository userRepository, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.twitter = twitter;
        this.userRepository = userRepository;
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

        user = Optional.of(twitter.userOperations().getUserProfile(screenName))
                .map(User::new)
                .get();

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

        user = Optional.of(twitter.userOperations().getUserProfile(profileId))
                .map(User::new)
                .get();

        user = getUser(user);

        log.info(String.format("Discover user: %s", user.getScreenName()));

        return user;
    }

    /**
     * Submit a job to crawl this user only if their follows/follower counts are within limits
     *
     * @param user is the {@link User} that is to potentially be requested for crawling
     * @return the saved {@link User} with full profile information now updated on the Neo4j node
     */
    private User getUser(User user) {
        User savedUser = userRepository.findUserByProfileId(user.getProfileId());

        if (savedUser != null) {
            user.setId(savedUser.getId());
        }

        user = userRepository.save(user, 1);

        try {
            // Only crawl users that have manageable follows/follower counts
            if (user.getFollowerCount() < MAX_FOLLOWERS && user.getFollowsCount() < MAX_FOLLOWS) {
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
}
