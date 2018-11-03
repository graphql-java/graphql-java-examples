package com.graphql.java.hibernate.example.data.model;

import javax.persistence.Entity;

@Entity
public class Droid extends FilmCharacter {
    private String primaryFunction;

    public String getPrimaryFunction() {
        return primaryFunction;
    }

    public void setPrimaryFunction(String primaryFunction) {
        this.primaryFunction = primaryFunction;
    }

    @Override
    public String toString() {
        return "Droid{" +
                "id='" + this.getId() + '\'' +
                ", name='" + this.getName() + '\'' +
                '}';
    }
}
