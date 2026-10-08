package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.service.DirectorService;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/directors")
public class DirectorController {

    private final DirectorService directorService;

    @Autowired
    public DirectorController(DirectorService directorService) {
        this.directorService = directorService;
    }

    @GetMapping
    public List<Director> getDirectors() {
        log.info("GET /directors");
        return directorService.getAllDirectors();
    }

    @GetMapping("/{id}")
    public Director getDirectorById(@PathVariable Long id) {
        log.info("GET /directors/{}", id);
        return directorService.getDirectorById(id);
    }

    @PostMapping
    public Director createDirector(@RequestBody Director director) {
        log.info("POST /directors, body:{}", director);
        return directorService.addDirector(director);
    }

    @PutMapping
    public Director updateDirector(@RequestBody Director director) {
        log.info("PUT /directors, body:{}", director);
        return directorService.updateDirector(director);
    }

    @DeleteMapping("/{id}")
    public void deleteDirector(@PathVariable Long id) {
        log.info("DELETE /directors/{}", id);
        directorService.deleteDirector(id);
    }

}
