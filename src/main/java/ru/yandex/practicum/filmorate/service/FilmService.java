package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.*;

@Service
@Slf4j
public class FilmService {

    private final FilmStorage filmStorage;
    // Нужен для проверки существования пользователя, который ставит лайк
    private final UserStorage userStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public Film addLike(long filmId, long userId) {
        Film film = filmStorage.findById(filmId);
        // Проверка существования пользователя — иначе 404
        userStorage.findById(userId);

        film.getLikes().add(userId);
        log.debug("Пользователь id={} поставил лайк фильму id={}", userId, filmId);
        return film;
    }

    public Film removeLike(long filmId, long userId) {
        Film film = filmStorage.findById(filmId);
        // Проверка существования пользователя — иначе 404
        userStorage.findById(userId);

        if (!film.getLikes().contains(userId)) {
            throw new NotFoundException(
                    "Пользователь id=" + userId + " не ставил лайк фильму id=" + filmId
            );
        }

        film.getLikes().remove(userId);
        log.debug("Пользователь id={} убрал лайк с фильма id={}", userId, filmId);
        return film;
    }

    public Collection<Film> getPopular(int count) {
        List<Film> allFilms = new ArrayList<>(filmStorage.findAll());

        // Сортировка по убыванию количества лайков
        Collections.sort(allFilms, new Comparator<Film>() {
            @Override
            public int compare(Film f1, Film f2) {
                return f2.getLikes().size() - f1.getLikes().size();
            }
        });

        // Возвращение не больше count фильмов
        int resultSize = Math.min(count, allFilms.size());
        return allFilms.subList(0, resultSize);
    }

    // Делегирование базовых операций хранилищу
    public Film add(Film film) {
        return filmStorage.add(film);
    }

    public Film update(Film film) {
        return filmStorage.update(film);
    }

    public Collection<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film findById(long id) {
        return filmStorage.findById(id);
    }
}