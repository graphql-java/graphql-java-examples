package com.graphql.java.examples.performance.checks;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.graphql.java.examples.performance.checks.instrumentation.InstrumentationFactory;
import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
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
import java.util.Arrays;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@Component
public class GraphQLProvider {
    @Autowired
    private GraphQLDataFetchers graphQLDataFetchers;

    private GraphQL graphQL;

    @PostConstruct
    public void init() throws IOException {
        URL url = Resources.getResource("schema.graphql");
        String sdl = Resources.toString(url, Charsets.UTF_8);
        GraphQLSchema graphQLSchema = buildSchema(sdl);

        Instrumentation chainedInstrumentation = new ChainedInstrumentation(Arrays.asList(
                InstrumentationFactory.maxDepthInstrumentation(5),
                InstrumentationFactory.timeoutInstrumentation(3, "instrumentedField"),
                InstrumentationFactory.maxComplexityInstrumentation(
                        5,
                        ImmutableMap.<String, Integer>builder()
                                .put("energy", 3)
                                .build()
                )
        ));

        this.graphQL = GraphQL
                .newGraphQL(graphQLSchema)
                .instrumentation(chainedInstrumentation)
                .build();
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
                        .dataFetcher("instrumentedField", graphQLDataFetchers.getInstrumentedFieldDataFetcher())
                        .dataFetcher("characters", graphQLDataFetchers.getCharactersDataFetcher())
                        .build())
                .type(newTypeWiring("Character")
                        .dataFetcher("friends", graphQLDataFetchers.getFriendsDataFetcher())
                        .dataFetcher("energy", graphQLDataFetchers.getEnergyDataFetcher())
                )
                .build();

    }

    @Bean
    public GraphQL graphQL() {
        return graphQL;
    }

}
