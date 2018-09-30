package com.graphql.java.http.example.context;

import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;

/**
 * The context object is passed to each level of a graphql query and in this case it contains
 * the data loader registry.  This allows us to keep our data loaders per request since
 * they cache data and cross request caches are often not what you want.
 */
public class Context {

    final DataLoaderRegistry dataLoaderRegistry;

    Context(DataLoaderRegistry dataLoaderRegistry) {
        this.dataLoaderRegistry = dataLoaderRegistry;
    }

    public DataLoader<String, Object> getCharacterDataLoader() {
        return dataLoaderRegistry.getDataLoader("characters");
    }
}
