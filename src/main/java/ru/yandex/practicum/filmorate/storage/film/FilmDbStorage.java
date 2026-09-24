package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.mapper.FilmRowMapper;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    private final JdbcTemplate jdbc;
    private final FilmRowMapper mapper;

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbc, FilmRowMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
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
        // Перечитываем из базы: так в ответе будет название рейтинга, а не только его id
        return findById(id).orElseThrow();
    }

    @Override
    public Film update(Film film) {
        jdbc.update(UPDATE_QUERY, film.getName(), film.getDescription(), Date.valueOf(film.getReleaseDate()),
                film.getDuration(), getMpaId(film), film.getId());
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
        return jdbc.query(FIND_ALL_QUERY, mapper);
    }

    @Override
    public Optional<Film> findById(long id) {
        return jdbc.query(FIND_BY_ID_QUERY, mapper, id).stream().findFirst();
    }

    @Override
    public List<Film> findPopular(int count) {
        return jdbc.query(FIND_POPULAR_QUERY, mapper, count);
    }

    // Рейтинга может не быть — тогда в колонку mpa_id уходит NULL
    private Integer getMpaId(Film film) {
        return film.getMpa() == null ? null : film.getMpa().getId();
    }
}