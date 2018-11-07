package com.graphqljava.defer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import graphql.DeferredExecutionResult;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class GraphQLController {


    public static final String CRLF = "\r\n";

    @Autowired
    GraphQL graphql;

    @Autowired
    ObjectMapper objectMapper;


    Logger log = LoggerFactory.getLogger(GraphQLController.class);

    @RequestMapping(value = "/test", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin
    public Mono<Void> graphql(ServerHttpResponse serverHttpResponse) throws IOException {
        ImmutableMap<String, Object> body = ImmutableMap.of("query", "{books{title author comments @defer {user text}}}");
        return graphql(body, serverHttpResponse);
    }

    @RequestMapping(value = "/graphql", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin
    public Mono<Void> graphql(@RequestBody Map<String, Object> body, ServerHttpResponse serverHttpResponse) throws IOException {
        String query = (String) body.get("query");
        if (query == null) {
            query = "";
        }
        Map<String, Object> variables = (Map<String, Object>) body.get("variables");
        if (variables == null) {
            variables = new LinkedHashMap<>();
        }
        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .variables(variables)
                .build();

        ExecutionResult executionResult = graphql.execute(executionInput);
        Map<Object, Object> extensions = executionResult.getExtensions();
        if (extensions != null && extensions.containsKey(GraphQL.DEFERRED_RESULTS)) {
            return handleDeferResponse(serverHttpResponse, executionResult, extensions);
        } else {
            return handleNormalResponse(serverHttpResponse, executionResult);
        }
    }

    private Mono<Void> handleDeferResponse(ServerHttpResponse serverHttpResponse,
                                           ExecutionResult executionResult,
                                           Map<Object, Object> extensions) {
        Publisher<DeferredExecutionResult> deferredResults = (Publisher<DeferredExecutionResult>) extensions.get(GraphQL.DEFERRED_RESULTS);
        try {
            return sendDeferResponse(serverHttpResponse, executionResult, deferredResults);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private Mono<Void> handleNormalResponse(ServerHttpResponse serverHttpResponse, ExecutionResult executionResult) throws IOException {
        Map<String, Object> result = executionResult.toSpecification();
        serverHttpResponse.setStatusCode(HttpStatus.OK);
        HttpHeaders headers = serverHttpResponse.getHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        String body = objectMapper.writeValueAsString(result);
        return serverHttpResponse.writeWith(strToDataBuffer(body));
    }

    private Mono<Void> sendDeferResponse(ServerHttpResponse serverHttpResponse, ExecutionResult executionResult, Publisher<DeferredExecutionResult> deferredResults) {
        // this implements this apollo defer spec: https://github.com/apollographql/apollo-server/blob/defer-support/docs/source/defer-support.md
        // the spec says CRLF + "-----" + CRLF is needed at the end, but it works without it and with it we get client
        // side errors with it, so we skp it
        serverHttpResponse.setStatusCode(HttpStatus.OK);
        HttpHeaders headers = serverHttpResponse.getHeaders();
        headers.set("Content-Type", "multipart/mixed; boundary=\"-\"");
        headers.set("Connection", "keep-alive");

        Flux<Mono<DataBuffer>> deferredDataBuffers = Flux.from(deferredResults).map(deferredExecutionResult -> {
            DeferPart deferPart = new DeferPart(deferredExecutionResult.toSpecification());
            StringBuilder builder = new StringBuilder();
            String body = deferPart.write();
            builder.append(CRLF).append("---").append(CRLF);
            builder.append(body);
            return strToDataBuffer(builder.toString());
        });
        Flux<Mono<DataBuffer>> firstResult = Flux.just(firstResult(executionResult));


        return serverHttpResponse.writeAndFlushWith(Flux.mergeSequential(firstResult, deferredDataBuffers));
    }

    private Mono<DataBuffer> firstResult(ExecutionResult executionResult) {
        StringBuilder builder = new StringBuilder();
        builder.append(CRLF).append("---").append(CRLF);
        DeferPart deferPart = new DeferPart(executionResult.toSpecification());
        String body = deferPart.write();
        builder.append(body);
        Mono<DataBuffer> dataBufferMono = strToDataBuffer(body);
        return dataBufferMono;
    }

    private Mono<DataBuffer> strToDataBuffer(String string) {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        DefaultDataBufferFactory defaultDataBufferFactory = new DefaultDataBufferFactory();
        return Mono.just(defaultDataBufferFactory.wrap(bytes));
    }

    private class DeferPart {

        private Object body;

        public DeferPart(Object data) {
            this.body = data;
        }

        public String write() {
            StringBuilder result = new StringBuilder();
            String bodyString = bodyToString();
            result.append("Content-Type: application/json").append(CRLF);
            result.append("Content-Length: ").append(bodyString.length()).append(CRLF).append(CRLF);
            result.append(bodyString);
            return result.toString();
        }

        private String bodyToString() {
            try {
                return objectMapper.writeValueAsString(body);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
