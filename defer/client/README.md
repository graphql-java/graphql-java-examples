# Simple GraphQL React client using @defer

This React application demonstrates the usage of the @defer GraphQL statement. 

This simple example will simulate the loading of a list of books. The books details, like title and author, are fast to fetch. However, the list of comments added for each book is much slower (in the real world, this list might be fetched from a separate micro-service with bad performance). Instead of waiting for the comments to load in order to show the list of books, or make two separate server calls, we use @defer in the `comments` field. The server will then respond with a Multipart Http response, first sending in the books details followed by the books comments.
 
## Running the example

You'll need a running GraphQL server on http://localhost:8080/graphql. You can use the example server from XXX (TODO: add link to the server implementation).

Then run:

```
yarn start
```
to start the client React application.

That's it!

Navigate to http://localhost:3000 and you'll a list of books being rendered very quickly, followed by the list of comments for each book, which takes a few more seconds to load.