package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.review.ReviewStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;

@Service
@Slf4j
public class ReviewService {

    private final ReviewStorage reviewStorage;
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final FeedService feedService;

    @Autowired
    public ReviewService(ReviewStorage reviewStorage,
                         @Qualifier("filmDbStorage") FilmStorage filmStorage,
                         @Qualifier("userDbStorage") UserStorage userStorage,
                         FeedService feedService) {
        this.reviewStorage = reviewStorage;
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.feedService = feedService;
    }

    private Review getReviewOrThrow(long id) {
        return reviewStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Отзыв с id=" + id + " не найден"));
    }

    private void checkUserExists(long id) {
        userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));
    }

    private void checkFilmExists(long id) {
        filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + id + " не найден"));
    }

    public Review add(Review review) {
        checkUserExists(review.getUserId());
        checkFilmExists(review.getFilmId());
        Review created = reviewStorage.add(review);
        feedService.addEvent(created.getUserId(), EventType.REVIEW, Operation.ADD, created.getReviewId());
        log.debug("Пользователь id={} оставил отзыв id={} к фильму id={}",
                created.getUserId(), created.getReviewId(), created.getFilmId());
        return created;
    }

    // Автор, фильм и рейтинг полезности не меняются — только текст и тип отзыва
    public Review update(Review review) {
        if (review.getReviewId() == null) {
            throw new ValidationException("Не указан id отзыва");
        }
        getReviewOrThrow(review.getReviewId());
        Review updated = reviewStorage.update(review);
        feedService.addEvent(updated.getUserId(), EventType.REVIEW, Operation.UPDATE, updated.getReviewId());
        return updated;
    }

    public void delete(long id) {
        Review review = getReviewOrThrow(id);
        reviewStorage.delete(id);
        feedService.addEvent(review.getUserId(), EventType.REVIEW, Operation.REMOVE, id);
        log.debug("Отзыв id={} удалён", id);
    }

    public Review findById(long id) {
        return getReviewOrThrow(id);
    }

    public Collection<Review> findMostUseful(Long filmId, int count) {
        if (count <= 0) {
            throw new ValidationException(
                    "Количество отзывов должно быть положительным числом, получено: " + count
            );
        }
        if (filmId != null) {
            checkFilmExists(filmId);
        }
        return reviewStorage.findMostUseful(filmId, count);
    }

    public void addLike(long reviewId, long userId) {
        addRating(reviewId, userId, true);
    }

    public void addDislike(long reviewId, long userId) {
        addRating(reviewId, userId, false);
    }

    public void removeLike(long reviewId, long userId) {
        removeRating(reviewId, userId, true);
    }

    public void removeDislike(long reviewId, long userId) {
        removeRating(reviewId, userId, false);
    }

    // Повторная оценка заменяет предыдущую: лайк после дизлайка меняет рейтинг на +2
    private void addRating(long reviewId, long userId, boolean isUseful) {
        getReviewOrThrow(reviewId);
        checkUserExists(userId);
        reviewStorage.addRating(reviewId, userId, isUseful);
        log.debug("Пользователь id={} оценил отзыв id={}: полезный={}", userId, reviewId, isUseful);
    }

    private void removeRating(long reviewId, long userId, boolean isUseful) {
        getReviewOrThrow(reviewId);
        checkUserExists(userId);
        reviewStorage.removeRating(reviewId, userId, isUseful);
        log.debug("Пользователь id={} убрал оценку отзыва id={}: полезный={}", userId, reviewId, isUseful);
    }
}
