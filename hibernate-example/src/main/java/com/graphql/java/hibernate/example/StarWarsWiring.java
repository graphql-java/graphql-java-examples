package com.graphql.java.hibernate.example;

import com.graphql.java.hibernate.example.data.model.Droid;
import com.graphql.java.hibernate.example.data.model.Episode;
import com.graphql.java.hibernate.example.data.model.FilmCharacter;
import com.graphql.java.hibernate.example.data.model.Human;
import com.graphql.java.hibernate.example.data.repository.CharacterRepository;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;
import graphql.schema.idl.EnumValuesProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is our wiring used to put fetching behaviour behind a graphql field.
 *
 * Data fetchers declared here use {@link CharacterRepository} to query data from the database.
 */
@Component
public class StarWarsWiring {
    /**
     * The autowired {@link CharacterRepository}, that will be used by the data fetcher to access the database.
     */
    private final CharacterRepository characterRepository;

    @Autowired
    public StarWarsWiring(CharacterRepository characterRepository) {
        this.characterRepository = characterRepository;
    }

    private CharacterRepository getCharacterRepository() {
        return characterRepository;
    }

    /**
     * Fetches one record from the database based on the type and id.
     *
     */
    DataFetcher humanDataFetcher = environment -> {
        String id = environment.getArgument("id");

        return getCharacterRepository().findByTypeAndId(Human.class, Long.valueOf(id));
    };

    /**
     * Fetches one record from the database based on the type and id.
     */
    DataFetcher droidDataFetcher = environment -> {
        String id = environment.getArgument("id");

        return getCharacterRepository().findByTypeAndId(Droid.class, Long.valueOf(id));
    };

    /**
     * Fetches all characters that are associated with a given episode or a list of all characters if episode is not
     * specified.
     *
     */
    DataFetcher charactersDataFetcher = environment -> {
        Episode episode = environment.getArgument("episode");

        return episode == null ?
                getCharacterRepository().findAll() :
                getCharacterRepository().findByEpisode(episode);
    };

    /**
     * Fetches a list of friends of a given character.
     */
    DataFetcher friendsDataFetcher = environment -> {
        FilmCharacter character = environment.getSource();

        return getCharacterRepository().findByIdIsIn(character.getFriendsIds());
    };

    /**
     * Character in the graphql type system is an Interface and something needs
     * to decide that concrete graphql object type to return
     */
    TypeResolver characterTypeResolver = environment -> {
        FilmCharacter character = environment.getObject();
        if (character instanceof Human) {
            return (GraphQLObjectType) environment.getSchema().getType("Human");
        } else {
            return (GraphQLObjectType) environment.getSchema().getType("Droid");
        }
    };

    EnumValuesProvider episodeResolver = Episode::valueOf;
}
