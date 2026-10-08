package ru.yandex.practicum.filmorate.storage.review;

import ru.yandex.practicum.filmorate.model.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewStorage {

    Review add(Review review);

    Review update(Review review);

    void delete(long id);

    Optional<Review> findById(long id);

    List<Review> findMostUseful(Long filmId, int count);

    void addRating(long reviewId, long userId, boolean isUseful);

    void removeRating(long reviewId, long userId, boolean isUseful);
}
