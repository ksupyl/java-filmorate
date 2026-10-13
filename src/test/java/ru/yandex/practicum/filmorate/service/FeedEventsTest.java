package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class FeedEventsTest {

    private final UserService userService;
    private final FilmService filmService;
    private final ReviewService reviewService;

    @Autowired
    FeedEventsTest(UserService userService, FilmService filmService, ReviewService reviewService) {
        this.userService = userService;
        this.filmService = filmService;
        this.reviewService = reviewService;
    }

    private User createUser(String login) {
        User user = new User();
        user.setEmail(login + "@mail.ru");
        user.setLogin(login);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return userService.add(user);
    }

    private Film createFilm() {
        Film film = new Film();
        film.setName("Фильм");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        return filmService.add(film);
    }

    @Test
    void shouldRecordUserActionsInFeed() {
        long userId = createUser("anna").getId();
        long friendId = createUser("boris").getId();
        long filmId = createFilm().getId();

        userService.addFriend(userId, friendId);
        userService.removeFriend(userId, friendId);
        Review review = new Review();
        review.setContent("Отличный фильм");
        review.setIsPositive(true);
        review.setUserId(userId);
        review.setFilmId(filmId);
        long reviewId = reviewService.add(review).getReviewId();
        review.setReviewId(reviewId);
        review.setContent("Так себе фильм");
        reviewService.update(review);
        reviewService.delete(reviewId);
        filmService.addLike(filmId, userId);
        filmService.removeLike(filmId, userId);

        List<Event> feed = userService.getFeed(userId);

        assertEquals(List.of(
                List.of(EventType.FRIEND, Operation.ADD, friendId),
                List.of(EventType.FRIEND, Operation.REMOVE, friendId),
                List.of(EventType.REVIEW, Operation.ADD, reviewId),
                List.of(EventType.REVIEW, Operation.UPDATE, reviewId),
                List.of(EventType.REVIEW, Operation.REMOVE, reviewId),
                List.of(EventType.LIKE, Operation.ADD, filmId),
                List.of(EventType.LIKE, Operation.REMOVE, filmId)
        ), feed.stream().map(e -> List.of(e.getEventType(), e.getOperation(), e.getEntityId())).toList());
        assertTrue(feed.stream().allMatch(e -> e.getUserId() == userId && e.getTimestamp() > 0));
        assertTrue(userService.getFeed(friendId).isEmpty());
    }

    @Test
    void shouldNotRecordReviewRatings() {
        long authorId = createUser("anna").getId();
        long raterId = createUser("boris").getId();
        Review review = new Review();
        review.setContent("Отличный фильм");
        review.setIsPositive(true);
        review.setUserId(authorId);
        review.setFilmId(createFilm().getId());
        long reviewId = reviewService.add(review).getReviewId();

        reviewService.addLike(reviewId, raterId);
        reviewService.addDislike(reviewId, raterId);

        assertTrue(userService.getFeed(raterId).isEmpty());
    }

    @Test
    void shouldThrowWhenFeedOfUnknownUser() {
        assertThrows(NotFoundException.class, () -> userService.getFeed(9999));
    }
}
