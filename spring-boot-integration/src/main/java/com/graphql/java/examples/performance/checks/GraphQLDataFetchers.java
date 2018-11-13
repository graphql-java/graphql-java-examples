package com.graphql.java.examples.performance.checks;

import graphql.schema.DataFetcher;
import org.springframework.stereotype.Component;

@Component
public class GraphQLDataFetchers {


    public DataFetcher getHelloWorldDataFetcher() {
        return environment -> "world";
    }

    public DataFetcher getEchoDataFetcher() {
        return environment -> environment.getArgument("toEcho");
    }


}
