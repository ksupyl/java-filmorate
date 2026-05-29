package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    public User addFriend(long userId, long friendId) {
        User user = userStorage.findById(userId);
        User friend = userStorage.findById(friendId);

        user.getFriends().add(friendId);
        friend.getFriends().add(userId);

        userStorage.update(user);
        userStorage.update(friend);

        log.debug("Пользователи id={} и id={} теперь друзья", userId, friendId);
        return user;
    }

    public User removeFriend(long userId, long friendId) {
        User user = userStorage.findById(userId);
        User friend = userStorage.findById(friendId);

        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);

        userStorage.update(user);
        userStorage.update(friend);

        log.debug("Пользователи id={} и id={} больше не друзья", userId, friendId);
        return user;
    }

    public Collection<User> getFriends(long userId) {
        User user = userStorage.findById(userId);
        List<User> friends = new ArrayList<>();

        for (Long friendId : user.getFriends()) {
            friends.add(userStorage.findById(friendId));
        }

        return friends;
    }

    public Collection<User> getCommonFriends(long userId, long otherId) {
        User user = userStorage.findById(userId);
        User other = userStorage.findById(otherId);
        List<User> common = new ArrayList<>();

        for (Long friendId : user.getFriends()) {
            // Если этот друг есть и у другого пользователя = общий
            if (other.getFriends().contains(friendId)) {
                common.add(userStorage.findById(friendId));
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

    // Делегирование базовых операций хранилищу
    public User add(User user) {
        validateLogin(user);
        return userStorage.add(user);
    }

    public User update(User user) {
        validateLogin(user);
        return userStorage.update(user);
    }

    public Collection<User> findAll() {
        return userStorage.findAll();
    }

    public User findById(long id) {
        return userStorage.findById(id);
    }
}