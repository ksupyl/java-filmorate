package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {

    // Хранилище пользователей в памяти
    private final Map<Long, User> users = new HashMap<>();

    private long currentId = 1;

    @GetMapping
    public Collection<User> findAll() {
        log.info("Получен запрос на получение списка всех пользователей");
        return users.values();
    }

    @PostMapping
    public User create(@Valid @RequestBody User user) {
        log.info("Получен запрос на создание пользователя: {}", user.getLogin());

        validateUser(user);

        user.setId(getNextId());
        users.put(user.getId(), user);

        log.info("Пользователь успешно добавлен. Присвоен ID: {}", user.getId());
        return user;
    }

    @PutMapping
    public User update(@Valid @RequestBody User user) {
        log.info("Получен запрос на обновление пользователя с ID: {}", user.getId());

        if (user.getId() == null || !users.containsKey(user.getId())) {
            log.error("Ошибка обновления: пользователь с ID {} не найден", user.getId());
            throw new ValidationException("Пользователь с таким ID не найден");
        }

        validateUser(user);

        users.put(user.getId(), user);
        log.info("Пользователь с ID {} успешно обновлен", user.getId());
        return user;
    }

    // Вспомогательный метод для валидации и бизнес-логики
    private void validateUser(User user) {
        // Проверка на пробелы в логине
        if (user.getLogin().contains(" ")) {
            log.error("Ошибка валидации: логин '{}' содержит пробелы", user.getLogin());
            throw new ValidationException("Логин не может содержать пробелы");
        }

        // Если имя пустое = использование логина
        if (user.getName() == null || user.getName().isBlank()) {
            log.info("Имя пользователя не указано. Используем логин: {}", user.getLogin());
            user.setName(user.getLogin());
        }
    }

    // Вспомогательный метод для генерации ID
    private long getNextId() {
        return currentId++;
    }
}
