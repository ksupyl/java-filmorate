package ru.yandex.practicum.filmorate.storage.review;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.mapper.ReviewRowMapper;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class ReviewDbStorage implements ReviewStorage {

    // Рейтинг полезности: лайк +1, дизлайк -1
    private static final String SELECT_REVIEWS =
            "SELECT r.id, r.content, r.is_positive, r.user_id, r.film_id, "
                    + "(SELECT COALESCE(SUM(CASE WHEN rl.is_useful THEN 1 ELSE -1 END), 0) "
                    + "FROM review_likes rl WHERE rl.review_id = r.id) AS useful "
                    + "FROM reviews r ";
    private static final String FIND_BY_ID_QUERY = SELECT_REVIEWS + "WHERE r.id = ?";
    private static final String FIND_ALL_QUERY = SELECT_REVIEWS
            + "ORDER BY useful DESC, r.id LIMIT ?";
    private static final String FIND_BY_FILM_QUERY = SELECT_REVIEWS
            + "WHERE r.film_id = ? ORDER BY useful DESC, r.id LIMIT ?";
    private static final String INSERT_QUERY =
            "INSERT INTO reviews (content, is_positive, user_id, film_id) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_QUERY =
            "UPDATE reviews SET content = ?, is_positive = ? WHERE id = ?";
    private static final String DELETE_QUERY = "DELETE FROM reviews WHERE id = ?";
    private static final String ADD_RATING_QUERY =
            "MERGE INTO review_likes (review_id, user_id, is_useful) KEY (review_id, user_id) VALUES (?, ?, ?)";
    private static final String REMOVE_RATING_QUERY =
            "DELETE FROM review_likes WHERE review_id = ? AND user_id = ? AND is_useful = ?";

    private final JdbcTemplate jdbc;
    private final ReviewRowMapper mapper;

    @Autowired
    public ReviewDbStorage(JdbcTemplate jdbc, ReviewRowMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    public Review add(Review review) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, review.getContent());
            ps.setBoolean(2, review.getIsPositive());
            ps.setLong(3, review.getUserId());
            ps.setLong(4, review.getFilmId());
            return ps;
        }, keyHolder);
        return findById(keyHolder.getKeyAs(Long.class)).orElseThrow();
    }

    @Override
    public Review update(Review review) {
        jdbc.update(UPDATE_QUERY, review.getContent(), review.getIsPositive(), review.getReviewId());
        return findById(review.getReviewId()).orElseThrow();
    }

    @Override
    public void delete(long id) {
        jdbc.update(DELETE_QUERY, id);
    }

    @Override
    public Optional<Review> findById(long id) {
        return jdbc.query(FIND_BY_ID_QUERY, mapper, id).stream().findFirst();
    }

    @Override
    public List<Review> findMostUseful(Long filmId, int count) {
        if (filmId == null) {
            return jdbc.query(FIND_ALL_QUERY, mapper, count);
        }
        return jdbc.query(FIND_BY_FILM_QUERY, mapper, filmId, count);
    }

    @Override
    public void addRating(long reviewId, long userId, boolean isUseful) {
        jdbc.update(ADD_RATING_QUERY, reviewId, userId, isUseful);
    }

    @Override
    public void removeRating(long reviewId, long userId, boolean isUseful) {
        jdbc.update(REMOVE_RATING_QUERY, reviewId, userId, isUseful);
    }
}
