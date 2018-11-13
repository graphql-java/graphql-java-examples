# Performance checks 

This example has a few mechanisms to prevent your GraphQL server from dealing with expensive queries sent by abusive clients 
(or maybe legitimate clients that running expensive queries unaware of the negative impacts they might cause).

Here we introduce 4 mechanisms to help with that task. 3 of them are based on GraphQL Java instrumentation capabilities,
and the forth one is a bit out GraphQL Java jurisdiction and more related to web servers.

1. MaxQueryDepthInstrumentation: limit the depth of queries to 5
2. MaxQueryComplexityInstrumentation: set complexity values to fields and limit query complexity to 5 
3. A custom Instrumentation that sets a timeout period of 3 seconds for DataFetchers 
4. A hard request timeout of 10 seconds, specified in the web server level (Spring) 

# The schema
The schema we're using is quite simple:

```graphql
type Query {
    instrumentedField(sleep: Int): String
    characters: [Character]
}

type Character {
    name: String!
    friends: [Character]
    energy: Float
}
```

There are a few interesting facts related to this example.

* instrumentedField: returns a fixed string ("value"). It receives a parameter "sleep" that forces the DataFetcher to
  take that amount of seconds to return. Set that value to anything above 3 seconds and a timeout error will be thrown.
  This simulates a long running DataFetcher that would be forcefully stopped.
  
```graphql
{
  instrumentedField(sleep: 4) # will result in an error
}
``` 
  
* friends: will return a list of characters, that can themselves have friends, and so on... It's quite clear that queries
  might overuse this field and end up having a large number of nested friends and characters. Add 5 or more levels of 
  friends and an error will be thrown.

```graphql
{
  characters {
    name
    friends {
      name
      friends {
        name
        friends {
          name
          friends {
            name     # an error will be thrown, since the depth is higher than the limit
          }
        }
      }
    }
  }
}
```

* energy: getting this field involves some expensive calculations, so we've established that it has a complexity value
  of 3 (all the other fields have complexity 0). We've also defined that a query can have a maximum complexity of 5. So,
  if "energy" is present 2 times or more in a given query, an error will be thrown.
 
```graphql
{
  characters {
    name
    energy
    friends {
      name
      energy   # an error will be thrown, since we've asked for "energy" 2 times
    }
  }
}
``` 

# Request timeout
Although this is not really GraphQL Java business, it might be useful to set a hard request timeout on the web server
level.
To achieve this using Spring, the following property can be used:

```
spring.mvc.async.request-timeout=10000
```


# Running the code
 
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
     
## Note about introspection and max query depth
A bad side effect of specifying a maximum depth for queries is that this will prevent introspection queries to properly
execute. This affects GraphiQL's documentation and autocomplete features, that will simply not work. 
This is a tricky problem to fix and [has been discussed in the past](https://github.com/graphql-java/graphql-java/issues/1055). 

You can still use GraphiQL to execute queries and inspect results. If you want documentation and autocomplete back in 
GraphiQL, just temporarily disable the max depth instrumentation. 
