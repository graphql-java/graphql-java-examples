package com.graphql.java.hibernate.example.data.model;

import javax.persistence.Entity;

@Entity
public class Human extends FilmCharacter {
    private String homePlanet;

    public String getHomePlanet() {
        return homePlanet;
    }

    public void setHomePlanet(String homePlanet) {
        this.homePlanet = homePlanet;
    }

    @Override
    public String toString() {
        return "Human{" +
                "id='" + this.getId() + '\'' +
                ", name='" + this.getName() + '\'' +
                '}';
    }
}
