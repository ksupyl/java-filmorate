package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class InMemoryFilmStorage implements FilmStorage {

    // Константа: дата первого релиза (день рождения кино)
    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    // Хранилище фильмов в памяти
    private final Map<Long, Film> films = new HashMap<>();
    private long currentId = 1;

    @Override
    public Film add(Film film) {
        validateReleaseDate(film);
        film.setId(currentId++);
        films.put(film.getId(), film);
        log.debug("Добавлен фильм: id={}, name={}", film.getId(), film.getName());
        return film;
    }

    @Override
    public Film update(Film film) {
        if (!films.containsKey(film.getId())) {
            log.warn("Попытка обновить несуществующий фильм: id={}", film.getId());
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }
        validateReleaseDate(film);
        films.put(film.getId(), film);
        log.debug("Обновлён фильм: id={}", film.getId());
        return film;
    }

    @Override
    public Film delete(long id) {
        Film film = findById(id);
        films.remove(id);
        log.debug("Удалён фильм: id={}", id);
        return film;
    }

    @Override
    public Collection<Film> findAll() {
        return films.values();
    }

    @Override
    public Film findById(long id) {
        Film film = films.get(id);
        if (film == null) {
            log.warn("Фильм не найден: id={}", id);
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }
        return film;
    }

    // Валидация даты релиза — не может быть раньше дня рождения кино
    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
            log.warn("Некорректная дата релиза: {}", film.getReleaseDate());
            throw new ValidationException(
                    "Дата релиза не может быть раньше 28 декабря 1895 года"
            );
        }
    }
}