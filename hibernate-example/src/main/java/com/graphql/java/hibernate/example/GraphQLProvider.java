package com.graphql.java.hibernate.example;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import graphql.GraphQL;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.tracing.TracingInstrumentation;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URL;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@Component
public class GraphQLProvider {
    private GraphQL graphQL;
    private StarWarsWiring starWarsWiring;

    @Autowired
    public GraphQLProvider(StarWarsWiring starWarsWiring) {
        this.starWarsWiring = starWarsWiring;
    }

    @PostConstruct
    public void init() throws IOException {
        URL url = Resources.getResource("starWarsSchemaAnnotated.graphqls");
        String sdl = Resources.toString(url, Charsets.UTF_8);
        GraphQLSchema graphQLSchema = buildSchema(sdl);

        Instrumentation instrumentation = new TracingInstrumentation();

        this.graphQL = GraphQL.newGraphQL(graphQLSchema).instrumentation(instrumentation).build();
    }

    private GraphQLSchema buildSchema(String sdl) {
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
        RuntimeWiring runtimeWiring = buildWiring();
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
    }

    private RuntimeWiring buildWiring() {
        return RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Query")
                        .dataFetcher("characters", starWarsWiring.charactersDataFetcher)
                        .dataFetcher("human", starWarsWiring.humanDataFetcher)
                        .dataFetcher("droid", starWarsWiring.droidDataFetcher)
                )
                .type(newTypeWiring("Human")
                        .dataFetcher("friends", starWarsWiring.friendsDataFetcher)
                )
                .type(newTypeWiring("Droid")
                        .dataFetcher("friends", starWarsWiring.friendsDataFetcher)
                )
                .type(newTypeWiring("Character")
                        .typeResolver(starWarsWiring.characterTypeResolver)
                )
                .type(newTypeWiring("Episode")
                        .enumValues(starWarsWiring.episodeResolver)
                )
                .build();
    }

    @Bean
    public GraphQL graphQL() {
        return graphQL;
    }

}
