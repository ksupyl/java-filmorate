package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;

@Service
public class DirectorService {

    private final DirectorStorage directorStorage;

    public DirectorService(@Qualifier("directorDbStorage") DirectorStorage directorStorage) {
        this.directorStorage = directorStorage;
    }

    public Director addDirector(Director director) {
        return directorStorage.addDirector(director);
    }

    public Director updateDirector(Director director) {
        return directorStorage.updateDirector(director);
    }

    public void deleteDirector(long id) {
        directorStorage.removeDirector(id);
    }

    public Director getDirectorById(long id) {
        return directorStorage.findDirectorById(id)
                .orElseThrow(() -> new NotFoundException("Режиссер с id " + id + " не найден"));
    }

    public java.util.List<Director> getAllDirectors() {
        return directorStorage.findAllDirectors();
    }

}
