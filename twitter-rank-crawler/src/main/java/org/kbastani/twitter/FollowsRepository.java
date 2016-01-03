package org.kbastani.twitter;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Set;

/**
 * This repository provides access to create efficient batched relationships between users using Spring Data Neo4j
 */
@RepositoryRestResource(collectionResourceRel = "following", itemResourceRel = "following", path = "following")
public interface FollowsRepository extends GraphRepository<Follows> {

    /**
     * Efficiently batches the creation of many FOLLOWS relationships
     * between {@link User} nodes
     *
     * @param follows a set of relationship entities containing a user "A" who follows a user "B"
     */
    @Query("FOREACH(x in {follows} | MERGE (a:User { profileId: x.userA.profileId })\n" +
            "MERGE (b:User { profileId: x.userB.profileId })\n" +
            "MERGE (a)-[:FOLLOWS]->(b))")
    void saveFollows(@Param("follows") Set<Follows> follows);
}
