package com.graphql.java.examples.performance.checks.instrumentation;

import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A custom Instrumentation that will stop a Data Fetcher that takes too long to return a value.
 * <p>
 * This Instrumentation will act only when the query is being executed. Some other Instrumentations
 * (like {@link graphql.analysis.MaxQueryDepthInstrumentation} and {@link graphql.analysis.MaxQueryComplexityInstrumentation})
 * have their rules applied when the query is being analyzed.
 */
public class TimeoutInstrumentation extends SimpleInstrumentation {
    private final int timeoutSeconds;
    private final List<String> fields;

    /**
     * @param timeoutSeconds the timeout period in seconds
     * @param fields         which fields should have their {@link DataFetcher} instrumented
     */
    public TimeoutInstrumentation(int timeoutSeconds, String... fields) {
        this.timeoutSeconds = timeoutSeconds;
        this.fields = Arrays.asList(fields);
    }

    /**
     * Wraps the {@link DataFetcher} in another instance of {@link DataFetcher} that will throw a Timeout error
     * if the original one takes too long to return.
     *
     * @param dataFetcher the original {@link DataFetcher}
     * @param parameters  contains data about the environment and parameters
     * @return
     */
    @Override
    public DataFetcher<?> instrumentDataFetcher(
            DataFetcher<?> dataFetcher, InstrumentationFieldFetchParameters parameters
    ) {
        // Only apply instrumentation to specified fields
        if (!fields.contains(parameters.getEnvironment().getField().getName())) {
            return dataFetcher;
        }

        // Times out if the original dataFetcher doesn't return before the specified period.
        // This implementation is using RxJava Observables but it could very well be implemented using
        // CompletableFutures or Threads
        return environment ->
                Observable.fromCallable(() -> dataFetcher.get(environment))
                        .subscribeOn(Schedulers.computation())
                        .timeout(timeoutSeconds, TimeUnit.SECONDS)
                        .blockingFirst();

    }
}
