package com.graphql.java.hibernate.example.data.model;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import java.util.Set;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class FilmCharacter {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;

    /**
     * Eagerly fetches a list of friends ids.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "CharacterFriend",
            joinColumns = @JoinColumn(name = "CharacterId"))
    @Column(name = "FriendId")
    private Set<Long> friendsIds;

    @ElementCollection(targetClass = Episode.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "MovieAppearance")
    @Column(name = "episode")
    private Set<Episode> appearsIn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Episode> getAppearsIn() {
        return appearsIn;
    }

    public void setAppearsIn(Set<Episode> appearsIn) {
        this.appearsIn = appearsIn;
    }

    public Set<Long> getFriendsIds() {
        return friendsIds;
    }

    public void setFriendsIds(Set<Long> friendsIds) {
        this.friendsIds = friendsIds;
    }
}
