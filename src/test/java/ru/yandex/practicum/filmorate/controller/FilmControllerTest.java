package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FilmControllerTest {
    private FilmController filmController;

    @BeforeEach
    void setUp() {
        // Перед каждым тестом создаётся новый чистый контроллер
        filmController = new FilmController();
    }

    @Test
    void shouldAddFilmWhenDataIsValid() {
        Film film = new Film();
        film.setName("Inception");
        film.setDescription("Sci-Fi thriller");
        film.setReleaseDate(LocalDate.of(2010, 7, 16));
        film.setDuration(148);

        Film savedFilm = filmController.create(film);

        assertNotNull(savedFilm.getId());
        assertEquals(1, filmController.findAll().size());
    }

    @Test
    void shouldThrowExceptionWhenReleaseDateIsTooOld() {
        Film film = new Film();
        film.setName("Old Movie");
        film.setDescription("Very old");
        // Установка граничного условия: дата раньше 28 декабря 1895 года
        film.setReleaseDate(LocalDate.of(1895, 12, 27));
        film.setDuration(100);

        // Проверка, что при попытке создать такой фильм выбросится ValidationException
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            filmController.create(film);
        });

        assertEquals("Дата релиза фильма не может быть раньше 28 декабря 1895 года", exception.getMessage());
    }
}
