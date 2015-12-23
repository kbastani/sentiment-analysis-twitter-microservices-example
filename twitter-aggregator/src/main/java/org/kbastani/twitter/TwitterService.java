package org.kbastani.twitter;

/**
 * This service is a contract that provides user discovery based on either a user's screen name
 * or the profile's ID from the Twitter API.
 *
 * @author kbastani
 */
public interface TwitterService {
    User discoverUserByScreenName(String screenName);
    User discoverUserByProfileId(Long profileId);
}
