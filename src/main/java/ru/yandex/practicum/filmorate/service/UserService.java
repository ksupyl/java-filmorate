package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@Slf4j
public class UserService {

    private final UserStorage userStorage;

    @Autowired
    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    private void validateDifferentUsers(long userId, long otherId) {
        if (userId == otherId) {
            throw new ValidationException(
                    "Идентификаторы пользователей должны различаться, получено: id=" + userId
            );
        }
    }

    // Получение пользователя по id с проверкой существования
    private User getUserOrThrow(long id) {
        return userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));
    }

    public User addFriend(long userId, long friendId) {
        validateDifferentUsers(userId, friendId);

        User user = getUserOrThrow(userId);
        User friend = getUserOrThrow(friendId);

        user.addFriend(friendId);
        friend.addFriend(userId);

        log.debug("Пользователи id={} и id={} теперь друзья", userId, friendId);
        return user;
    }

    public User removeFriend(long userId, long friendId) {
        validateDifferentUsers(userId, friendId);

        User user = getUserOrThrow(userId);
        User friend = getUserOrThrow(friendId);

        user.removeFriend(friendId);
        friend.removeFriend(userId);

        log.debug("Пользователи id={} и id={} больше не друзья", userId, friendId);
        return user;
    }

    public Collection<User> getFriends(long userId) {
        User user = getUserOrThrow(userId);
        List<User> friends = new ArrayList<>();

        for (Long friendId : user.getFriends()) {
            friends.add(getUserOrThrow(friendId));
        }

        return friends;
    }

    public Collection<User> getCommonFriends(long userId, long otherId) {
        validateDifferentUsers(userId, otherId);

        User user = getUserOrThrow(userId);
        User other = getUserOrThrow(otherId);
        List<User> common = new ArrayList<>();

        for (Long friendId : user.getFriends()) {
            if (other.getFriends().contains(friendId)) {
                common.add(getUserOrThrow(friendId));
            }
        }

        return common;
    }

    // Логин не может содержать пробелы
    private void validateLogin(User user) {
        if (user.getLogin().contains(" ")) {
            log.warn("Некорректный логин: '{}'", user.getLogin());
            throw new ValidationException("Логин не может содержать пробелы");
        }
    }

    private void applyDefaultName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.debug("Имя пользователя не задано — используется логин: '{}'", user.getLogin());
        }
    }

    // Делегирование базовых операций хранилищу
    public User add(User user) {
        validateLogin(user);
        applyDefaultName(user);
        return userStorage.add(user);
    }

    public User update(User user) {
        getUserOrThrow(user.getId());

        validateLogin(user);
        applyDefaultName(user);
        return userStorage.update(user);
    }

    public Collection<User> findAll() {
        return userStorage.findAll();
    }

    public User findById(long id) {
        return getUserOrThrow(id);
    }
}