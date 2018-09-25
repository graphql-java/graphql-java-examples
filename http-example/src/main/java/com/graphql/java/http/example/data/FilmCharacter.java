package com.graphql.java.http.example.data;

import java.util.List;

public interface FilmCharacter {
    String getId();

    String getName();

    List<String> getFriends();

    List<Integer> getAppearsIn();
}
