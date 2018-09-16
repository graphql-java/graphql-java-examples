package com.graphqljava.defer;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class GraphQLProvider {

    GraphQL graphQL;

    List<Map> books = ImmutableList.of(
            ImmutableMap.of("title", "Harry Potter and the Chamber of Secrets", "author", "J.K. Rowling"),
            ImmutableMap.of("title", "The Lord of the rings", "author", "J. R. R. Tolkien")
    );

    List<Map> commentsHarryPotter = ImmutableList.of(
            ImmutableMap.of("user", "andi", "text", "great"),
            ImmutableMap.of("user", "brad", "text", "read better ones"),
            ImmutableMap.of("user", "felipe", "text", "scary")
    );
    List<Map> commentsRings = ImmutableList.of(
            ImmutableMap.of("user", "andi", "text", "too long"),
            ImmutableMap.of("user", "anonymous", "text", "it is a book?")
    );

    DataFetcher<Object> booksFetcher = environment -> books;
    DataFetcher<Object> commentsFetcher = environment -> CompletableFuture.supplyAsync(() -> {
        Map<String, String> source = environment.getSource();
        if (source.get("title").contains("Potter")) {
            sleep(2000);
            return commentsHarryPotter;
        } else {
            sleep(3000);
            return commentsRings;
        }
    });

    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    public void init() throws IOException {
        URL url = Resources.getResource("schema.graphql");
        String sdl = Resources.toString(url, Charsets.UTF_8);
        GraphQLSchema graphQLSchema = buildSchema(sdl);
        this.graphQL = GraphQL.newGraphQL(graphQLSchema).build();
    }

    private GraphQLSchema buildSchema(String sdl) {
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
        RuntimeWiring runtimeWiring = buildWiring();
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
        return graphQLSchema;
    }

    private RuntimeWiring buildWiring() {
        return RuntimeWiring.newRuntimeWiring()
                .type("Query", builder -> builder
                        .dataFetcher("books", booksFetcher))
                .type("Book", builder -> builder
                        .dataFetcher("comments", commentsFetcher))
                .build();
    }

    @Bean
    public GraphQL graphQL() {
        return graphQL;
    }

}
