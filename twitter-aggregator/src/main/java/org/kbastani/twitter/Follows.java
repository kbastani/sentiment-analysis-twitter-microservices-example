package org.kbastani.twitter;

import org.neo4j.ogm.annotation.*;

/**
 * This domain class is a Neo4j relationship indicating a directed follows connection between two users.
 *
 * @author kbastani
 */
@RelationshipEntity(type = "FOLLOWS")
public class Follows {

    @GraphId
    private Long relationshipId;
    @StartNode
    private User userA;
    @EndNode
    private User userB;

    public Follows(User userA, User userB) {
        this.userA = userA;
        this.userB = userB;
    }

    public Follows() {
    }

    public Long getRelationshipId() {
        return relationshipId;
    }

    public void setRelationshipId(Long relationshipId) {
        this.relationshipId = relationshipId;
    }

    public User getUserA() {
        return userA;
    }

    public void setUserA(User userA) {
        this.userA = userA;
    }

    public User getUserB() {
        return userB;
    }

    public void setUserB(User userB) {
        this.userB = userB;
    }

    @Override
    public String toString() {
        return "Follows{" +
                "relationshipId=" + relationshipId +
                ", userA=" + userA +
                ", userB=" + userB +
                '}';
    }
}
