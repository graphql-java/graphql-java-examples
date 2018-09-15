import React from 'react';
import ApolloClient from 'apollo-boost';
import { ApolloProvider } from 'react-apollo';

import Books from './books';

const client = new ApolloClient({
  uri: 'http://localhost:8080/graphql'
});

const App = () => (
  <ApolloProvider client={client}>
    <div>
      <h2>GraphQL @defer</h2>
      <Books />
    </div>
  </ApolloProvider>
);

export default App;
