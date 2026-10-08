package ru.yandex.practicum.filmorate.storage.review;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.mapper.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.mapper.GenreRowMapper;
import ru.yandex.practicum.filmorate.storage.mapper.ReviewRowMapper;
import ru.yandex.practicum.filmorate.storage.mapper.UserRowMapper;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import({ReviewDbStorage.class, ReviewRowMapper.class, FilmDbStorage.class, FilmRowMapper.class,
        GenreRowMapper.class, UserDbStorage.class, UserRowMapper.class})
class ReviewDbStorageTest {

    private final ReviewDbStorage reviewStorage;
    private final FilmDbStorage filmStorage;
    private final UserDbStorage userStorage;

    private User author;
    private Film film;

    @Autowired
    ReviewDbStorageTest(ReviewDbStorage reviewStorage, FilmDbStorage filmStorage, UserDbStorage userStorage) {
        this.reviewStorage = reviewStorage;
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    @BeforeEach
    void setUp() {
        author = createUser("author");
        film = createFilm("Фильм");
    }

    private User createUser(String login) {
        User user = new User();
        user.setEmail(login + "@mail.ru");
        user.setLogin(login);
        user.setName("Пользователь " + login);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return userStorage.add(user);
    }

    private Film createFilm(String name) {
        Film newFilm = new Film();
        newFilm.setName(name);
        newFilm.setDescription("Описание");
        newFilm.setReleaseDate(LocalDate.of(2000, 1, 1));
        newFilm.setDuration(120);
        return filmStorage.add(newFilm);
    }

    private Review createReview(long filmId, String content) {
        Review review = new Review();
        review.setContent(content);
        review.setIsPositive(true);
        review.setUserId(author.getId());
        review.setFilmId(filmId);
        return reviewStorage.add(review);
    }

    @Test
    void shouldAddReviewAndFindById() {
        Review review = createReview(film.getId(), "Отличный фильм");

        assertNotNull(review.getReviewId());
        assertEquals(0, review.getUseful());
        assertEquals(review, reviewStorage.findById(review.getReviewId()).orElseThrow());
    }

    @Test
    void shouldReturnEmptyWhenReviewNotFound() {
        assertTrue(reviewStorage.findById(9999).isEmpty());
    }

    @Test
    void shouldUpdateOnlyContentAndType() {
        Review review = createReview(film.getId(), "Отличный фильм");
        User other = createUser("other");
        Review changes = new Review();
        changes.setReviewId(review.getReviewId());
        changes.setContent("Так себе фильм");
        changes.setIsPositive(false);
        changes.setUserId(other.getId());
        changes.setFilmId(createFilm("Другой").getId());

        Review updated = reviewStorage.update(changes);

        assertEquals("Так себе фильм", updated.getContent());
        assertFalse(updated.getIsPositive());
        assertEquals(author.getId(), updated.getUserId());
        assertEquals(film.getId(), updated.getFilmId());
    }

    @Test
    void shouldDeleteReview() {
        Review review = createReview(film.getId(), "Отличный фильм");

        reviewStorage.delete(review.getReviewId());

        assertTrue(reviewStorage.findById(review.getReviewId()).isEmpty());
    }

    @Test
    void shouldCountUsefulFromLikesAndDislikes() {
        Review review = createReview(film.getId(), "Отличный фильм");
        long reviewId = review.getReviewId();
        reviewStorage.addRating(reviewId, createUser("first").getId(), true);
        reviewStorage.addRating(reviewId, createUser("second").getId(), true);
        reviewStorage.addRating(reviewId, createUser("third").getId(), false);

        assertEquals(1, reviewStorage.findById(reviewId).orElseThrow().getUseful());
    }

    @Test
    void shouldReplaceLikeWithDislike() {
        Review review = createReview(film.getId(), "Отличный фильм");
        long reviewId = review.getReviewId();
        long userId = createUser("rater").getId();
        reviewStorage.addRating(reviewId, userId, true);

        reviewStorage.addRating(reviewId, userId, false);

        assertEquals(-1, reviewStorage.findById(reviewId).orElseThrow().getUseful());
    }

    @Test
    void shouldRemoveOnlyMatchingRating() {
        Review review = createReview(film.getId(), "Отличный фильм");
        long reviewId = review.getReviewId();
        long userId = createUser("rater").getId();
        reviewStorage.addRating(reviewId, userId, true);

        reviewStorage.removeRating(reviewId, userId, false);
        assertEquals(1, reviewStorage.findById(reviewId).orElseThrow().getUseful());

        reviewStorage.removeRating(reviewId, userId, true);
        assertEquals(0, reviewStorage.findById(reviewId).orElseThrow().getUseful());
    }

    @Test
    void shouldSortByUsefulAndFilterByFilm() {
        Review first = createReview(film.getId(), "Первый");
        Review second = createReview(film.getId(), "Второй");
        Review third = createReview(film.getId(), "Третий");
        Review otherFilmReview = createReview(createFilm("Другой").getId(), "Про другой фильм");
        reviewStorage.addRating(second.getReviewId(), createUser("rater").getId(), true);
        reviewStorage.addRating(first.getReviewId(), createUser("hater").getId(), false);

        List<Long> byFilm = reviewStorage.findMostUseful(film.getId(), 10).stream()
                .map(Review::getReviewId)
                .toList();
        assertEquals(List.of(second.getReviewId(), third.getReviewId(), first.getReviewId()), byFilm);

        assertEquals(4, reviewStorage.findMostUseful(null, 10).size());
        assertEquals(List.of(second.getReviewId(), third.getReviewId()),
                reviewStorage.findMostUseful(null, 2).stream().map(Review::getReviewId).toList());
        assertTrue(reviewStorage.findMostUseful(film.getId(), 10).stream()
                .noneMatch(review -> review.getReviewId().equals(otherFilmReview.getReviewId())));
    }

    @Test
    void shouldDeleteReviewsWithFilm() {
        Review review = createReview(film.getId(), "Отличный фильм");

        filmStorage.delete(film.getId());

        assertTrue(reviewStorage.findById(review.getReviewId()).isEmpty());
    }
}
