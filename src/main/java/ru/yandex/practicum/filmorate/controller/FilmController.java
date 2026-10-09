package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {

    private final FilmService filmService;

    @Autowired
    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @GetMapping
    public Collection<Film> findAll() {
        log.info("GET /films");
        return filmService.findAll();
    }

    @GetMapping("/{id}")
    public Film findById(@PathVariable long id) {
        log.info("GET /films/{}", id);
        return filmService.findById(id);
    }

    @PostMapping
    public Film create(@Valid @RequestBody Film film) {
        log.info("POST /films : {}", film.getName());
        return filmService.add(film);
    }

    @PutMapping
    public Film update(@Valid @RequestBody Film film) {
        log.info("PUT /films : id={}", film.getId());
        return filmService.update(film);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id) {
        log.info("DELETE /films/{}", id);
        filmService.delete(id);
    }

    @PutMapping("/{id}/like/{userId}")
    public Film addLike(@PathVariable long id, @PathVariable long userId) {
        log.info("PUT /films/{}/like/{}", id, userId);
        return filmService.addLike(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public Film removeLike(@PathVariable long id, @PathVariable long userId) {
        log.info("DELETE /films/{}/like/{}", id, userId);
        return filmService.removeLike(id, userId);
    }

    @GetMapping("/popular")
    public Collection<Film> getPopular(
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(required = false) Integer genreId,
            @RequestParam(required = false) Integer year) {
        log.info("GET /films/popular?count={}&genreId={}&year={}", count, genreId, year);
        return filmService.getPopular(count, genreId, year);
    }

    @GetMapping("/common")
    public Collection<Film> getCommon(@RequestParam long userId, @RequestParam long friendId) {
        log.info("GET /films/common?userId={}&friendId={}", userId, friendId);
        return filmService.getCommon(userId, friendId);
    }

    @GetMapping("/director/{directorId}")
    public Collection<Film> getFilmsByDirector(@PathVariable long directorId,
                                    @RequestParam (defaultValue = "likes") String sortBy) {
        log.info("GET /films/director/{}?sortBy={}", directorId, sortBy);
        return filmService.findByDirectorSorted(directorId, sortBy);
    }

    @GetMapping("/search")
    public Collection<Film> searchFilms(@RequestParam String query,
                                        @RequestParam List<String> by) {
        log.info("GET /films/search?query={}&by={}", query, by);
        return filmService.searchFilms(query, by);
    }
}