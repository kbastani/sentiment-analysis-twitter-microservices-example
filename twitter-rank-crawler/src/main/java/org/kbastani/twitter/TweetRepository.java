package org.kbastani.twitter;

import org.springframework.data.neo4j.repository.GraphRepository;

public interface TweetRepository extends GraphRepository<Tweet> {
}
