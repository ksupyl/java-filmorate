package ru.yandex.practicum.filmorate.storage.mpa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.mapper.MpaRowMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// Поднимается только база и JdbcTemplate, хранилище и маппер подключаем сами
@JdbcTest
@AutoConfigureTestDatabase
@Import({MpaDbStorage.class, MpaRowMapper.class})
class MpaDbStorageTest {
    private final MpaDbStorage mpaStorage;

    @Autowired
    MpaDbStorageTest(MpaDbStorage mpaStorage) {
        this.mpaStorage = mpaStorage;
    }

    @Test
    void shouldFindAllRatingsInIdOrder() {
        List<Mpa> ratings = mpaStorage.findAll();

        assertEquals(5, ratings.size());
        assertEquals("G", ratings.get(0).getName());
        assertEquals("NC-17", ratings.get(4).getName());
    }

    @Test
    void shouldFindMpaById() {
        Optional<Mpa> mpa = mpaStorage.findById(3);

        assertTrue(mpa.isPresent());
        assertEquals("PG-13", mpa.get().getName());
    }

    @Test
    void shouldReturnEmptyWhenMpaNotFound() {
        assertTrue(mpaStorage.findById(9999).isEmpty());
    }
}