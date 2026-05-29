package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class FilmServiceTest {

    private FilmService filmService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        InMemoryUserStorage userStorage = new InMemoryUserStorage();
        userService = new UserService(userStorage);
        filmService = new FilmService(new InMemoryFilmStorage(), userStorage);
    }

    // Вспомогательные методы - отмена дублирования кода создания объектов в каждом тесте
    private Film createFilm(String name) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        return filmService.add(film);
    }

    private User createUser(String login) {
        User user = new User();
        user.setEmail(login + "@mail.ru");
        user.setLogin(login);
        user.setName(login);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return userService.add(user);
    }

    @Test
    void shouldAddFilm() {
        Film film = createFilm("Inception");
        assertNotNull(film.getId());
    }

    @Test
    void shouldThrowWhenReleaseDateTooOld() {
        Film film = new Film();
        film.setName("Old Movie");
        film.setDescription("Очень старый");
        film.setReleaseDate(LocalDate.of(1895, 12, 27));
        film.setDuration(100);

        assertThrows(ValidationException.class, () -> filmService.add(film));
    }

    @Test
    void shouldAddAndRemoveLike() {
        Film film = createFilm("Inception");
        User user = createUser("userOne");

        filmService.addLike(film.getId(), user.getId());
        assertEquals(1, filmService.findById(film.getId()).getLikes().size());

        filmService.removeLike(film.getId(), user.getId());
        assertEquals(0, filmService.findById(film.getId()).getLikes().size());
    }

    @Test
    void shouldThrowWhenAddLikeFromNonExistentUser() {
        Film film = createFilm("Inception");
        assertThrows(NotFoundException.class, () -> filmService.addLike(film.getId(), 999L));
    }

    @Test
    void shouldThrowWhenRemoveLikeNotExist() {
        Film film = createFilm("Inception");
        User user = createUser("userOne");

        assertThrows(NotFoundException.class,
                () -> filmService.removeLike(film.getId(), user.getId()));
    }

    @Test
    void shouldReturnPopularFilms() {
        Film film1 = createFilm("Film1");
        Film film2 = createFilm("Film2");
        Film film3 = createFilm("Film3");

        User user1 = createUser("user1");
        User user2 = createUser("user2");

        // film2 получает 2 лайка, film1 — 1, film3 — 0
        filmService.addLike(film2.getId(), user1.getId());
        filmService.addLike(film2.getId(), user2.getId());
        filmService.addLike(film1.getId(), user1.getId());

        Collection<Film> popular = filmService.getPopular(3);
        Film[] films = popular.toArray(new Film[0]);

        assertEquals(film2.getId(), films[0].getId());
        assertEquals(film1.getId(), films[1].getId());
        assertEquals(film3.getId(), films[2].getId());
    }

    @Test
    void shouldReturnPopularWithCountLimit() {
        createFilm("Film1");
        createFilm("Film2");
        createFilm("Film3");

        Collection<Film> popular = filmService.getPopular(2);
        assertEquals(2, popular.size());
    }
}