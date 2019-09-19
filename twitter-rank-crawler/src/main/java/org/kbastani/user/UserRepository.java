package org.kbastani.user;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * The {@link User} repository provides custom Cypher queries as repository search operations
 *
 * @author kbastani
 */
public interface UserRepository extends Neo4jRepository<User, Long> {

    User findUserByScreenName(String screenName);

    User findUserByProfileId(Long profileId);

    @Query("MATCH (user:User { profileId: {profileId} }) RETURN id(user) as id")
    Long getUserIdByProfileId(@Param("profileId") Long profileId);

    @Query("MATCH (user:User) WHERE exists(user.pagerank) AND exists(user.screenName) AND coalesce(user.imported, false) = true\n" +
            "WITH user\n" +
            "ORDER BY user.pagerank DESC\n" +
            "SKIP {skip}\n" +
            "LIMIT {limit}\n" +
            "RETURN user")
    Set<User> findRankedUsers(@Param("skip") Integer skip, @Param("limit") Integer limit);

    @Query("MATCH (user:User)-[:TWEETED]->(tweet:Tweet),\n" +
            "\t(tweet)-[entity:HAS_ENTITY]->(:TextEntity)\n" +
            "WHERE (user)-[:NEXT]-()\n" +
            "RETURN user.profileId as userProfileId, COLLECT(TOFLOAT(entity.sentiment)) as sentiment")
    Set<SentimentResult> findUserSentimentResults();

    @Query("MATCH (user:User)-[:NEXT]-(), (user)-[:TWEETED]->()-[r:HAS_ENTITY]->(e:TextEntity)\n" +
            "WHERE user.profileId = {profileId}\n" +
            "RETURN user.profileId as userProfileId, COLLECT(r.sentiment) as sentiment")
    SentimentResult getUserSentiment(@Param("profileId") Long profileId);

    @Query("FOREACH(x in {users} | MERGE (user:User { profileId: x.profileId })\n" +
            "SET user.averageSentiment = x.averageSentiment\n" +
            "SET user.stdSentiment = x.stdSentiment\n" +
            "SET user.cumulativeSentiment = x.cumulativeSentiment)")
    void updateUserStatistics(@Param("users") Set<User> users);

    @Query("MATCH (a:User)<-[r:FOLLOWS]-(b)\n" +
            "WHERE NOT exists(a.screenName)\n" +
            "WITH a, count(r) as weight\n" +
            "WHERE weight > 2\n" +
            "WITH a, weight\n" +
            "ORDER BY weight DESC\n" +
            "LIMIT 1\n" +
            "WITH a\n" +
            "RETURN a")
    User findNextUserToCrawl();

    @Query("MATCH (user:User) WHERE exists(user.pagerank) AND NOT exists(user.screenName)\n" +
            "WITH user\n" +
            "ORDER BY user.pagerank DESC\n" +
            "LIMIT 1\n" +
            "RETURN user")
    User findRankedUserToCrawl();

    @Query("MATCH (user:User) WHERE exists(user.pagerank) AND exists(user.screenName) AND (user)-[:NEXT]-()\n" +
            "WITH user\n" +
            "ORDER BY coalesce(user.lastActivityScan, 0)\n" +
            "LIMIT 1\n" +
            "RETURN user")
    User findNextUserActivityScan();

    /**
     * Initialize the user.lastPageRank value to the current user.pagerank
     */
    @Query("MATCH (user:User) WHERE exists(user.pagerank) AND exists(user.screenName) AND NOT exists(user.lastPageRank)\n" +
            "WITH collect(user) as users\n" +
            "FOREACH(x in users | \n" +
            "SET x.lastPageRank = toFloat(x.pagerank))")
    void setLastPageRank();

    /**
     * Updates the current rank and last rank of each ranked user. This query allows us to see how much a
     * user's rank has increased or decreased from the last PageRank job.
     */
    @Query("MATCH (user:User) WHERE exists(user.pagerank) AND exists(user.screenName) AND exists(user.lastPageRank)\n" +
            "WITH user\n" +
            "ORDER BY user.pagerank DESC\n" +
            "WITH collect(user) as users\n" +
            "UNWIND range(0,size(users)-1) AS idx\n" +
            "WITH users[idx] AS user, 1 + idx AS currentRank\n" +
            "WITH user, coalesce(user.currentRank, 0) as previousRank, currentRank\n" +
            "WITH collect({ user: user, previousRank: previousRank, currentRank: currentRank }) as results\n" +
            "FOREACH (x in [y IN results WHERE y.user.pagerank <> y.user.lastPageRank | y] | \n" +
            "\tSET x.user.previousRank = x.previousRank\n" +
            "\tSET x.user.currentRank = x.currentRank\n" +
            "\tSET x.user.lastPageRank = x.user.pagerank)")
    void updateUserCurrentRank();


    /**
     * Updates a linked list of users in the order that they are discovered.
     */
    @Transactional
    @Query("MATCH (user:User) WHERE exists(user.discoveredTime)\n" +
            "WITH user ORDER BY user.discoveredTime\n" +
            "WITH collect(user) as users\n" +
            "UNWIND range(0,size(users)-2) as idx\n" +
            "WITH users[idx] AS s1, users[idx+1] AS s2, idx as n1\n" +
            "MERGE (s1)-[:NEXT]->(s2)\n" +
            "SET s1.discoveredRank = n1 + 1\n" +
            "SET s2.discoveredRank = n1 + 2")
    void updateDiscoveryChain();

    @Query("MATCH (u:User)\n" +
            "WITH collect(u) as nodes\n" +
            "CALL apoc.algo.pageRankWithConfig(nodes,{iterations:10,types:\"FOLLOWS\"}) YIELD node, score\n" +
            "WITH node, score\n" +
            "SET node.pagerank = score")
    void updatePageRankForFollowGraph();
}