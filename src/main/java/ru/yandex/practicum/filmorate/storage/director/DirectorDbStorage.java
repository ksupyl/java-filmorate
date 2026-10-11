package ru.yandex.practicum.filmorate.storage.director;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.mapper.DirectorRowMapper;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class DirectorDbStorage implements DirectorStorage {

    private static final String INSERT_QUERY = "INSERT INTO directors (name) VALUES (?)";
    private static final String UPDATE_QUERY = "UPDATE directors SET name = ? WHERE id = ?";
    private static final String DELETE_QUERY = "DELETE FROM directors WHERE id = ?";
    private static final String SELECT_BY_ID_QUERY = "SELECT id, name FROM directors WHERE id = ?";
    private static final String SELECT_ALL_QUERY = "SELECT id, name FROM directors ORDER BY id";

    private final JdbcTemplate jdbcTemplate;
    private final DirectorRowMapper directorRowMapper;

    @Autowired
    public DirectorDbStorage(JdbcTemplate jdbcTemplate, DirectorRowMapper directorRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.directorRowMapper = directorRowMapper;
    }

    @Override
    public Director addDirector(Director director) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_QUERY, new String[]{"id"});
            ps.setString(1, director.getName());
            return ps;
        }, keyHolder);
        director.setId(keyHolder.getKeyAs(Long.class));
        return director;
    }

    @Override
    public Director updateDirector(Director director) {
        // update возвращает число изменённых строк: 0 — режиссёра с таким id нет
        int updatedRows = jdbcTemplate.update(UPDATE_QUERY, director.getName(), director.getId());
        if (updatedRows == 0) {
            throw new NotFoundException("Режиссёр с id=" + director.getId() + " не найден");
        }
        return director;
    }

    // Связи с фильмами база удаляет сама — каскадом из schema.sql
    @Override
    public void removeDirector(long id) {
        if (jdbcTemplate.update(DELETE_QUERY, id) == 0) {
            throw new NotFoundException("Режиссёр с id=" + id + " не найден");
        }
    }

    @Override
    public Optional<Director> findDirectorById(long id) {
        return jdbcTemplate.query(SELECT_BY_ID_QUERY, directorRowMapper, id).stream().findFirst();
    }

    @Override
    public List<Director> findAllDirectors() {
        return jdbcTemplate.query(SELECT_ALL_QUERY, directorRowMapper);
    }
}
