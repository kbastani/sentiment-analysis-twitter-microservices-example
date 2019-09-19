package org.kbastani.user;

import org.springframework.data.neo4j.annotation.QueryResult;

import java.util.ArrayList;
import java.util.List;

@QueryResult
public class SentimentResult {
    private Long userProfileId;
    private List<Double> sentiment = new ArrayList<>();

    public SentimentResult() {
    }

    public Long getUserProfileId() {
        return userProfileId;
    }

    public void setUserProfileId(Long userProfileId) {
        this.userProfileId = userProfileId;
    }

    public List<Double> getSentiment() {
        return sentiment;
    }

    public void setSentiment(List<Double> sentiment) {
        this.sentiment = sentiment;
    }

    @Override
    public String toString() {
        return "SentimentResult{" +
                "userProfileId=" + userProfileId +
                ", sentiment=" + sentiment +
                '}';
    }
}
