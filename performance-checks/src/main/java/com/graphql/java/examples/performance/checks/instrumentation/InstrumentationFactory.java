package com.graphql.java.examples.performance.checks.instrumentation;

import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.instrumentation.Instrumentation;

import java.util.Map;

/**
 * Contains methods to create instances of the 3 {@link Instrumentation} used in this example
 */
public class InstrumentationFactory {

    /**
     * Creates an instance of our custom {@link TimeoutInstrumentation}
     *
     * @param timeoutSeconds the timeout period in seconds
     * @param fields         which fields should have their {@link graphql.schema.DataFetcher} instrumented
     * @return an instance of {@link TimeoutInstrumentation}
     * @see TimeoutInstrumentation
     */
    public static Instrumentation timeoutInstrumentation(int timeoutSeconds, String... fields) {
        return new TimeoutInstrumentation(timeoutSeconds, fields);
    }

    /**
     * Creates an instance of the {@link MaxQueryDepthInstrumentation}, defined in the GraphQL Java library
     * <p>
     * This Instrumentation will analyze the incoming GraphQL queries and, if they contain more nested fields than
     * the limit specified by maxDepth, an error will be thrown before any data fetchers execute.
     *
     * @param maxDepth the maximum depth allowed in the queries
     * @return an instance of {@link MaxQueryDepthInstrumentation}
     */
    public static Instrumentation maxDepthInstrumentation(int maxDepth) {
        return new MaxQueryDepthInstrumentation(maxDepth);
    }

    /**
     * Creates an instance of the {@link MaxQueryComplexityInstrumentation}, defined in the GraphQL Java library
     * <p>
     * This Instrumentation will analyze the incoming GraphQL queries and, if the sum of the complexity of the fields
     * used in the query is higher than the limit specified by maxComplexity an error will be thrown before any
     * data fetchers execute.
     * <p>
     * The default implementation of {@link MaxQueryComplexityInstrumentation} considers that every field has a
     * complexity of 1. This behaviour can be extended by providing an implementation of
     * {@link graphql.analysis.FieldComplexityCalculator}.
     * <p>
     * The example {@link Instrumentation} created by this method allows the consumer to specify arbitrary
     * complexity for each field.
     *
     * @param maxComplexity    the maximum complexity allowed in the query
     * @param fieldsComplexity a map containing the complexity of each field. Fields that are not contained in the map
     *                         are considered to have complexity equal to 0.
     * @return an instance of {@link MaxQueryComplexityInstrumentation}
     */
    public static Instrumentation maxComplexityInstrumentation(
            int maxComplexity, Map<String, Integer> fieldsComplexity
    ) {
        return new MaxQueryComplexityInstrumentation(maxComplexity, (environment, childComplexity) -> {
            final String fieldName = environment.getField().getName();

            final int thisComplexity = fieldsComplexity.getOrDefault(fieldName, 0);

            return thisComplexity + childComplexity;
        });
    }

}
