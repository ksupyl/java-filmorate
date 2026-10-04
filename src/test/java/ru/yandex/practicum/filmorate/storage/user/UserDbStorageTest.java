package ru.yandex.practicum.filmorate.storage.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.mapper.UserRowMapper;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Поднимается только база и JdbcTemplate, хранилище и маппер подключаем сами
@JdbcTest
@AutoConfigureTestDatabase
@Import({UserDbStorage.class, UserRowMapper.class})
class UserDbStorageTest {

    private final UserDbStorage userStorage;

    @Autowired
    UserDbStorageTest(UserDbStorage userStorage) {
        this.userStorage = userStorage;
    }

    // Пользователей в тестовой базе нет: создаём сами, id берём из того, что вернул add
    private User createUser(String login) {
        User user = new User();
        user.setEmail(login + "@mail.ru");
        user.setLogin(login);
        user.setName("Пользователь " + login);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return userStorage.add(user);
    }

    @Test
    void shouldAddUserAndFindById() {
        User user = createUser("anna");

        assertNotNull(user.getId());
        // @Data сравнивает объекты по всем полям — одна строка проверяет всё
        assertEquals(user, userStorage.findById(user.getId()).orElseThrow());
    }

    @Test
    void shouldReturnEmptyWhenUserNotFound() {
        assertTrue(userStorage.findById(9999).isEmpty());
    }

    @Test
    void shouldFindAllUsers() {
        createUser("anna");
        createUser("boris");

        assertEquals(2, userStorage.findAll().size());
    }

    @Test
    void shouldUpdateUser() {
        User user = createUser("anna");
        user.setName("Анна Каренина");
        user.setEmail("karenina@mail.ru");

        userStorage.update(user);

        User updated = userStorage.findById(user.getId()).orElseThrow();
        assertEquals("Анна Каренина", updated.getName());
        assertEquals("karenina@mail.ru", updated.getEmail());
    }

    @Test
    void shouldDeleteUser() {
        User user = createUser("anna");

        userStorage.delete(user.getId());

        assertTrue(userStorage.findById(user.getId()).isEmpty());
    }

    @Test
    void shouldAddFriendOneWay() {
        User anna = createUser("anna");
        User boris = createUser("boris");

        userStorage.addFriend(anna.getId(), boris.getId());

        assertEquals(List.of(boris), userStorage.findFriends(anna.getId()));
        assertTrue(userStorage.findFriends(boris.getId()).isEmpty());
    }

    @Test
    void shouldRemoveFriend() {
        User anna = createUser("anna");
        User boris = createUser("boris");
        userStorage.addFriend(anna.getId(), boris.getId());

        userStorage.removeFriend(anna.getId(), boris.getId());

        assertTrue(userStorage.findFriends(anna.getId()).isEmpty());
    }

    @Test
    void shouldFindCommonFriends() {
        User anna = createUser("anna");
        User boris = createUser("boris");
        User common = createUser("common");
        userStorage.addFriend(anna.getId(), common.getId());
        userStorage.addFriend(boris.getId(), common.getId());

        assertEquals(List.of(common), userStorage.findCommonFriends(anna.getId(), boris.getId()));
    }
}