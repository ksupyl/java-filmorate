package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
public class FilmService {

    // Константа: дата первого релиза (день рождения кино)
    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    private final FilmStorage filmStorage;
    // Нужен для проверки существования пользователя, который ставит лайк
    private final UserStorage userStorage;

    // Нужны для проверки рейтинга и жанра из запроса
    private final MpaService mpaService;
    private final GenreService genreService;
    private final DirectorService directorService;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       MpaService mpaService,
                       GenreService genreService,
                       DirectorService directorService) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.mpaService = mpaService;
        this.genreService = genreService;
        this.directorService = directorService;
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

        filmStorage.addLike(filmId, userId);
        log.debug("Пользователь id={} поставил лайк фильму id={}", userId, filmId);
        return film;
    }

    public Film removeLike(long filmId, long userId) {
        Film film = getFilmOrThrow(filmId);
        getUserOrThrow(userId); // проверка существования пользователя — иначе 404

        if (!filmStorage.removeLike(filmId, userId)) {
            throw new NotFoundException(
                    "Пользователь id=" + userId + " не ставил лайк фильму id=" + filmId
            );
        }

        log.debug("Пользователь id={} убрал лайк с фильма id={}", userId, filmId);
        return film;
    }

    public Collection<Film> getPopular(int count, Integer genreId, Integer year) {
        // Валидация: count должен быть положительным числом
        if (count <= 0) {
            throw new ValidationException(
                    "Количество фильмов должно быть положительным числом, получено: " + count
            );
        }
        // Жанр, если указан, должен существовать — иначе 404
        if (genreId != null) {
            genreService.findById(genreId);
        }
        // Валидация: год, если указан, должен быть положительным
        if (year != null && year <= 0) {
            throw new ValidationException(
                    "Год должен быть положительным числом, получено: " + year
            );
        }
        return filmStorage.findPopular(count, genreId, year);
    }

    // Общие фильмы двух пользователей — те, что лайкнули оба, по убыванию популярности
    public Collection<Film> getCommon(long userId, long friendId) {
        getUserOrThrow(userId); // оба пользователя должны существовать — иначе 404
        getUserOrThrow(friendId);
        return filmStorage.findCommon(userId, friendId);
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

    // Рейтинг и жанры из запроса должны существовать в справочниках — иначе 404
    private void validateMpaAndGenres(Film film) {
        if (film.getMpa() != null) {
            mpaService.findById(film.getMpa().getId()); // бросит NotFoundException
        }
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }
        // Один запрос за всеми жанрами вместо запроса на каждый
        Set<Integer> knownIds = new HashSet<>();
        for (Genre genre : genreService.findAll()) {
            knownIds.add(genre.getId());
        }
        for (Genre genre : film.getGenres()) {
            if (!knownIds.contains(genre.getId())) {
                throw new NotFoundException("Жанр с id=" + genre.getId() + " не найден");
            }
        }
    }

    private void validateDirectors(Film film) {

        if (film.getDirectors() == null || film.getDirectors().isEmpty()) {
            return;
        }

        Set<Long> knownIds = new HashSet<>();
        for (Director director : directorService.getAllDirectors()) {
            knownIds.add(director.getId());
        }
        for (Director director : film.getDirectors()) {
            if (!knownIds.contains(director.getId())) {
                throw new NotFoundException("Режиссёр с id=" + director.getId() + " не найден");
            }
        }
    }

    public Film add(Film film) {
        validateReleaseDate(film);
        validateMpaAndGenres(film);
        validateDirectors(film);
        return filmStorage.add(film);
    }

    public Film update(Film film) {
        getFilmOrThrow(film.getId()); // проверка существования — иначе 404
        validateReleaseDate(film);
        validateMpaAndGenres(film);
        validateDirectors(film);
        return filmStorage.update(film);
    }

    public Collection<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film findById(long id) {
        return getFilmOrThrow(id);
    }

    public Collection<Film> findByDirectorSorted(long directorId, String sortBy) {
        directorService.getDirectorById(directorId); // проверка существования режиссёра — иначе 404
        return filmStorage.findFilmByDirector(directorId, sortBy);
    }
}