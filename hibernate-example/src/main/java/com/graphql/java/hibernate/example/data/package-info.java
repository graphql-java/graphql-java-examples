/**
 * This package contains the Hibernate code - entities and repository.
 *
 * There's absolutely nothing GraphQL specific about anything in this package. It could very well be replace with
 * other database library tools like QueryDSL, JDBC a NoSQL data repository, etc.
 *
 * Some might find that the entity relationship is not very trivial. We have an abstract entity `FilmCharacter` with
 * two concrete implementations: `Droid` and `Human`. This is to make this example support the same Star Wars GraphQL
 * Schema used on other examples in this repo.
 */
package com.graphql.java.hibernate.example.data;