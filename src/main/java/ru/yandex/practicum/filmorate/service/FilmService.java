package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;

@Service
@Slf4j
public class FilmService {

    // Константа: дата первого релиза (день рождения кино)
    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    private final FilmStorage filmStorage;
    // Нужен для проверки существования пользователя, который ставит лайк
    private final UserStorage userStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    // Получение фильма по id с проверкой существования
    private Film getFilmOrThrow(long id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + id + " не найден"));
    }

    // Получение пользователя по id с проверкой существования
    private void getUserOrThrow(long id) {
        userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));
    }

    public Film addLike(long filmId, long userId) {
        Film film = getFilmOrThrow(filmId);
        getUserOrThrow(userId); // проверка существования пользователя — иначе 404

        film.addLike(userId);
        filmStorage.update(film);
        log.debug("Пользователь id={} поставил лайк фильму id={}", userId, filmId);
        return film;
    }

    public Film removeLike(long filmId, long userId) {
        Film film = getFilmOrThrow(filmId);
        getUserOrThrow(userId); // проверка существования пользователя — иначе 404

        if (!film.removeLike(userId)) {
            throw new NotFoundException(
                    "Пользователь id=" + userId + " не ставил лайк фильму id=" + filmId
            );
        }

        filmStorage.update(film);
        log.debug("Пользователь id={} убрал лайк с фильма id={}", userId, filmId);
        return film;
    }

    public Collection<Film> getPopular(int count) {
        // Валидация: count должен быть положительным числом
        if (count <= 0) {
            throw new ValidationException(
                    "Количество фильмов должно быть положительным числом, получено: " + count
            );
        }
        return filmStorage.findPopular(count);
    }

    // Валидация даты релиза — не может быть раньше дня рождения кино
    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
            log.warn("Некорректная дата релиза: {}", film.getReleaseDate());
            throw new ValidationException(
                    "Дата релиза фильма не может быть раньше 28 декабря 1895 года"
            );
        }
    }

    public Film add(Film film) {
        validateReleaseDate(film);
        return filmStorage.add(film);
    }

    public Film update(Film film) {
        getFilmOrThrow(film.getId()); // проверка существования — иначе 404
        validateReleaseDate(film);
        return filmStorage.update(film);
    }

    public Collection<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film findById(long id) {
        return getFilmOrThrow(id);
    }
}