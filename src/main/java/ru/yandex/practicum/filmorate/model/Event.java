package ru.yandex.practicum.filmorate.model;

import lombok.Data;

@Data
public class Event {
    private Long eventId;
    private Long timestamp;
    private Long userId;
    private EventType eventType;
    private Operation operation;
    private Long entityId;
}
