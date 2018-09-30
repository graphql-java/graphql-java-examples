package com.graphql.java.http.example.context;

import org.dataloader.DataLoaderRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ContextProvider {

    final DataLoaderRegistry dataLoaderRegistry;

    @Autowired
    public ContextProvider(DataLoaderRegistry dataLoaderRegistry) {
        this.dataLoaderRegistry = dataLoaderRegistry;
    }

    public Context newContext() {
        return new Context(dataLoaderRegistry);
    }

}
