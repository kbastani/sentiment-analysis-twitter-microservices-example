package org.kbastani.user;

import org.neo4j.ogm.annotation.*;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This is the {@link User} domain class that represents a Twitter profile as a Neo4j node.
 *
 * @author kbastani
 */
@NodeEntity
public class User implements Serializable {

    @Id
    @GeneratedValue
    private Long id;

    @Index(unique = true)
    private Long profileId;

    @Relationship(type = "FOLLOWS", direction = "OUTGOING")
    private Set<User> follows = new HashSet<>();

    @Relationship(type = "FOLLOWS", direction = "INCOMING")
    private Set<User> followers = new HashSet<>();

    private String screenName;
    private String name;
    private String url;
    private String profileImageUrl;
    private String description;
    private String location;
    private Date createdDate;
    private Integer followerCount;
    private Integer followsCount;
    private Float pagerank;
    private Integer previousRank;
    private Integer currentRank;
    private Float lastPageRank;
    private Boolean imported;
    private Long discoveredTime;
    private Integer discoveredRank;
    private Long lastActivityScan;
    private Long lastImportedTweetId;
    private Double averageSentiment;
    private Double stdSentiment;
    private Double cumulativeSentiment;

    public User() {
    }

    public User(Long id, Long profileId) {
        this.id = id;
        this.profileId = profileId;
    }

    public User(Long id, List<User> follows, List<User> followers) {
        this.profileId = id;
        this.follows.addAll(follows == null ? new HashSet<>() : follows);
        this.followers.addAll(followers == null ? new HashSet<>() : followers);
        this.follows = this.follows.stream().distinct().collect(Collectors.toSet());
    }

    public User(twitter4j.User twitterProfile) {
        this.profileId = twitterProfile.getId();
        this.createdDate = twitterProfile.getCreatedAt();
        this.screenName = twitterProfile.getScreenName();
        this.name = twitterProfile.getName();
        this.url = twitterProfile.getURL();
        this.description = twitterProfile.getDescription();
        this.location = twitterProfile.getLocation();
        this.profileImageUrl = twitterProfile.getProfileImageURL();
        this.followerCount = twitterProfile.getFollowersCount();
        this.followsCount = twitterProfile.getFriendsCount();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Set<User> getFollows() {
        return follows;
    }

    public void setFollows(Set<User> follows) {
        this.follows = follows;
    }

    public Set<User> getFollowers() {
        return followers;
    }

    public void setFollowers(Set<User> followers) {
        this.followers = followers;
    }

    public Integer getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(Integer followerCount) {
        this.followerCount = followerCount;
    }

    public Integer getFollowsCount() {
        return followsCount;
    }

    public void setFollowsCount(Integer followsCount) {
        this.followsCount = followsCount;
    }

    public Float getPagerank() {
        return pagerank;
    }

    public void setPagerank(Float pagerank) {
        this.pagerank = pagerank;
    }

    public Integer getPreviousRank() {
        return previousRank;
    }

    public void setPreviousRank(Integer previousRank) {
        this.previousRank = previousRank;
    }

    public Integer getCurrentRank() {
        return currentRank;
    }

    public void setCurrentRank(Integer currentRank) {
        this.currentRank = currentRank;
    }

    public Float getLastPageRank() {
        return lastPageRank;
    }

    public void setLastPageRank(Float lastPageRank) {
        this.lastPageRank = lastPageRank;
    }

    public Boolean getImported() {
        return imported;
    }

    public void setImported(Boolean imported) {
        this.imported = imported;
    }

    public Long getDiscoveredTime() {
        return discoveredTime;
    }

    public void setDiscoveredTime(Long discoveredTime) {
        this.discoveredTime = discoveredTime;
    }

    public Integer getDiscoveredRank() {
        return discoveredRank;
    }

    public void setDiscoveredRank(Integer discoveredRank) {
        this.discoveredRank = discoveredRank;
    }

    public Long getLastActivityScan() {
        return lastActivityScan;
    }

    public void setLastActivityScan(Long lastActivityScan) {
        this.lastActivityScan = lastActivityScan;
    }

    public Long getLastImportedTweetId() {
        return lastImportedTweetId;
    }

    public void setLastImportedTweetId(Long lastImportedTweetId) {
        this.lastImportedTweetId = lastImportedTweetId;
    }

    public Double getAverageSentiment() {
        return averageSentiment;
    }

    public void setAverageSentiment(Double averageSentiment) {
        this.averageSentiment = averageSentiment;
    }

    public Double getStdSentiment() {
        return stdSentiment;
    }

    public void setStdSentiment(Double stdSentiment) {
        this.stdSentiment = stdSentiment;
    }

    public Double getCumulativeSentiment() {
        return cumulativeSentiment;
    }

    public void setCumulativeSentiment(Double cumulativeSentiment) {
        this.cumulativeSentiment = cumulativeSentiment;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", profileId=" + profileId +
                ", follows=" + follows +
                ", followers=" + followers +
                ", screenName='" + screenName + '\'' +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", profileImageUrl='" + profileImageUrl + '\'' +
                ", description='" + description + '\'' +
                ", location='" + location + '\'' +
                ", createdDate=" + createdDate +
                ", followerCount=" + followerCount +
                ", followsCount=" + followsCount +
                ", pagerank=" + pagerank +
                ", previousRank=" + previousRank +
                ", currentRank=" + currentRank +
                ", lastPageRank=" + lastPageRank +
                ", imported=" + imported +
                ", discoveredTime=" + discoveredTime +
                ", discoveredRank=" + discoveredRank +
                ", lastActivityScan=" + lastActivityScan +
                ", lastImportedTweetId=" + lastImportedTweetId +
                ", averageSentiment=" + averageSentiment +
                ", stdSentiment=" + stdSentiment +
                ", cumulativeSentiment=" + cumulativeSentiment +
                '}';
    }
}
