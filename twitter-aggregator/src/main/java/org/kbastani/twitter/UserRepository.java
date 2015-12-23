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

    @Query("FOREACH(x in {users} | MERGE (a:User { profileId: x.userA.profileId })\n" +
            "MERGE (b:User { profileId: x.userB.profileId })\n" +
            "MERGE (a)-[:FOLLOWS]->(b))")
    Set<User> saveUsers(@Param("users") Set<User> users);

    @Query("MATCH (user:User) WHERE has(user.pagerank)\n" +
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
}
