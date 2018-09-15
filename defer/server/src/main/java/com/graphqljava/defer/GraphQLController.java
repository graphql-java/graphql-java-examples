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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
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
    public void graphql(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        ImmutableMap<String, Object> body = ImmutableMap.of("query", "{books{title author comments @defer {user text}}}");
        graphql(body, httpServletRequest, httpServletResponse);
    }

    @RequestMapping(value = "/graphql", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin
    public void graphql(@RequestBody Map<String, Object> body, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
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
            handleDeferResponse(httpServletRequest, httpServletResponse, executionResult, extensions);
        } else {
            handleNormalResponse(httpServletResponse, executionResult);
        }
    }

    private void handleDeferResponse(HttpServletRequest httpServletRequest,
                                     HttpServletResponse httpServletResponse,
                                     ExecutionResult executionResult,
                                     Map<Object, Object> extensions) {
        AsyncContext asyncContext = httpServletRequest.startAsync();
        asyncContext.start(() -> {
            Publisher<DeferredExecutionResult> deferredResults = (Publisher<DeferredExecutionResult>) extensions.get(GraphQL.DEFERRED_RESULTS);
            try {
                sendDeferResponse(asyncContext, httpServletRequest, httpServletResponse, executionResult, deferredResults);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    private void handleNormalResponse(HttpServletResponse httpServletResponse, ExecutionResult executionResult) throws IOException {
        Map<String, Object> result = executionResult.toSpecification();
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        httpServletResponse.setCharacterEncoding("UTF-8");
        httpServletResponse.setContentType("application/json");
        httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");
        String body = objectMapper.writeValueAsString(result);
        PrintWriter writer = httpServletResponse.getWriter();
        writer.write(body);
        writer.close();

    }

    private void sendDeferResponse(AsyncContext asyncContext, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, ExecutionResult executionResult, Publisher<DeferredExecutionResult> deferredResults) throws IOException {
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        httpServletResponse.setCharacterEncoding("UTF-8");
        httpServletResponse.setContentType("multipart/mixed; boundary=\"-\"");
        httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");
        httpServletResponse.setHeader("Transfer-Encoding", "chunked");
        httpServletResponse.setHeader("Connection", "keep-alive");
        PrintWriter writer = httpServletResponse.getWriter();

        DeferPart deferPart = new DeferPart(executionResult.toSpecification());
        writer.append(CRLF).append("---").append(CRLF);
        String body = deferPart.write();
        writer.write(body);
        httpServletResponse.flushBuffer();

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
                    DeferPart deferPart = new DeferPart(executionResult.toSpecification());
                    String body = deferPart.write();
                    writer.append(CRLF).append("---").append(CRLF);
                    writer.write(body);
                    httpServletResponse.flushBuffer();
                    subscription.request(10);
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
                writer.append(CRLF).append("-----").append(CRLF);
                writer.close();
                asyncContext.complete();
            }
        });


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
