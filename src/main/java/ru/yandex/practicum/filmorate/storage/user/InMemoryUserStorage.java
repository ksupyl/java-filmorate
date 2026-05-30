package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.HashMap;
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
        User user = findById(id);
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
}