package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.List;

public interface FilmStorage {

    Film add(Film film);

    Film update(Film film);

    Film delete(long id);

    Collection<Film> findAll();

    Film findById(long id);

    List<Film> findPopular(int count);
}