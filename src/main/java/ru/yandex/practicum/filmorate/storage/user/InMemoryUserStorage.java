package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class InMemoryUserStorage implements UserStorage {

    private final Map<Long, User> users = new HashMap<>();
    private long currentId = 1;

    @Override
    public User add(User user) {
        // Если имя пустое — используем логин (бизнес-правило из прошлого спринта)
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        user.setId(currentId++);
        users.put(user.getId(), user);
        log.debug("Добавлен пользователь: id={}, login={}", user.getId(), user.getLogin());
        return user;
    }

    @Override
    public User update(User user) {
        if (!users.containsKey(user.getId())) {
            log.warn("Попытка обновить несуществующего пользователя: id={}", user.getId());
            throw new NotFoundException("Пользователь с id=" + user.getId() + " не найден");
        }

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
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
    public User findById(long id) {
        User user = users.get(id);
        if (user == null) {
            log.warn("Пользователь не найден: id={}", id);
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }
        return user;
    }
}