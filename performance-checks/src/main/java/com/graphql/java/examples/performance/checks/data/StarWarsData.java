package com.graphql.java.examples.performance.checks.data;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * A fake, static, "database" containing data about a few Star Wars characters
 */
@SuppressWarnings("unused")
public class StarWarsData {
    static FilmCharacter luke = new FilmCharacter(
            "1000",
            "Luke Skywalker",
            asList("1002", "1003")
    );

    static FilmCharacter vader = new FilmCharacter(
            "1001",
            "Darth Vader",
            asList("1004")
    );

    static FilmCharacter han = new FilmCharacter(
            "1002",
            "Han Solo",
            asList("1000", "1003")
    );

    static FilmCharacter leia = new FilmCharacter(
            "1003",
            "Leia Organa",
            asList("1000", "1002")
    );

    static FilmCharacter tarkin = new FilmCharacter(
            "1004",
            "Wilhuff Tarkin",
            asList("1001")
    );

    static Map<String, FilmCharacter> characterData = new LinkedHashMap<>();

    static {
        characterData.put("1000", luke);
        characterData.put("1001", vader);
        characterData.put("1002", han);
        characterData.put("1003", leia);
        characterData.put("1004", tarkin);
    }

    public static boolean isFilmCharacter(String id) {
        return characterData.get(id) != null;
    }

    public static Collection<FilmCharacter> getAllCharacters() {
        return characterData.values();
    }

    public static FilmCharacter getCharacter(String id) {
        if (characterData.get(id) != null) {
            return characterData.get(id);
        } else if (characterData.get(id) != null) {
            return characterData.get(id);
        }
        return null;
    }
}
