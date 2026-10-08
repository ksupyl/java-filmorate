package ru.yandex.practicum.filmorate.storage.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.mapper.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.mapper.GenreRowMapper;
import ru.yandex.practicum.filmorate.storage.mapper.UserRowMapper;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Поднимается только база и JdbcTemplate, хранилище и маппер подключаем сами
// FilmDbStorage нужен для лайков, на которых строятся рекомендации.
@JdbcTest
@AutoConfigureTestDatabase
@Import({UserDbStorage.class, UserRowMapper.class, FilmRowMapper.class,
        FilmDbStorage.class, GenreRowMapper.class})
class UserDbStorageTest {

    private final UserDbStorage userStorage;
    private final FilmDbStorage filmStorage;

    @Autowired
    UserDbStorageTest(UserDbStorage userStorage, FilmDbStorage filmStorage) {
        this.userStorage = userStorage;
        this.filmStorage = filmStorage;
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

    private Film createFilm(String name) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        return filmStorage.add(film);
    }

    private void likeFilm(Film film, User user) {
        filmStorage.addLike(film.getId(), user.getId());
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

    // Без ON DELETE CASCADE удаление упало бы на внешнем ключе friendship
    @Test
    void shouldDeleteUserWithFriendships() {
        User anna = createUser("anna");
        User boris = createUser("boris");
        userStorage.addFriend(anna.getId(), boris.getId());
        userStorage.addFriend(boris.getId(), anna.getId());

        userStorage.delete(boris.getId());

        assertTrue(userStorage.findById(boris.getId()).isEmpty());
        assertTrue(userStorage.findFriends(anna.getId()).isEmpty());
    }

    @Test
    void shouldThrowWhenDeletingUnknownUser() {
        assertThrows(NotFoundException.class, () -> userStorage.delete(9999));
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

    @Test
    void shouldReturnRecommendationsFromMostSimilarUser() {
        User anna = createUser("anna");
        User boris = createUser("boris");
        Film first = createFilm("Первый");
        Film second = createFilm("Второй");
        userStorage.addFriend(anna.getId(), boris.getId());
        likeFilm(first, anna);
        likeFilm(first, boris);
        likeFilm(second, boris);

        List<Film> recommendations = userStorage.findRecommendations(anna.getId());

        assertEquals(List.of(second.getId()),
                recommendations.stream().map(Film::getId).toList());
    }

    @Test
    void shouldReturnEmptyRecommendationsWhenNoSimilarUsers() {
        User anna = createUser("anna");
        createUser("boris");
        Film film = createFilm("Фильм");
        likeFilm(film, anna);

        assertTrue(userStorage.findRecommendations(anna.getId()).isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenSimilarUserLikedNothingNew() {
        User anna = createUser("anna");
        User boris = createUser("boris");
        Film film = createFilm("Фильм");
        likeFilm(film, anna);
        likeFilm(film, boris);

        assertTrue(userStorage.findRecommendations(anna.getId()).isEmpty());
    }

    @Test
    void shouldPickUserWithLargestOverlap() {
        User anna = createUser("anna");
        User boris = createUser("boris");
        User vera = createUser("vera");
        Film first = createFilm("Первый");
        Film second = createFilm("Второй");
        Film third = createFilm("Третий");

        likeFilm(first, anna);
        likeFilm(second, anna);
        likeFilm(first, boris);
        likeFilm(second, boris);
        likeFilm(third, boris);
        likeFilm(first, vera);

        List<Film> recommendations = userStorage.findRecommendations(anna.getId());

        assertEquals(List.of(third.getId()),
                recommendations.stream().map(Film::getId).toList());
    }
}