package ru.yandex.practicum.filmorate.storage.genre;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.mapper.GenreRowMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// Поднимается только база и JdbcTemplate, хранилище и маппер подключаем сами
@JdbcTest
@AutoConfigureTestDatabase
@Import({GenreDbStorage.class, GenreRowMapper.class})
class GenreDbStorageTest {

    private final GenreDbStorage genreStorage;

    @Autowired
    GenreDbStorageTest(GenreDbStorage genreStorage) {
        this.genreStorage = genreStorage;
    }

    @Test
    void shouldFindAllGenresInIdOrder() {
        List<Genre> genres = genreStorage.findAll();

        assertEquals(6, genres.size());
        assertEquals("Комедия", genres.get(0).getName());
        assertEquals("Боевик", genres.get(5).getName());
    }

    @Test
    void shouldFindGenreById() {
        Optional<Genre> genre = genreStorage.findById(2);

        assertTrue(genre.isPresent());
        assertEquals("Драма", genre.get().getName());
    }

    @Test
    void shouldReturnEmptyWhenGenreNotFound() {
        assertTrue(genreStorage.findById(9999).isEmpty());
    }
}