package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(new InMemoryUserStorage());
    }

    private User createUser(String login) {
        User user = new User();
        user.setEmail(login + "@mail.ru");
        user.setLogin(login);
        user.setName(login);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return userService.add(user);
    }

    @Test
    void shouldAddUser() {
        User user = createUser("testLogin");
        assertNotNull(user.getId());
    }

    @Test
    void shouldThrowWhenLoginContainsSpace() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("bad login");
        user.setName("Test");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        assertThrows(ValidationException.class, () -> userService.add(user));
    }

    @Test
    void shouldReplaceEmptyNameWithLogin() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("testLogin");
        user.setName("");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User saved = userService.add(user);
        assertEquals("testLogin", saved.getName());
    }

    @Test
    void shouldAddAndRemoveFriend() {
        User user1 = createUser("user1");
        User user2 = createUser("user2");

        userService.addFriend(user1.getId(), user2.getId());

        // Оба должны видеть друг друга
        assertTrue(userService.findById(user1.getId()).getFriends().contains(user2.getId()));
        assertTrue(userService.findById(user2.getId()).getFriends().contains(user1.getId()));

        userService.removeFriend(user1.getId(), user2.getId());

        assertFalse(userService.findById(user1.getId()).getFriends().contains(user2.getId()));
        assertFalse(userService.findById(user2.getId()).getFriends().contains(user1.getId()));
    }

    @Test
    void shouldReturnCommonFriends() {
        User user1 = createUser("user1");
        User user2 = createUser("user2");
        User common = createUser("common");

        userService.addFriend(user1.getId(), common.getId());
        userService.addFriend(user2.getId(), common.getId());

        Collection<User> commonFriends = userService.getCommonFriends(
                user1.getId(), user2.getId()
        );

        assertEquals(1, commonFriends.size());
        assertEquals(common.getId(), commonFriends.iterator().next().getId());
    }

    @Test
    void shouldReturnEmptyCommonFriendsWhenNone() {
        User user1 = createUser("user1");
        User user2 = createUser("user2");

        Collection<User> commonFriends = userService.getCommonFriends(
                user1.getId(), user2.getId()
        );

        assertTrue(commonFriends.isEmpty());
    }
}