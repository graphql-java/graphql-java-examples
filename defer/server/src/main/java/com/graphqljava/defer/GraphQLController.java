package com.graphqljava.defer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class GraphQLController {


    public static final String CRLF = "\r\n";
    @Autowired
    GraphQL graphql;

    @Autowired
    ObjectMapper objectMapper;


    Logger log = LoggerFactory.getLogger(GraphQLController.class);


    @RequestMapping(value = "/graphql", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void graphql(@RequestBody Map<String, Object> body, HttpServletResponse httpServletResponse) throws IOException {
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
            Publisher<DeferredExecutionResult> deferredResults = (Publisher<DeferredExecutionResult>) extensions.get(GraphQL.DEFERRED_RESULTS);
            sendDeferResponse(httpServletResponse, executionResult, deferredResults);
        } else {
            sendNormalResponse(httpServletResponse, executionResult);
        }
    }

    private void sendNormalResponse(HttpServletResponse httpServletResponse, ExecutionResult executionResult) throws IOException {
        Map<String, Object> result = executionResult.toSpecification();
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        httpServletResponse.setCharacterEncoding("UTF-8");
        httpServletResponse.setContentType("application/json");
        String body = objectMapper.writeValueAsString(result);
        PrintWriter writer = httpServletResponse.getWriter();
        writer.write(body);
        writer.close();

    }

    private void sendDeferResponse(HttpServletResponse httpServletResponse, ExecutionResult executionResult, Publisher<DeferredExecutionResult> deferredResults) throws IOException {
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        httpServletResponse.setCharacterEncoding("UTF-8");
        httpServletResponse.setContentType("multipart/mixed; boundary=\"-\"");
        httpServletResponse.setHeader("Transfer-Encoding", "chunked");
        PrintWriter writer = httpServletResponse.getWriter();

        writer.write("---" + CRLF);
        DeferPart deferPart = new DeferPart(executionResult.toSpecification());
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
                DeferPart deferPart = new DeferPart(executionResult.toSpecification());
                String body = deferPart.write();
                writer.write(body);
                try {
                    httpServletResponse.flushBuffer();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                subscription.request(10);
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onComplete() {
                writer.close();
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
            result.append("Content-Length: ").append(bodyString.length()).append(CRLF);
            result.append(bodyString).append(CRLF);
            result.append(CRLF).append("---").append(CRLF);
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
