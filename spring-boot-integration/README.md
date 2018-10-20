# GraphQL Java spring boot integration

A very simple example of how to integrate GraphQL Java with Spring boot. 
 
 To build the example code in this repository type:
 
``` 
./gradlew build
 ```
    
To run the example code type:
    
``` 
./gradlew bootRun
``` 
    
To access the example application, point your browser at:
     http://localhost:8080/  
    
# Code Explanation

The actual Spring MVC Controller is `GraphQLController` which accepts GET and POST requests on `/graphql`.

It uses the `GraphQL` instance provided by `GraphQLProvider`.

The actual data is retrieved by the DataFetchers in `GraphQLDataFetchers`.

