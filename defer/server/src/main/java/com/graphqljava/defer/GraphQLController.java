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
//        PrintWriter writer = httpServletResponse.getWriter();
//        writer.write(body);
//        writer.close();

    }

    private Mono<Void> sendDeferResponse(ServerHttpResponse serverHttpResponse, ExecutionResult executionResult, Publisher<DeferredExecutionResult> deferredResults)  {
        serverHttpResponse.setStatusCode(HttpStatus.OK);
        HttpHeaders headers = serverHttpResponse.getHeaders();
        headers.set("Content-Type", "multipart/mixed; boundary=\"-\"");
        headers.set("transfer-encoding", "chunked");
//        return message.headers().contains(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED, true);
        headers.set("Connection", "keep-alive");


        DataBufferFactory dataBufferFactory = serverHttpResponse.bufferFactory();


//        serverHttpResponse.writeAndFlushWith(Mono.just(firstDataBuffer)).subscribe(aVoid -> {
//            System.out.println("done FIRST");
//        }, throwable -> {
//            throwable.printStackTrace();
//        }, () -> {
//            System.out.println("completed FIRST");
//        });


//        Flux<Mono<DataBuffer>> dataBufferFlux = Flux.from(deferredResults).map(deferredExecutionResult -> {
//            DeferPart deferPart = new DeferPart(executionResult.toSpecification());
//            StringBuilder builder = new StringBuilder();
//            String body = deferPart.write();
//            System.out.println("body:" + body);
//            builder.append(CRLF).append("---").append(CRLF);
//            builder.append(body);
//            Mono<DataBuffer> dataBuffer = strToDataBuffer(dataBufferFactory, builder.toString());
//            return dataBuffer;
//        });

//        Flux<Mono<DataBuffer>> resultFlux = Flux.mergeSequential(Flux.just(firstDataBuffer), dataBufferFlux);
//        serverHttpResponse.writeAndFlushWith(resultFlux).subscribe(aVoid -> {
//            StringBuilder end = new StringBuilder();
//            end.append(CRLF).append("-----").append(CRLF);
//            serverHttpResponse.writeWith(strToDataBuffer(dataBufferFactory, end.toString()));
//            serverHttpResponse.setComplete();
//
//        });

//        serverHttpResponse.beforeCommit(() -> {
//            System.out.println("BEFORE COMMIT");
//        });

        Flux<Mono<DataBuffer>> dataBufferFlux = Flux.create(monoFluxSink -> {

            Mono<DataBuffer> firstDataBuffer = firstResult(executionResult);
            monoFluxSink.next(firstDataBuffer);

            deferredResults.subscribe(new Subscriber<DeferredExecutionResult>() {

                Subscription subscription;

                @Override
                public void onSubscribe(Subscription s) {
                    subscription = s;
                    subscription.request(10);
                }

                @Override
                public void onNext(DeferredExecutionResult executionResult) {
                    try {
//                    DeferPart deferPart = new DeferPart(executionResult.toSpecification());
//                    String body = deferPart.write();
//                    writer.append(CRLF).append("---").append(CRLF);
//                    writer.write(body);
                        System.out.println("is comitted:" + serverHttpResponse.isCommitted());
                        DeferPart deferPart = new DeferPart(executionResult.toSpecification());
                        StringBuilder builder = new StringBuilder();
                        String body = deferPart.write();
                        System.out.println("body:" + body);
                        builder.append(CRLF).append("---").append(CRLF);
                        builder.append(body);
                        Mono<DataBuffer> dataBuffer = strToDataBuffer(builder.toString());
                        monoFluxSink.next(dataBuffer);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(Throwable t) {
                    t.printStackTrace();
                }

                @Override
                public void onComplete() {
//                writer.append(CRLF).append("-----").append(CRLF);
//                writer.close();
//                asyncContext.complete();
                    System.out.println("END!!!");
                    StringBuilder end = new StringBuilder();
                    end.append(CRLF).append("-----").append(CRLF);
                    Mono<DataBuffer> dataBuffer = strToDataBuffer(end.toString());
                    monoFluxSink.next(dataBuffer);
//                    serverHttpResponse.writeAndFlushWith(Mono.just().subscribe(aVoid -> {
//                        System.out.println("done END");
//                    }, throwable -> {
//                        throwable.printStackTrace();
//                    }, () -> {
//                        System.out.println("completed END");
//                        serverHttpResponse.setComplete();
//                    });
                }
            });

        });

        return serverHttpResponse.writeAndFlushWith(dataBufferFlux);
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
