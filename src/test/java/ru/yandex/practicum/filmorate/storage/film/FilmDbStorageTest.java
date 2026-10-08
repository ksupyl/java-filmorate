package ru.yandex.practicum.filmorate.storage.film;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.storage.director.DirectorDbStorage;
import ru.yandex.practicum.filmorate.storage.mapper.DirectorRowMapper;
import ru.yandex.practicum.filmorate.storage.mapper.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.mapper.GenreRowMapper;
import ru.yandex.practicum.filmorate.storage.mapper.UserRowMapper;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

// Лайкам нужны настоящие пользователи, поэтому подключаем и их хранилище
@JdbcTest
@AutoConfigureTestDatabase
@Import({FilmDbStorage.class, FilmRowMapper.class, GenreRowMapper.class,
        UserDbStorage.class, UserRowMapper.class,
        DirectorDbStorage.class, DirectorRowMapper.class})
class FilmDbStorageTest {

    private final FilmDbStorage filmStorage;
    private final UserDbStorage userStorage;
    private final DirectorDbStorage directorStorage;

    @Autowired
    FilmDbStorageTest(FilmDbStorage filmStorage, UserDbStorage userStorage, DirectorDbStorage directorStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.directorStorage = directorStorage;
    }

    // Фильм без рейтинга и жанров: тесты добавляют их сами, где нужно
    private Film newFilm(String name) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        return film;
    }

    private Film createFilm(String name) {
        return filmStorage.add(newFilm(name));
    }

    private User createUser(String login) {
        User user = new User();
        user.setEmail(login + "@mail.ru");
        user.setLogin(login);
        user.setName("Пользователь " + login);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return userStorage.add(user);
    }

    private static Mpa mpa(int id) {
        Mpa mpa = new Mpa();
        mpa.setId(id);
        return mpa;
    }

    private static Genre genre(int id) {
        Genre genre = new Genre();
        genre.setId(id);
        return genre;
    }

    private static List<String> genreNames(Film film) {
        return film.getGenres().stream().map(Genre::getName).toList();
    }

    private Director createDirector(String directorName) {
        Director director = new Director();
        director.setName(directorName);
        return director;
    }

    @Test
    void shouldAddFilmWithMpaAndGenres() {
        Film film = newFilm("С жанрами");
        film.setMpa(mpa(1));
        // Дубль жанра 3 схлопнется в Set, а в базе жанры встанут по порядку id
        film.getGenres().addAll(List.of(genre(3), genre(1), genre(3)));

        Film saved = filmStorage.add(film);

        Film found = filmStorage.findById(saved.getId()).orElseThrow();
        assertEquals("G", found.getMpa().getName());
        assertEquals(List.of("Комедия", "Мультфильм"), genreNames(found));
    }

    @Test
    void shouldAddFilmWithoutMpaAndGenres() {
        Film film = createFilm("Без рейтинга");

        Film found = filmStorage.findById(film.getId()).orElseThrow();
        assertNull(found.getMpa());
        assertTrue(found.getGenres().isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenFilmNotFound() {
        assertTrue(filmStorage.findById(9999).isEmpty());
    }

    @Test
    void shouldFindAllFilms() {
        createFilm("Первый");
        createFilm("Второй");

        assertEquals(2, filmStorage.findAll().size());
    }

    @Test
    void shouldUpdateFilmAndReplaceGenres() {
        Film film = createFilm("Черновик");
        film.getGenres().add(genre(1));
        filmStorage.update(film);

        film.setName("Чистовик");
        film.getGenres().clear();
        film.getGenres().add(genre(4));
        filmStorage.update(film);

        Film found = filmStorage.findById(film.getId()).orElseThrow();
        assertEquals("Чистовик", found.getName());
        assertEquals(List.of("Триллер"), genreNames(found));
    }

    @Test
    void shouldDeleteFilm() {
        Film film = createFilm("Лишний");

        filmStorage.delete(film.getId());

        assertTrue(filmStorage.findById(film.getId()).isEmpty());
    }

    @Test
    void shouldAddAndRemoveLike() {
        Film film = createFilm("Любимый");
        User user = createUser("anna");

        filmStorage.addLike(film.getId(), user.getId());

        assertTrue(filmStorage.removeLike(film.getId(), user.getId()));  // лайк был
        assertFalse(filmStorage.removeLike(film.getId(), user.getId())); // удалять уже нечего
    }

    @Test
    void shouldFindPopularByLikes() {
        Film first = createFilm("Первый");
        Film second = createFilm("Второй");
        Film third = createFilm("Третий");
        User anna = createUser("anna");
        User boris = createUser("boris");
        // У второго два лайка, у первого один, у третьего ни одного
        filmStorage.addLike(second.getId(), anna.getId());
        filmStorage.addLike(second.getId(), boris.getId());
        filmStorage.addLike(first.getId(), anna.getId());

        List<Film> popular = filmStorage.findPopular(10, null, null);

        assertEquals(List.of(second.getId(), first.getId(), third.getId()),
                popular.stream().map(Film::getId).toList());
        assertEquals(2, filmStorage.findPopular(2, null, null).size());
    }

    @Test
    void shouldFilterPopularByGenre() {
        Film comedy = createFilm("Комедия");
        comedy.getGenres().add(genre(1));
        filmStorage.update(comedy);
        Film drama = createFilm("Драма");
        drama.getGenres().add(genre(2));
        filmStorage.update(drama);
        User anna = createUser("anna");
        filmStorage.addLike(comedy.getId(), anna.getId());
        filmStorage.addLike(drama.getId(), anna.getId());

        List<Film> popular = filmStorage.findPopular(10, 1, null);

        assertEquals(List.of(comedy.getId()), popular.stream().map(Film::getId).toList());
    }

    @Test
    void shouldFilterPopularByYear() {
        Film old = createFilm("Старый");
        Film fresh = newFilm("Новый");
        fresh.setReleaseDate(LocalDate.of(2020, 1, 1));
        fresh = filmStorage.add(fresh);
        User anna = createUser("anna");
        filmStorage.addLike(old.getId(), anna.getId());
        filmStorage.addLike(fresh.getId(), anna.getId());

        List<Film> popular = filmStorage.findPopular(10, null, 2000);

        assertEquals(List.of(old.getId()), popular.stream().map(Film::getId).toList());
    }

    @Test
    void shouldFilterPopularByGenreAndYear() {
        Film comedy2000 = newFilm("Комедия 2000");
        comedy2000.getGenres().add(genre(1));
        comedy2000 = filmStorage.add(comedy2000);
        Film comedy2020 = newFilm("Комедия 2020");
        comedy2020.setReleaseDate(LocalDate.of(2020, 1, 1));
        comedy2020.getGenres().add(genre(1));
        comedy2020 = filmStorage.add(comedy2020);
        User anna = createUser("anna");
        filmStorage.addLike(comedy2000.getId(), anna.getId());
        filmStorage.addLike(comedy2020.getId(), anna.getId());

        List<Film> popular = filmStorage.findPopular(10, 1, 2000);

        assertEquals(List.of(comedy2000.getId()), popular.stream().map(Film::getId).toList());
    }

    @Test
    void shouldReturnAllPopularWhenFiltersAreNull() {
        Film film = createFilm("Один");
        User anna = createUser("anna");
        filmStorage.addLike(film.getId(), anna.getId());

        assertEquals(1, filmStorage.findPopular(10, null, null).size());
    }

    @Test
    void shouldFindCommonFilmsSortedByPopularity() {
        Film first = createFilm("Первый");
        Film second = createFilm("Второй");
        Film onlyAnna = createFilm("Только Анны");
        User anna = createUser("anna");
        User boris = createUser("boris");
        User vera = createUser("vera");
        // Оба лайкнули первый и второй; у второго есть ещё лайк Веры — он популярнее
        filmStorage.addLike(first.getId(), anna.getId());
        filmStorage.addLike(first.getId(), boris.getId());
        filmStorage.addLike(second.getId(), anna.getId());
        filmStorage.addLike(second.getId(), boris.getId());
        filmStorage.addLike(second.getId(), vera.getId());
        // Этот лайкнула только Анна — в общие не попадает
        filmStorage.addLike(onlyAnna.getId(), anna.getId());

        List<Film> common = filmStorage.findCommon(anna.getId(), boris.getId());

        assertEquals(List.of(second.getId(), first.getId()),
                common.stream().map(Film::getId).toList());
    }

    @Test
    void shouldReturnEmptyCommonFilmsWhenNoSharedLikes() {
        Film film = createFilm("Один");
        User anna = createUser("anna");
        User boris = createUser("boris");
        filmStorage.addLike(film.getId(), anna.getId());

        assertEquals(0, filmStorage.findCommon(anna.getId(), boris.getId()).size());
    }

    @Test
    void shouldReturnAllFilmsByDirectorSortedByLikes() {
        Director director = createDirector("Christopher Nolan");
        directorStorage.addDirector(director);
        Film film1 = newFilm("Фильм 1");
        film1.getDirectors().add(director);
        film1 = filmStorage.add(film1);
        Film film2 = newFilm("Фильм 2");
        film2.getDirectors().add(director);
        film2 = filmStorage.add(film2);
        User anna = createUser("anna");
        User boris = createUser("boris");
        filmStorage.addLike(film1.getId(), anna.getId());
        filmStorage.addLike(film2.getId(), anna.getId());
        filmStorage.addLike(film2.getId(), boris.getId());

        List<Film> allFilms = filmStorage.findFilmByDirector(director.getId(), "likes");
        assertEquals(2, allFilms.size());
        assertEquals(film2.getId(), allFilms.get(0).getId());
        assertEquals(film1.getId(), allFilms.get(1).getId());
    }

    @Test
    void shouldReturnAllFilmsByDirectorSortedByYear() {
        Director director = createDirector("Steven Spielberg");
        directorStorage.addDirector(director);
        Film film1 = newFilm("Фильм 1");
        film1.setReleaseDate(LocalDate.of(2000, 1, 1));
        film1.getDirectors().add(director);
        film1 = filmStorage.add(film1);
        Film film2 = newFilm("Фильм 2");
        film2.setReleaseDate(LocalDate.of(2010, 1, 1));
        film2.getDirectors().add(director);
        film2 = filmStorage.add(film2);
        Film film3 = newFilm("Фильм 3");
        film3.setReleaseDate(LocalDate.of(2005, 1, 1));
        film3.getDirectors().add(director);
        film3 = filmStorage.add(film3);
        List<Film> allFilms = filmStorage.findFilmByDirector(director.getId(), "year");
        assertEquals(film1.getId(), allFilms.get(0).getId());
        assertEquals(film3.getId(), allFilms.get(1).getId());
        assertEquals(film2.getId(), allFilms.get(2).getId());
    }
}