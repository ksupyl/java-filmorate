package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.mapper.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.mapper.GenreRowMapper;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

@Repository
public class FilmDbStorage implements FilmStorage {

    // Общее начало запросов на чтение: фильм + название его рейтинга
    private static final String SELECT_FILMS =
            "SELECT f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id, m.name AS mpa_name "
                    + "FROM films f "
                    + "LEFT JOIN mpa m ON f.mpa_id = m.id ";
    private static final String FIND_ALL_QUERY = SELECT_FILMS + "ORDER BY f.id";
    private static final String FIND_BY_ID_QUERY = SELECT_FILMS + "WHERE f.id = ?";
    private static final String FIND_POPULAR_QUERY = SELECT_FILMS
            + "LEFT JOIN film_likes fl ON f.id = fl.film_id "
            + "GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id, m.name "
            + "ORDER BY COUNT(fl.user_id) DESC, f.id "
            + "LIMIT ?";
    private static final String INSERT_QUERY =
            "INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES (?, ?, ?, ?, ?)";
    private static final String UPDATE_QUERY =
            "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? WHERE id = ?";
    private static final String DELETE_QUERY = "DELETE FROM films WHERE id = ?";
    private static final String DELETE_FILM_GENRES_QUERY = "DELETE FROM film_genres WHERE film_id = ?";
    private static final String INSERT_FILM_GENRE_QUERY =
            "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
    // Жанры одного фильма — по порядку id
    private static final String FIND_GENRES_BY_FILM_QUERY =
            "SELECT g.id, g.name FROM film_genres fg "
                    + "JOIN genres g ON fg.genre_id = g.id "
                    + "WHERE fg.film_id = ? "
                    + "ORDER BY g.id";
    // Жанры нескольких фильмов одним запросом; вместо %s подставляется «?, ?, ?» по числу фильмов
    private static final String FIND_GENRES_BY_FILMS_QUERY =
            "SELECT fg.film_id, g.id, g.name FROM film_genres fg "
                    + "JOIN genres g ON fg.genre_id = g.id "
                    + "WHERE fg.film_id IN (%s) "
                    + "ORDER BY fg.film_id, g.id";
    private static final String ADD_LIKE_QUERY =
            "MERGE INTO film_likes (film_id, user_id) KEY (film_id, user_id) VALUES (?, ?)";
    private static final String REMOVE_LIKE_QUERY =
            "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";

    private final JdbcTemplate jdbc;
    private final FilmRowMapper mapper;
    private final GenreRowMapper genreMapper;

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbc, FilmRowMapper mapper, GenreRowMapper genreMapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.genreMapper = genreMapper;
    }

    @Override
    public Film add(Film film) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());
            ps.setObject(5, getMpaId(film));
            return ps;
        }, keyHolder);

        Long id = keyHolder.getKeyAs(Long.class);
        saveGenres(id, film.getGenres());
        // Перечитываем из базы: так в ответе будут названия рейтинга и жанров, а не только их id
        return findById(id).orElseThrow();
    }

    @Override
    public Film update(Film film) {
        jdbc.update(UPDATE_QUERY, film.getName(), film.getDescription(), Date.valueOf(film.getReleaseDate()),
                film.getDuration(), getMpaId(film), film.getId());
        saveGenres(film.getId(), film.getGenres());
        return findById(film.getId()).orElseThrow();
    }

    @Override
    public Film delete(long id) {
        Film film = findById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + id + " не найден"));
        jdbc.update(DELETE_QUERY, id);
        return film;
    }

    @Override
    public Collection<Film> findAll() {
        List<Film> films = jdbc.query(FIND_ALL_QUERY, mapper);
        loadGenresForFilms(films);
        return films;
    }

    @Override
    public Optional<Film> findById(long id) {
        Optional<Film> film = jdbc.query(FIND_BY_ID_QUERY, mapper, id).stream().findFirst();
        film.ifPresent(this::loadGenres);
        return film;
    }

    @Override
    public List<Film> findPopular(int count) {
        List<Film> films = jdbc.query(FIND_POPULAR_QUERY, mapper, count);
        loadGenresForFilms(films);
        return films;
    }

    @Override
    public void addLike(long filmId, long userId) {
        jdbc.update(ADD_LIKE_QUERY, filmId, userId);
    }

    @Override
    public boolean removeLike(long filmId, long userId) {
        // update возвращает число удалённых строк: 0 — лайка не было
        return jdbc.update(REMOVE_LIKE_QUERY, filmId, userId) > 0;
    }

    // Рейтинга может не быть — тогда в колонку mpa_id уходит NULL
    private Integer getMpaId(Film film) {
        return film.getMpa() == null ? null : film.getMpa().getId();
    }

    // Заменяет жанры фильма: старые связи удаляются, новые вставляются одним пакетом
    private void saveGenres(long filmId, Set<Genre> genres) {
        jdbc.update(DELETE_FILM_GENRES_QUERY, filmId);
        if (genres == null || genres.isEmpty()) {
            return;
        }
        List<Object[]> rows = new ArrayList<>();
        for (Genre genre : genres) {
            rows.add(new Object[]{filmId, genre.getId()});
        }
        jdbc.batchUpdate(INSERT_FILM_GENRE_QUERY, rows);
    }

    // Жанры одного фильма
    private void loadGenres(Film film) {
        film.getGenres().addAll(jdbc.query(FIND_GENRES_BY_FILM_QUERY, genreMapper, film.getId()));
    }

    // Жанры списка фильмов: один запрос только по этим фильмам, раскладываем через Map
    private void loadGenresForFilms(List<Film> films) {
        if (films.isEmpty()) {
            return; // фильмов нет — запрос не нужен
        }
        Map<Long, Film> filmsById = new HashMap<>();
        for (Film film : films) {
            filmsById.put(film.getId(), film);
        }
        String placeholders = String.join(", ", Collections.nCopies(filmsById.size(), "?"));
        jdbc.query(String.format(FIND_GENRES_BY_FILMS_QUERY, placeholders), rs -> {
            Film film = filmsById.get(rs.getLong("film_id"));
            film.getGenres().add(genreMapper.mapRow(rs, rs.getRow()));
        }, filmsById.keySet().toArray());
    }
}