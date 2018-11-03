package com.graphql.java.hibernate.example.data.repository;

import com.graphql.java.hibernate.example.data.model.Episode;
import com.graphql.java.hibernate.example.data.model.FilmCharacter;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Set;

public interface CharacterRepository extends CrudRepository<FilmCharacter, Long> {
    List<FilmCharacter> findByIdIsIn(Set<Long> ids);

    @Query("select character from FilmCharacter character where type(character) = ?1 and character.id = ?2")
    FilmCharacter findByTypeAndId(Class<?> type, Long id);

    @Query("select character from FilmCharacter character where ?1 member of character.appearsIn")
    List<FilmCharacter> findByEpisode(Episode episode);
}
