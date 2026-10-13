package ru.yandex.practicum.filmorate.storage.feed;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.storage.mapper.EventRowMapper;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class FeedDbStorage implements FeedStorage {

    private static final String INSERT_QUERY =
            "INSERT INTO feed_events (user_id, event_type, operation, entity_id, created_at) VALUES (?, ?, ?, ?, ?)";
    private static final String FIND_BY_USER_QUERY =
            "SELECT event_id, user_id, event_type, operation, entity_id, created_at FROM feed_events "
                    + "WHERE user_id = ? ORDER BY event_id";

    private final JdbcTemplate jdbc;
    private final EventRowMapper mapper;

    @Autowired
    public FeedDbStorage(JdbcTemplate jdbc, EventRowMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    public Event add(Event event) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, event.getUserId());
            ps.setString(2, event.getEventType().name());
            ps.setString(3, event.getOperation().name());
            ps.setLong(4, event.getEntityId());
            ps.setLong(5, event.getTimestamp());
            return ps;
        }, keyHolder);
        event.setEventId(keyHolder.getKeyAs(Long.class));
        return event;
    }

    @Override
    public List<Event> findByUserId(long userId) {
        return jdbc.query(FIND_BY_USER_QUERY, mapper, userId);
    }
}
