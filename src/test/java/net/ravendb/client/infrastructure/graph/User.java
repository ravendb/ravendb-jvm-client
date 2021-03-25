package net.ravendb.client.infrastructure.graph;

import java.util.List;

public class User {

    private String id;
    private String name;
    private int age;

    private List<Rating> hasRated;

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

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public List<Rating> getHasRated() {
        return hasRated;
    }

    public void setHasRated(List<Rating> hasRated) {
        this.hasRated = hasRated;
    }

    public static class Rating {
        private String movie;
        private int score;

        public String getMovie() {
            return movie;
        }

        public void setMovie(String movie) {
            this.movie = movie;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }
    }
}
