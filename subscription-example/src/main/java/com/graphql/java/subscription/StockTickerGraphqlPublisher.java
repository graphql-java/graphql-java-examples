package com.graphql.java.subscription;

import com.graphql.java.subscription.data.StockTickerPublisher;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.List;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@Component
public class StockTickerGraphqlPublisher {
    private final static StockTickerPublisher STOCK_TICKER_PUBLISHER = new StockTickerPublisher();

    private final GraphQLSchema graphQLSchema;

    public StockTickerGraphqlPublisher() {
        graphQLSchema = buildSchema();
    }

    private GraphQLSchema buildSchema() {
        //
        // reads a file that provides the schema types
        //
        Reader streamReader = loadSchemaFile("stocks.graphqls");
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(streamReader);

        RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Subscription")
                        .dataFetcher("stockQuotes", stockQuotesSubscriptionFetcher())
                )
                .build();

        return new SchemaGenerator().makeExecutableSchema(typeRegistry, wiring);
    }

    private DataFetcher stockQuotesSubscriptionFetcher() {
        return environment -> {
            List<String> stockCodes = environment.getArgument("stockCodes");
            if (stockCodes == null) {
                return STOCK_TICKER_PUBLISHER.getPublisher();
            } else {
                return STOCK_TICKER_PUBLISHER.getPublisher(stockCodes);
            }
        };
    }

    public GraphQLSchema getGraphQLSchema() {
        return graphQLSchema;
    }

    @SuppressWarnings("SameParameterValue")
    private Reader loadSchemaFile(String name) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(name);
        return new InputStreamReader(stream);
    }

}
