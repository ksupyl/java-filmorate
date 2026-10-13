package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.storage.feed.FeedStorage;

import java.time.Instant;
import java.util.List;

@Service
public class FeedService {

    private final FeedStorage feedStorage;

    @Autowired
    public FeedService(FeedStorage feedStorage) {
        this.feedStorage = feedStorage;
    }

    public void addEvent(long userId, EventType eventType, Operation operation, long entityId) {
        Event event = new Event();
        event.setTimestamp(Instant.now().toEpochMilli());
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setOperation(operation);
        event.setEntityId(entityId);
        feedStorage.add(event);
    }

    public List<Event> findByUserId(long userId) {
        return feedStorage.findByUserId(userId);
    }
}
