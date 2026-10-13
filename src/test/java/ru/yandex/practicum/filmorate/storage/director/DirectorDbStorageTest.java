package ru.yandex.practicum.filmorate.storage.director;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.mapper.DirectorRowMapper;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import({DirectorDbStorage.class, DirectorRowMapper.class})
public class DirectorDbStorageTest {

    private final DirectorDbStorage directorDbStorage;

    @Autowired
    public DirectorDbStorageTest(DirectorDbStorage directorDbStorage) {
        this.directorDbStorage = directorDbStorage;
    }

    private Director createDirector(String directorName) {
        Director director = new Director();
        director.setName(directorName);
        return director;
    }

    @Test
    public void addDirector() {
        Director director = createDirector("name");
        directorDbStorage.addDirector(director);
        assertEquals("name", director.getName());
        assertNotNull(director.getId());
    }

    @Test
    public void getDirectorById() {
        Director director = createDirector("director");
        Director createdDirector = directorDbStorage.addDirector(director);
        Director retrievedDirector = directorDbStorage.findDirectorById(createdDirector.getId()).orElse(null);
        assertEquals(createdDirector.getId(), retrievedDirector.getId());
        assertEquals(createdDirector.getName(), retrievedDirector.getName());
    }

    @Test
    public void deleteDirectorById() {
        Director director = createDirector("director");
        directorDbStorage.addDirector(director);
        directorDbStorage.removeDirector(director.getId());
        assertTrue(directorDbStorage.findDirectorById(director.getId()).isEmpty());
    }

    @Test
    public void updateDirectorById() {
        Director director = createDirector("director");
        directorDbStorage.addDirector(director);
        director.setName("updated name");
        directorDbStorage.updateDirector(director);
        Director updatedDirector = directorDbStorage.findDirectorById(director.getId()).orElse(null);
        assertEquals("updated name", updatedDirector.getName());
    }

    @Test
    public void getAllDirectors() {
        Director director = createDirector("director");
        Director director2 = createDirector("director2");
        directorDbStorage.addDirector(director);
        directorDbStorage.addDirector(director2);
        List<Director> allDirectors = directorDbStorage.findAllDirectors();
        assertEquals(2, allDirectors.size());
    }
}
