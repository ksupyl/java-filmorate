package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class InMemoryUserStorage implements UserStorage {

    private final Map<Long, User> users = new HashMap<>();
    private long currentId = 1;

    @Override
    public User add(User user) {
        user.setId(currentId++);
        users.put(user.getId(), user);
        log.debug("Добавлен пользователь: id={}, login={}", user.getId(), user.getLogin());
        return user;
    }

    @Override
    public User update(User user) {

        users.put(user.getId(), user);
        log.debug("Обновлён пользователь: id={}", user.getId());
        return user;
    }

    @Override
    public User delete(long id) {
        User user = findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));

        users.remove(id);
        log.debug("Удалён пользователь: id={}", id);
        return user;
    }

    @Override
    public Collection<User> findAll() {
        return users.values();
    }

    @Override
    public Optional<User> findById(long id) {
        return Optional.ofNullable(users.get(id));
    }

    // Существование пользователей проверяет сервис до вызова этих методов
    @Override
    public void addFriend(long userId, long friendId) {
        users.get(userId).addFriend(friendId);
    }

    @Override
    public void removeFriend(long userId, long friendId) {
        users.get(userId).removeFriend(friendId);
    }

    @Override
    public Collection<User> findFriends(long userId) {
        User user = users.get(userId);
        List<User> friends = new ArrayList<>();

        for (Long friendId : user.getFriends()) {
            friends.add(users.get(friendId));
        }

        return friends;
    }

    @Override
    public Collection<User> findCommonFriends(long userId, long otherId) {
        User user = users.get(userId);
        User other = users.get(otherId);
        List<User> common = new ArrayList<>();

        for (Long friendId : user.getFriends()) {
            if (other.getFriends().contains(friendId)) {
                common.add(users.get(friendId));
            }
        }

        return common;
    }
}