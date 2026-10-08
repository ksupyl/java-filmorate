package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.mapper.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.mapper.UserRowMapper;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.Optional;
import java.util.List;

@Repository
public class UserDbStorage implements UserStorage {

    private static final String INSERT_QUERY =
            "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_QUERY =
            "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";
    private static final String DELETE_QUERY =
            "DELETE FROM users WHERE id = ?";
    private static final String FIND_ALL_QUERY =
            "SELECT id, email, login, name, birthday FROM users ORDER BY id";
    private static final String FIND_BY_ID_QUERY =
            "SELECT id, email, login, name, birthday FROM users WHERE id = ?";
    private static final String ADD_FRIEND_QUERY =
            "MERGE INTO friendship (user_id, friend_id) KEY (user_id, friend_id) VALUES (?, ?)";
    private static final String REMOVE_FRIEND_QUERY =
            "DELETE FROM friendship WHERE user_id = ? AND friend_id = ?";
    private static final String FIND_FRIENDS_QUERY =
            "SELECT u.id, u.email, u.login, u.name, u.birthday FROM users u "
                    + "JOIN friendship f ON u.id = f.friend_id "
                    + "WHERE f.user_id = ? ORDER BY u.id";
    private static final String FIND_COMMON_FRIENDS_QUERY =
            "SELECT u.id, u.email, u.login, u.name, u.birthday FROM users u "
                    + "JOIN friendship f1 ON u.id = f1.friend_id AND f1.user_id = ? "
                    + "JOIN friendship f2 ON u.id = f2.friend_id AND f2.user_id = ? "
                    + "ORDER BY u.id";

    private static final String FIND_MOST_SIMILAR_USER_QUERY =
            "SELECT fl2.user_id AS other_id, COUNT(*) AS common "
                    + "FROM film_likes fl1 "
                    + "JOIN film_likes fl2 ON fl1.film_id = fl2.film_id "
                    + "WHERE fl1.user_id = ? AND fl2.user_id <> ? "
                    + "GROUP BY fl2.user_id "
                    + "ORDER BY common DESC, fl2.user_id "
                    + "LIMIT 1";
    private static final String FIND_RECOMMENDATIONS_QUERY =
            "SELECT f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id, m.name AS mpa_name "
                    + "FROM films f "
                    + "JOIN film_likes fl ON f.id = fl.film_id "
                    + "LEFT JOIN mpa m ON f.mpa_id = m.id "
                    + "WHERE fl.user_id = ? "
                    + "AND f.id NOT IN (SELECT film_id FROM film_likes WHERE user_id = ?) "
                    + "ORDER BY f.id";
    private final JdbcTemplate jdbc;
    private final UserRowMapper mapper;
    private final FilmRowMapper filmMapper;

    @Autowired
    public UserDbStorage(JdbcTemplate jdbc, UserRowMapper mapper, FilmRowMapper filmMapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.filmMapper = filmMapper;
    }

    @Override
    public User add(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, Date.valueOf(user.getBirthday()));
            return ps;
        }, keyHolder);
        user.setId(keyHolder.getKeyAs(Long.class));
        return user;
    }

    @Override
    public User update(User user) {
        jdbc.update(UPDATE_QUERY,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                Date.valueOf(user.getBirthday()),
                user.getId());
        return user;
    }

    @Override
    public User delete(long id) {
        User user = findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));
        jdbc.update(DELETE_QUERY, id);
        return user;
    }

    @Override
    public Collection<User> findAll() {
        return jdbc.query(FIND_ALL_QUERY, mapper);
    }

    @Override
    public Optional<User> findById(long id) {
        return jdbc.query(FIND_BY_ID_QUERY, mapper, id).stream().findFirst();
    }

    @Override
    public void addFriend(long userId, long friendId) {
        jdbc.update(ADD_FRIEND_QUERY, userId, friendId);
    }

    @Override
    public void removeFriend(long userId, long friendId) {
        jdbc.update(REMOVE_FRIEND_QUERY, userId, friendId);
    }

    @Override
    public Collection<User> findFriends(long userId) {
        return jdbc.query(FIND_FRIENDS_QUERY, mapper, userId);
    }

    @Override
    public Collection<User> findCommonFriends(long userId, long otherId) {
        return jdbc.query(FIND_COMMON_FRIENDS_QUERY, mapper, userId, otherId);
    }

    @Override
    public List<Film> findRecommendations(long userId) {
        List<Long> similarUserIds = jdbc.query(
                FIND_MOST_SIMILAR_USER_QUERY,
                (rs, rowNum) -> rs.getLong("other_id"),
                userId, userId
        );
        if (similarUserIds.isEmpty()) {
            return List.of();
        }

        Long similarUserId = similarUserIds.getFirst();
        return jdbc.query(FIND_RECOMMENDATIONS_QUERY, filmMapper, similarUserId, userId);
    }
}