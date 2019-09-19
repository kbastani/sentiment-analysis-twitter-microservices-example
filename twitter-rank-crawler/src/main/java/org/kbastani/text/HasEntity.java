package org.kbastani.text;

import org.kbastani.tweet.Tweet;
import org.neo4j.ogm.annotation.*;

/**
 * This domain class represents a relationship between a {@link Tweet} and a {@link TextEntity}.
 *
 * @author kbastani
 */
@RelationshipEntity(type = "HAS_ENTITY")
public class HasEntity {

    @Id
    @GeneratedValue
    private Long id;

    @StartNode
    private Tweet tweet;
    @EndNode
    private TextEntity textEntity;
    private Double salience;
    private Double sentiment;
    private Double magnitude;

    public HasEntity() {
    }

    public HasEntity(Tweet tweet, TextEntity textEntity) {
        this.tweet = tweet;
        this.textEntity = textEntity;
    }

    public HasEntity(Tweet tweet, TextEntity textEntity, Double salience) {
        this.tweet = tweet;
        this.textEntity = textEntity;
        this.salience = salience;
    }

    public HasEntity(Tweet tweet, TextEntity textEntity, Double salience, Double sentiment, Double magnitude) {
        this.tweet = tweet;
        this.textEntity = textEntity;
        this.salience = salience;
        this.sentiment = sentiment;
        this.magnitude = magnitude;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Tweet getTweet() {
        return tweet;
    }

    public void setTweet(Tweet tweet) {
        this.tweet = tweet;
    }

    public TextEntity getTextEntity() {
        return textEntity;
    }

    public void setTextEntity(TextEntity textEntity) {
        this.textEntity = textEntity;
    }

    public Double getSalience() {
        return salience;
    }

    public void setSalience(Double salience) {
        this.salience = salience;
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
        return "HasEntity{" +
                "id=" + id +
                ", tweet=" + tweet +
                ", textEntity=" + textEntity +
                ", salience=" + salience +
                '}';
    }
}
