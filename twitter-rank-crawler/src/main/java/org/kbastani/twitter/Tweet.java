package org.kbastani.twitter;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;

import java.io.Serializable;
import java.util.Date;

/**
 * This class represents an imported {@link org.springframework.social.twitter.api.Tweet} from the
 * Twitter API, as a domain class for a Neo4j node
 *
 * @author kbastani
 */
@NodeEntity
public class Tweet implements Serializable {

    @GraphId
    private Long id;

    private final Long tweetId;
    private final String text;
    private final Date createdAt;
    private final String fromUser;
    private final String profileImageUrl;
    private final Long userId;
    private final Long fromUserId;
    private final String languageCode;
    private final String source;

    public Tweet(org.springframework.social.twitter.api.Tweet tweet) {
        this.tweetId = tweet.getId();
        this.text = tweet.getText();
        this.createdAt = tweet.getCreatedAt();
        this.fromUser = tweet.getFromUser();
        this.profileImageUrl = tweet.getProfileImageUrl();
        this.userId = tweet.getToUserId();
        this.fromUserId = tweet.getFromUserId();
        this.languageCode = tweet.getLanguageCode();
        this.source = tweet.getSource();
    }

    public Long getId() {
        return id;
    }

    public Long getTweetId() {
        return tweetId;
    }

    public String getText() {
        return text;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public String getFromUser() {
        return fromUser;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public Long getUserId() {
        return userId;
    }

    public long getFromUserId() {
        return fromUserId;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        return "Tweet{" +
                "id=" + id +
                ", tweetId=" + tweetId +
                ", text='" + text + '\'' +
                ", createdAt=" + createdAt +
                ", fromUser='" + fromUser + '\'' +
                ", profileImageUrl='" + profileImageUrl + '\'' +
                ", userId=" + userId +
                ", fromUserId=" + fromUserId +
                ", languageCode='" + languageCode + '\'' +
                ", source='" + source + '\'' +
                '}';
    }
}
