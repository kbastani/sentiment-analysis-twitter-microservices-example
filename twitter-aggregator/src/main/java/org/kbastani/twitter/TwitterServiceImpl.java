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

@Service
public class TwitterServiceImpl implements TwitterService {

    private final Log log = LogFactory.getLog(TwitterService.class);
    private static final String QUEUE_NAME = "twitter.profiles";
    private final Twitter twitter;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public TwitterServiceImpl(Twitter twitter, UserRepository userRepository, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.twitter = twitter;
        this.userRepository = userRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public User discoverUserByScreenName(String screenName) {
        User user;

        user = Optional.of(twitter.userOperations().getUserProfile(screenName))
                .map(User::new)
                .get();

        user = getUser(user);

        return user;
    }

    public User discoverUserByProfileId(Long profileId) {
        User user;

        user = Optional.of(twitter.userOperations().getUserProfile(profileId))
                .map(User::new)
                .get();

        user = getUser(user);

        log.info(String.format("Discover user: %s", user.getScreenName()));

        return user;
    }

    private User getUser(User user) {
        User savedUser = userRepository.findUserByProfileId(user.getProfileId());

        if (savedUser != null) {
            user.setId(savedUser.getId());
        }

        user = userRepository.save(user, 1);

        try {
            // Only crawl users that have manageable follows/follower counts
            if(user.getFollowerCount() < 50000 && user.getFollowsCount() < 50000) {
                rabbitTemplate.convertAndSend(QUEUE_NAME, objectMapper.writeValueAsString(user));
            } else {
                // Retry
                User nextUserToCrawl = userRepository.findNextUserToCrawl();

                if(nextUserToCrawl != null) {
                    this.discoverUserByProfileId(nextUserToCrawl.getProfileId());
                }
            }
        } catch (JsonProcessingException e) {
            log.error(e);
        }
        return user;
    }
}
