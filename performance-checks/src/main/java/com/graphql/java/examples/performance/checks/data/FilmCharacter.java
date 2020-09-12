package com.graphql.java.examples.performance.checks.data;

import java.util.List;

/**
 * A character from the movie Star Wars
 */
public class FilmCharacter {
    final String id;
    final String name;
    final List<String> friends;

    public FilmCharacter(String id, String name, List<String> friends) {
        this.id = id;
        this.name = name;
        this.friends = friends;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getFriends() {
        return friends;
    }
}
