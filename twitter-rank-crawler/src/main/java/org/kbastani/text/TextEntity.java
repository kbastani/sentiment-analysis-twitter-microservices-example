package org.kbastani.text;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
public class TextEntity {

    @Id
    @GeneratedValue
    private Long id;
    private String name;

    public TextEntity() {
    }

    public TextEntity(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "TextEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
