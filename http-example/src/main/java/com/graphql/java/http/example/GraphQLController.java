package com.graphql.java.http.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphql.java.http.example.context.Context;
import com.graphql.java.http.example.context.ContextProvider;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class GraphQLController {

    private final GraphQL graphql;
    private final ObjectMapper objectMapper;
    private final ContextProvider contextProvider;

    @Autowired
    public GraphQLController(GraphQL graphql, ObjectMapper objectMapper, ContextProvider contextProvider) {
        this.graphql = graphql;
        this.objectMapper = objectMapper;
        this.contextProvider = contextProvider;
    }

    @RequestMapping(value = "/graphql", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin
    public void graphqlGET(@RequestParam("query") String query,
                           @RequestParam(value = "operationName", required = false) String operationName,
                           @RequestParam("variables") String variablesJson,
                           HttpServletResponse httpServletResponse) throws IOException {
        if (query == null) {
            query = "";
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        ;
        if (variablesJson != null) {
            variables = objectMapper.readValue(variablesJson, new TypeReference<Map<String, Object>>() {
            });
        }
        executeGraphqlQuery(httpServletResponse, operationName, query, variables);
    }


    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/graphql", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin
    public void graphql(@RequestBody Map<String, Object> body, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        String query = (String) body.get("query");
        if (query == null) {
            query = "";
        }
        String operationName = (String) body.get("operationName");
        Map<String, Object> variables = (Map<String, Object>) body.get("variables");
        if (variables == null) {
            variables = new LinkedHashMap<>();
        }
        executeGraphqlQuery(httpServletResponse, operationName, query, variables);
    }

    private void executeGraphqlQuery(HttpServletResponse httpServletResponse, String operationName, String query, Map<String, Object> variables) throws IOException {
        //
        // the context object is something that means something to down stream code.  It is instructions
        // from yourself to your other code such as DataFetchers.  The engine passes this on unchanged and
        // makes it available to inner code
        //
        // the graphql guidance says  :
        //
        //  - GraphQL should be placed after all authentication middleware, so that you
        //  - have access to the same session and user information you would in your
        //  - HTTP endpoint handlers.
        //
        Context context = contextProvider.newContext();

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .variables(variables)
                .operationName(operationName)
                .context(context)
                .build();

        ExecutionResult executionResult = graphql.execute(executionInput);
        handleNormalResponse(httpServletResponse, executionResult);
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
}
