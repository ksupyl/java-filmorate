package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;

@Component
@Slf4j
public class InMemoryFilmStorage implements FilmStorage {

    // Хранилище фильмов в памяти
    private final Map<Long, Film> films = new HashMap<>();
    private long currentId = 1;

    @Override
    public Film add(Film film) {
        film.setId(currentId++);
        films.put(film.getId(), film);
        log.debug("Добавлен фильм: id={}, name={}", film.getId(), film.getName());
        return film;
    }

    @Override
    public Film update(Film film) {

        films.put(film.getId(), film);
        log.debug("Обновлён фильм: id={}", film.getId());
        return film;
    }

    @Override
    public Film delete(long id) {
        Film film = findById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + id + " не найден"));

        films.remove(id);
        log.debug("Удалён фильм: id={}", id);
        return film;
    }

    @Override
    public Collection<Film> findAll() {
        return films.values();
    }

    @Override
    public Optional<Film> findById(long id) {
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public List<Film> findPopular(int count) {
        // Сортировка по убыванию лайков, берём первые count штук
        return films.values().stream()
                .sorted((f1, f2) -> f2.getLikes().size() - f1.getLikes().size())
                .limit(count)
                .collect(java.util.stream.Collectors.toList());
    }
}