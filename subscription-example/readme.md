# graphql-java Subscriptions over WebSockets example

An example of using graphql subscriptions via websockets, graphql-java, reactive-streams and RxJava.

To build the example code in this repository type:

    ./gradlew build
    
To run the example code type:
    
    ./gradlew bootRun
    
To access the example application, point your browser at:

    http://localhost:8080/  
    
# Code Explanation

This example shows how you can use graphql-java subscription support to "subscribe" to a publisher of events.
Then as events occur, graphql-java will map the original graphql query over those same event objects and send out
a stream of `ExecutionResult` objects.

In this example application we have a stock update type system defined as:

    type Subscription {
        stockQuotes(stockCodes:[String]) : StockPriceUpdate!
    }
    
    type StockPriceUpdate {
        dateTime : String
        stockCode : String
        stockPrice : Float
        stockPriceChange : Float
    }

The JavaScript client sends a subscription graphql query over websockets to the server:

    var query = 'subscription StockCodeSubscription { \n' +
        '    stockQuotes {' +
        '       dateTime\n' +
        '       stockCode\n' +
        '       stockPrice\n' +
        '       stockPriceChange\n' +
        '     }' +
        '}';
    var graphqlMsg = {
        query: query,
        variables: {}
    };
    exampleSocket.send(JSON.stringify(graphqlMsg));
   
The server executes this with the graphql-java engine:

        GraphQL graphQL = GraphQL
                .newGraphQL(graphqlPublisher.getGraphQLSchema())
                .build();

        ExecutionResult executionResult = graphQL.execute(executionInput);

The result of that initial subscription query is a http://www.reactive-streams.org/ `Publisher`

        Publisher<ExecutionResult> stockPriceStream = executionResult.getData();
        
Under the covers a RxJava 2.x implementation is used to provide a stream of synthesized stock events.

RxJava Flows are an implementation of the reactive streams Publisher interface.  You can use ANY reactive streams
implementation as a source.  graphql-java uses the reactive streams interfaces as a common interface.

See https://github.com/ReactiveX/RxJava for more information on RxJava.  
        
The server side code then subscribes to this publisher of events and sends the results back over the websocket
to the waiting browser client:

        stockPriceStream.subscribe(new Subscriber<ExecutionResult>() {

            @Override
            public void onSubscribe(Subscription s) {
                subscriptionRef.set(s);
                request(1);
            }

            @Override
            public void onNext(ExecutionResult er) {
                log.debug("Sending stick price update");
                try {
                    Object stockPriceUpdate = er.getData();
                    String json = JsonKit.toJsonString(stockPriceUpdate);
                    webSocketSession.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                request(1);
            }

            @Override
            public void onError(Throwable t) {
                log.error("Subscription threw an exception", t);
                try {
                    webSocketSession.close();
                } catch (IOException e) {
                    log.error("Unable to close websocket session", e);
                }
            }

            @Override
            public void onComplete() {
                log.info("Subscription complete");
                try {
                    webSocketSession.close();
                } catch (IOException e) {
                    log.error("Unable to close websocket session", e);
                }
            }
        });

The selection set of fields named in the original query will be applied to each underlying stock update object.  

The selection set in this example application is selected as follows:

        stockQuotes {
            dateTime
            stockCode
            stockPrice
            stockPriceChange
        }

The underling stock update object is mapped to this selection of fields, just like any normal graphql query.  The format
of the results on the browser is JSON, again like any other normal graphql query.
