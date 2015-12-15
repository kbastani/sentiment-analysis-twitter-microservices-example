package org.kbastani.twitter;

public interface TwitterService {
    User discoverUserByScreenName(String screenName);

    User discoverUserByProfileId(Long profileId);
}
