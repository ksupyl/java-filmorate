package ru.yandex.practicum.filmorate.storage.feed;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.mapper.EventRowMapper;
import ru.yandex.practicum.filmorate.storage.mapper.UserRowMapper;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import({FeedDbStorage.class, EventRowMapper.class, UserDbStorage.class, UserRowMapper.class})
class FeedDbStorageTest {

    private final FeedDbStorage feedStorage;
    private final UserDbStorage userStorage;

    @Autowired
    FeedDbStorageTest(FeedDbStorage feedStorage, UserDbStorage userStorage) {
        this.feedStorage = feedStorage;
        this.userStorage = userStorage;
    }

    private User createUser(String login) {
        User user = new User();
        user.setEmail(login + "@mail.ru");
        user.setLogin(login);
        user.setName("Пользователь " + login);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return userStorage.add(user);
    }

    private Event addEvent(long userId, EventType eventType, Operation operation, long entityId) {
        Event event = new Event();
        event.setTimestamp(1_700_000_000_000L);
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setOperation(operation);
        event.setEntityId(entityId);
        return feedStorage.add(event);
    }

    @Test
    void shouldReturnUserEventsInOrderOfAdding() {
        User anna = createUser("anna");
        User boris = createUser("boris");
        Event first = addEvent(anna.getId(), EventType.FRIEND, Operation.ADD, boris.getId());
        addEvent(boris.getId(), EventType.LIKE, Operation.ADD, 1);
        Event second = addEvent(anna.getId(), EventType.REVIEW, Operation.UPDATE, 5);

        assertNotNull(first.getEventId());
        assertEquals(List.of(first, second), feedStorage.findByUserId(anna.getId()));
    }

    @Test
    void shouldReturnEmptyFeedForUserWithoutEvents() {
        assertTrue(feedStorage.findByUserId(createUser("anna").getId()).isEmpty());
    }

    @Test
    void shouldDeleteEventsWithUser() {
        User anna = createUser("anna");
        addEvent(anna.getId(), EventType.LIKE, Operation.ADD, 1);

        userStorage.delete(anna.getId());

        assertTrue(feedStorage.findByUserId(anna.getId()).isEmpty());
    }
}
