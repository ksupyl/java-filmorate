package ru.yandex.practicum.filmorate.storage.director;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.mapper.DirectorRowMapper;
import java.util.List;

@Repository
public class DirectorDbStorage implements DirectorStorage {

    private final JdbcTemplate jdbcTemplate;
    private final DirectorRowMapper directorRowMapper;

    private static final String INSERT_QUERY = "INSERT INTO directors (name) VALUES (?)";
    private static final String UPDATE_QUERY = "UPDATE directors SET name = ? WHERE id = ?";
    private static final String DELETE_QUERY = "DELETE FROM directors WHERE id = ?";
    private static final String SELECT_BY_ID_QUERY = "SELECT * FROM directors WHERE id = ?";
    private static final String SELECT_ALL_QUERY = "SELECT * FROM directors";

    @Autowired
    public DirectorDbStorage(JdbcTemplate jdbcTemplate, DirectorRowMapper directorRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.directorRowMapper = directorRowMapper;
    }

    @Override
    public Director addDirector(Director director) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(INSERT_QUERY, new String[]{"id"});
            ps.setString(1, director.getName());
            return ps;
        }, keyHolder);
        director.setId(keyHolder.getKey().longValue());
        return director;
    }

    @Override
    public Director updateDirector(Director director) {
        jdbcTemplate.update(UPDATE_QUERY, director.getName(), director.getId());
        return director;
    }

    @Override
    public void removeDirector(long id) {
        Director director = findDirectorById(id)
                .orElseThrow(() -> new NotFoundException("Director not found with id: " + id));
        jdbcTemplate.update(DELETE_QUERY, id);
    }

    @Override
    public Optional<Director> findDirectorById(long id) {
        Optional<Director> director = jdbcTemplate.query(SELECT_BY_ID_QUERY, directorRowMapper, id).stream().findFirst();
        return director;
    }

    @Override
    public List<Director> findAllDirectors() {
        List<Director> directors = jdbcTemplate.query(SELECT_ALL_QUERY, directorRowMapper);
        return directors;
    }
}
