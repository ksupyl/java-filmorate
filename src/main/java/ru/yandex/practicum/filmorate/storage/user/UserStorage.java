package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.Optional;

public interface UserStorage {

    User add(User user);

    User update(User user);

    User delete(long id);

    Collection<User> findAll();

    Optional<User> findById(long id);

    void addFriend(long userId, long friendId);

    void removeFriend(long userId, long friendId);

    Collection<User> findFriends(long userId);

    Collection<User> findCommonFriends(long userId, long otherId);
}