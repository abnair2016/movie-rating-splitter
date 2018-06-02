package com.anair.demo.component.movieratingsplitter.component.model;

import lombok.Data;

@Data
public class FilmRating {
    private String source;
    private String value;
    private Film film;

    public FilmRating() {}

    public FilmRating(final String source, final String value, final Film film) {
        this.source = source;
        this.value = value;
        this.film = film;
    }
}
