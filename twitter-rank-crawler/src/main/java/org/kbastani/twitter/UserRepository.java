package org.kbastani.twitter;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;

import java.util.Set;

/**
 * The {@link User} repository provides custom Cypher queries as repository search operations
 *
 * @author kbastani
 */
public interface UserRepository extends GraphRepository<User> {

    User findUserByScreenName(String screenName);

    User findUserByProfileId(Long profileId);

    @Query("MATCH (user:User { profileId: {profileId} }) RETURN id(user) as id")
    Long getUserIdByProfileId(@Param("profileId") Long profileId);

    @Query("MATCH (user:User) WHERE has(user.pagerank) AND has(user.screenName) AND coalesce(user.imported, false) = true\n" +
            "WITH user\n" +
            "ORDER BY user.pagerank DESC\n" +
            "SKIP {skip}\n" +
            "LIMIT {limit}\n" +
            "RETURN user")
    Set<User> findRankedUsers(@Param("skip") Integer skip, @Param("limit") Integer limit);

    @Query("MATCH (a:User)<-[r:FOLLOWS]-(b)\n" +
            "WHERE NOT has(a.screenName)\n" +
            "WITH a, count(r) as weight\n" +
            "WHERE weight > 2\n" +
            "WITH a, weight\n" +
            "ORDER BY weight DESC\n" +
            "LIMIT 1\n" +
            "WITH a\n" +
            "RETURN a")
    User findNextUserToCrawl();

    @Query("MATCH (user:User) WHERE has(user.pagerank) AND NOT has(user.screenName)\n" +
            "WITH user\n" +
            "ORDER BY user.pagerank DESC\n" +
            "LIMIT 1\n" +
            "RETURN user")
    User findRankedUserToCrawl();

    /**
     * Initialize the user.lastPageRank value to the current user.pagerank
     */
    @Query("MATCH (user:User) WHERE has(user.pagerank) AND has(user.screenName) AND NOT has(user.lastPageRank)\n" +
            "WITH collect(user) as users\n" +
            "FOREACH(x in users | \n" +
            "SET x.lastPageRank = toFloat(x.pagerank))")
    void setLastPageRank();

    /**
     * Updates the current rank and last rank of each ranked user. This query allows us to see how much a
     * user's rank has increased or decreased from the last PageRank job.
     */
    @Query("MATCH (user:User) WHERE has(user.pagerank) AND has(user.screenName) AND has(user.lastPageRank) AND user.imported = true\n" +
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
    @Query("MATCH (user:User) WHERE has(user.discoveredTime)\n" +
            "WITH user ORDER BY user.discoveredTime\n" +
            "WITH collect(user) as users\n" +
            "UNWIND range(0,size(users)-2) as idx\n" +
            "WITH users[idx] AS s1, users[idx+1] AS s2, idx as n1\n" +
            "MERGE (s1)-[:NEXT]->(s2)\n" +
            "SET s1.discoveredRank = n1 + 1\n" +
            "SET s2.discoveredRank = n1 + 2")
    void updateDiscoveryChain();
}