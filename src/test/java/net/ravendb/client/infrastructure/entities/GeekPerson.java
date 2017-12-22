package net.ravendb.client.infrastructure.entities;

public class GeekPerson {
    private String name;
    private int[] favoritePrimes;
    private long[] favoriteVeryLargePrimes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int[] getFavoritePrimes() {
        return favoritePrimes;
    }

    public void setFavoritePrimes(int[] favoritePrimes) {
        this.favoritePrimes = favoritePrimes;
    }

    public long[] getFavoriteVeryLargePrimes() {
        return favoriteVeryLargePrimes;
    }

    public void setFavoriteVeryLargePrimes(long[] favoriteVeryLargePrimes) {
        this.favoriteVeryLargePrimes = favoriteVeryLargePrimes;
    }
}
