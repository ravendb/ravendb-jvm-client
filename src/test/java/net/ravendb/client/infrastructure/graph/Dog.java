package net.ravendb.client.infrastructure.graph;

public class Dog {
    private String id;
    private String name;
    private String[] likes;
    private String[] dislikes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getLikes() {
        return likes;
    }

    public void setLikes(String[] likes) {
        this.likes = likes;
    }

    public String[] getDislikes() {
        return dislikes;
    }

    public void setDislikes(String[] dislikes) {
        this.dislikes = dislikes;
    }
}
