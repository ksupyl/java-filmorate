package ru.yandex.practicum.filmorate.storage.film;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.mapper.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.mapper.GenreRowMapper;
import ru.yandex.practicum.filmorate.storage.mapper.UserRowMapper;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Лайкам нужны настоящие пользователи, поэтому подключаем и их хранилище
@JdbcTest
@AutoConfigureTestDatabase
@Import({FilmDbStorage.class, FilmRowMapper.class, GenreRowMapper.class,
        UserDbStorage.class, UserRowMapper.class})
class FilmDbStorageTest {

    private final FilmDbStorage filmStorage;
    private final UserDbStorage userStorage;

    @Autowired
    FilmDbStorageTest(FilmDbStorage filmStorage, UserDbStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    // Фильм без рейтинга и жанров: тесты добавляют их сами, где нужно
    private Film newFilm(String name) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        return film;
    }

    private Film createFilm(String name) {
        return filmStorage.add(newFilm(name));
    }

    private User createUser(String login) {
        User user = new User();
        user.setEmail(login + "@mail.ru");
        user.setLogin(login);
        user.setName("Пользователь " + login);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return userStorage.add(user);
    }

    private static Mpa mpa(int id) {
        Mpa mpa = new Mpa();
        mpa.setId(id);
        return mpa;
    }

    private static Genre genre(int id) {
        Genre genre = new Genre();
        genre.setId(id);
        return genre;
    }

    private static List<String> genreNames(Film film) {
        return film.getGenres().stream().map(Genre::getName).toList();
    }

    @Test
    void shouldAddFilmWithMpaAndGenres() {
        Film film = newFilm("С жанрами");
        film.setMpa(mpa(1));
        // Дубль жанра 3 схлопнется в Set, а в базе жанры встанут по порядку id
        film.getGenres().addAll(List.of(genre(3), genre(1), genre(3)));

        Film saved = filmStorage.add(film);

        Film found = filmStorage.findById(saved.getId()).orElseThrow();
        assertEquals("G", found.getMpa().getName());
        assertEquals(List.of("Комедия", "Мультфильм"), genreNames(found));
    }

    @Test
    void shouldAddFilmWithoutMpaAndGenres() {
        Film film = createFilm("Без рейтинга");

        Film found = filmStorage.findById(film.getId()).orElseThrow();
        assertNull(found.getMpa());
        assertTrue(found.getGenres().isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenFilmNotFound() {
        assertTrue(filmStorage.findById(9999).isEmpty());
    }

    @Test
    void shouldFindAllFilms() {
        createFilm("Первый");
        createFilm("Второй");

        assertEquals(2, filmStorage.findAll().size());
    }

    @Test
    void shouldUpdateFilmAndReplaceGenres() {
        Film film = createFilm("Черновик");
        film.getGenres().add(genre(1));
        filmStorage.update(film);

        film.setName("Чистовик");
        film.getGenres().clear();
        film.getGenres().add(genre(4));
        filmStorage.update(film);

        Film found = filmStorage.findById(film.getId()).orElseThrow();
        assertEquals("Чистовик", found.getName());
        assertEquals(List.of("Триллер"), genreNames(found));
    }

    @Test
    void shouldDeleteFilm() {
        Film film = createFilm("Лишний");

        filmStorage.delete(film.getId());

        assertTrue(filmStorage.findById(film.getId()).isEmpty());
    }

    @Test
    void shouldAddAndRemoveLike() {
        Film film = createFilm("Любимый");
        User user = createUser("anna");

        filmStorage.addLike(film.getId(), user.getId());

        assertTrue(filmStorage.removeLike(film.getId(), user.getId()));  // лайк был
        assertFalse(filmStorage.removeLike(film.getId(), user.getId())); // удалять уже нечего
    }

    @Test
    void shouldFindPopularByLikes() {
        Film first = createFilm("Первый");
        Film second = createFilm("Второй");
        Film third = createFilm("Третий");
        User anna = createUser("anna");
        User boris = createUser("boris");
        // У второго два лайка, у первого один, у третьего ни одного
        filmStorage.addLike(second.getId(), anna.getId());
        filmStorage.addLike(second.getId(), boris.getId());
        filmStorage.addLike(first.getId(), anna.getId());

        List<Film> popular = filmStorage.findPopular(10);

        assertEquals(List.of(second.getId(), first.getId(), third.getId()),
                popular.stream().map(Film::getId).toList());
        assertEquals(2, filmStorage.findPopular(2).size());
    }
}