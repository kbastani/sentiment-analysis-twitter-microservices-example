package org.kbastani.tweet;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import java.util.Set;

/**
 * The {@link Tweeted} repository provides custom Cypher queries as repository search operations
 *
 * @author kbastani
 */
public interface TweetedRepository extends Neo4jRepository<Tweeted, Long> {

    /**
     * Efficiently batches the creation of many TWEETED relationships
     *
     * @param tweeted a set of relationship entities containing a user who created a tweet
     */
    @Query("FOREACH(x in {tweeted} | MERGE (a:User { profileId: x.user.profileId })\n" +
            "MERGE (b:Tweet { profileId: x.tweet.profileId, tweetId: x.tweet.tweetId })\n" +
            "MERGE (a)-[:TWEETED]->(b))")
    void saveTweetedRelationships(@Param("tweeted") Set<Tweeted> tweeted);
}