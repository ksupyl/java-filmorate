package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.mapper.DirectorRowMapper;
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

    // Базовый запрос для популярных фильмов: WHERE-условия добавляются динамически
    private static final String FIND_POPULAR_BASE_QUERY = SELECT_FILMS
            + "LEFT JOIN film_likes fl ON f.id = fl.film_id ";
    // Хвост запроса популярных фильмов: группировка и сортировка по числу лайков
    private static final String FIND_POPULAR_TAIL_QUERY = " "
            + "GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id, m.name "
            + "ORDER BY COUNT(fl.user_id) DESC, f.id";
    // Лимит отдельно: в общих фильмах, поиске и фильмах режиссёра его нет
    private static final String LIMIT_CLAUSE = " LIMIT ?";

    // Условия фильтрации популярных фильмов: по жанру и по году выпуска
    private static final String FIND_POPULAR_GENRE_FILTER =
            "f.id IN (SELECT film_id FROM film_genres WHERE genre_id = ?)";
    private static final String FIND_POPULAR_YEAR_FILTER =
            "YEAR(f.release_date) = ?";
    private static final String LIKED_BY_USER_FILTER =
            "f.id IN (SELECT film_id FROM film_likes WHERE user_id = ?)";

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
    private static final String INSERT_FILM_DIRECTOR_QUERY =
            "INSERT INTO film_director (film_id, director_id) VALUES (?, ?)";
    private static final String DELETE_DIRECTOR_FILM_QUERY =
            "DELETE FROM film_director WHERE film_id = ?";
    private static final String FIND_DIRECTORS_BY_FILM_QUERY =
            "SELECT d.id, d.name FROM film_director fd "
                    + "JOIN directors d ON fd.director_id = d.id "
                    + "WHERE fd.film_id = ? "
                    + "ORDER BY d.id";
    private static final String FIND_DIRECTORS_BY_FILMS_QUERY =
            "SELECT fd.film_id, d.id, d.name " +
                    "FROM film_director fd " +
                    "JOIN directors d ON fd.director_id = d.id " +
                    "WHERE fd.film_id IN (%s) " +
                    "ORDER BY fd.film_id, d.id";
    private static final String FIND_DIRECTORS_BY_LIKES_AND_YEAR =
            "SELECT f.id, f.name, f.description, f.release_date, f.duration, " +
            "f.mpa_id, m.name AS mpa_name " +
            "FROM films f " +
            "JOIN film_director fd ON f.id = fd.film_id " +
            "LEFT JOIN mpa m ON f.mpa_id = m.id " +
            "LEFT JOIN film_likes fl ON f.id = fl.film_id " +
            "WHERE fd.director_id = ? " +
            "GROUP BY f.id, f.name, f.description, f.release_date, " +
            "f.duration, f.mpa_id, m.name ";

    private final JdbcTemplate jdbc;
    private final FilmRowMapper mapper;
    private final GenreRowMapper genreMapper;
    private final DirectorRowMapper directorMapper;

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbc, FilmRowMapper mapper, GenreRowMapper genreMapper, DirectorRowMapper directorMapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.genreMapper = genreMapper;
        this.directorMapper = directorMapper;
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
        saveDirector(id, film.getDirectors());
        // Перечитываем из базы: так в ответе будут названия рейтинга и жанров, а не только их id
        return findById(id).orElseThrow();
    }

    @Override
    public Film update(Film film) {
        jdbc.update(UPDATE_QUERY, film.getName(), film.getDescription(), Date.valueOf(film.getReleaseDate()),
                film.getDuration(), getMpaId(film), film.getId());
        saveGenres(film.getId(), film.getGenres());
        saveDirector(film.getId(), film.getDirectors());
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
        loadDirectors(films);
        return films;
    }

    @Override
    public Optional<Film> findById(long id) {
        Optional<Film> film = jdbc.query(FIND_BY_ID_QUERY, mapper, id).stream().findFirst();
        film.ifPresent(this::loadGenres);
        film.ifPresent(this::loadDirectors);
        return film;
    }

    @Override
    public List<Film> findPopular(int count, Integer genreId, Integer year) {
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (genreId != null) {
            // Подзапрос вместо JOIN: иначе фильм с несколькими жанрами задвоит строки
            conditions.add(FIND_POPULAR_GENRE_FILTER);
            params.add(genreId);
        }
        if (year != null) {
            conditions.add(FIND_POPULAR_YEAR_FILTER);
            params.add(year);
        }
        params.add(count); // LIMIT всегда последний

        String sql = buildPopularQuery(conditions) + LIMIT_CLAUSE;
        List<Film> films = jdbc.query(sql, mapper, params.toArray());
        loadGenresForFilms(films);
        loadDirectors(films);
        return films;
    }

    @Override
    public List<Film> findCommon(long userId, long friendId) {
        // Одно и то же условие дважды, с разными id: фильм лайкнули оба
        String sql = buildPopularQuery(List.of(LIKED_BY_USER_FILTER, LIKED_BY_USER_FILTER));
        List<Film> films = jdbc.query(sql, mapper, userId, friendId);
        loadGenresForFilms(films);
        loadDirectors(films);
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

    @Override
    public List<Film> findFilmByDirector(long directorId, String sortBy) {
        String orderBy;

        if ("year".equals(sortBy)) {
            orderBy = "ORDER BY f.release_date ASC, f.id";
        } else if ("likes".equals(sortBy)) {
            orderBy = "ORDER BY COUNT(fl.user_id) DESC, f.id";
        } else {
            throw new ValidationException(
                    "Неизвестный тип сортировки: " + sortBy
            );
        }

        List<Film> films = jdbc.query(FIND_DIRECTORS_BY_LIKES_AND_YEAR + orderBy, mapper, directorId
        );

        loadDirectors(films);
        loadGenresForFilms(films);

        return films;
    }

    // Собирает запрос популярных фильмов: базовое чтение + WHERE по условиям + группировка с лимитом
    private String buildPopularQuery(List<String> conditions) {
        StringBuilder sql = new StringBuilder(FIND_POPULAR_BASE_QUERY);
        if (!conditions.isEmpty()) {
            sql.append("WHERE ").append(String.join(" AND ", conditions)).append(" ");
        }
        return sql.append(FIND_POPULAR_TAIL_QUERY).toString();
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

    // Директора одного фильма
    private void loadDirectors(Film film) {
        film.getDirectors().addAll(jdbc.query(FIND_DIRECTORS_BY_FILM_QUERY, directorMapper, film.getId()));
    }

    private void loadDirectors(List<Film> films) {
        if (films.isEmpty()) {
            return; // фильмов нет — запрос не нужен
        }
        Map<Long, Film> filmsById = new HashMap<>();
        for (Film film : films) {
            filmsById.put(film.getId(), film);
        }
        String placeholders = String.join(
                ", ",
                Collections.nCopies(filmsById.size(), "?")
        );

        String query = String.format(
                FIND_DIRECTORS_BY_FILMS_QUERY,
                placeholders
        );

        jdbc.query(query, rs -> {
            Film film = filmsById.get(rs.getLong("film_id"));
            film.getDirectors().add(directorMapper.mapRow(rs, rs.getRow())
            );
        }, filmsById.keySet().toArray());
    }

    // Заменяет директоров фильма: старые связи удаляются, новые вставляются одним пакетом
    private void saveDirector(long filmId, Set<Director> directors) {
        jdbc.update(DELETE_DIRECTOR_FILM_QUERY, filmId);
        if (directors == null || directors.isEmpty()) {
            return;
        }
        List<Object[]> rows = new ArrayList<>();
        for (Director director : directors) {
            rows.add(new Object[]{filmId, director.getId()});
        }
        jdbc.batchUpdate(INSERT_FILM_DIRECTOR_QUERY, rows);
    }
}