package com.graphql.java.examples.performance.checks;

import com.graphql.java.examples.performance.checks.data.FilmCharacter;
import com.graphql.java.examples.performance.checks.data.StarWarsData;
import graphql.schema.DataFetcher;
import org.springframework.stereotype.Component;

import javax.naming.Context;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Component
public class GraphQLDataFetchers {
    public DataFetcher getInstrumentedFieldDataFetcher() {
        return environment -> {
            final Integer sleepTime = environment.getArgument("sleep");

            sleep(sleepTime);

            return "value";
        };
    }

    public DataFetcher getCharactersDataFetcher() {
        return environment -> StarWarsData.getAllCharacters();
    }

    public DataFetcher getFriendsDataFetcher() {
        return environment -> {
            FilmCharacter character = environment.getSource();
            List<String> friendIds = character.getFriends();

            return friendIds.stream().map(StarWarsData::getCharacter).collect(toList());
        };
    }

    public DataFetcher getEnergyDataFetcher() {
        return environment -> Math.random() * 1000;
    }

    static void sleep(Integer seconds) {
        if(seconds == null) {
            return;
        }

        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
