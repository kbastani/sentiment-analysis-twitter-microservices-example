package org.kbastani.tweet;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

import java.util.Date;

@NodeEntity
public class Tweet {

    @Id
    @GeneratedValue
    private Long id;
    private Long tweetId;
    private String text;
    private Boolean analyzed;
    private Long profileId;
    private Date createdDate;
    private Integer createdHour;
    private Integer createdDay;
    private Integer createdMonth;
    private Integer createdYear;
    private Long createdTimestamp;
    private Double sentiment;
    private Double magnitude;

    public Tweet() {
    }

    public Tweet(Long tweetId, String text) {
        this.tweetId = tweetId;
        this.text = text;
    }

    public Tweet(Long tweetId, String text, Long profileId) {
        this.tweetId = tweetId;
        this.text = text;
        this.profileId = profileId;
    }

    public Tweet(Long tweetId, String text, Long profileId, Date createdDate) {
        this.tweetId = tweetId;
        this.text = text;
        this.profileId = profileId;
        this.createdDate = createdDate;

        // Index date/time
        this.setCreatedHour(createdDate.getHours());
        this.setCreatedDay(createdDate.getDate());
        this.setCreatedMonth(createdDate.getMonth());
        this.setCreatedYear(createdDate.getYear());
        this.setCreatedTimestamp(createdDate.getTime());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTweetId() {
        return tweetId;
    }

    public void setTweetId(Long tweetId) {
        this.tweetId = tweetId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Boolean getAnalyzed() {
        return analyzed;
    }

    public void setAnalyzed(Boolean analyzed) {
        this.analyzed = analyzed;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Integer getCreatedDay() {
        return createdDay;
    }

    public void setCreatedDay(Integer createdDay) {
        this.createdDay = createdDay;
    }

    public Integer getCreatedMonth() {
        return createdMonth;
    }

    public void setCreatedMonth(Integer createdMonth) {
        this.createdMonth = createdMonth;
    }

    public Integer getCreatedYear() {
        return createdYear;
    }

    public void setCreatedYear(Integer createdYear) {
        this.createdYear = createdYear;
    }

    public Long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public Integer getCreatedHour() {
        return createdHour;
    }

    public void setCreatedHour(Integer createdHour) {
        this.createdHour = createdHour;
    }

    public Double getSentiment() {
        return sentiment;
    }

    public void setSentiment(Double sentiment) {
        this.sentiment = sentiment;
    }

    public Double getMagnitude() {
        return magnitude;
    }

    public void setMagnitude(Double magnitude) {
        this.magnitude = magnitude;
    }

    @Override
    public String toString() {
        return "Tweet{" +
                "id=" + id +
                ", tweetId=" + tweetId +
                ", text='" + text + '\'' +
                ", analyzed=" + analyzed +
                ", profileId=" + profileId +
                ", createdDate=" + createdDate +
                ", createdHour=" + createdHour +
                ", createdDay=" + createdDay +
                ", createdMonth=" + createdMonth +
                ", createdYear=" + createdYear +
                ", createdTimestamp=" + createdTimestamp +
                ", sentiment=" + sentiment +
                ", magnitude=" + magnitude +
                '}';
    }
}
