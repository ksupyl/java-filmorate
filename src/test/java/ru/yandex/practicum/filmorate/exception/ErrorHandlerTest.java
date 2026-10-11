package ru.yandex.practicum.filmorate.exception;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Некорректные и пограничные запросы: раньше часть из них падала с 500.
// Ошибки клиента должны давать 400/404/405/415, корректные запросы — 200
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@Transactional
class ErrorHandlerTest {

    private static final String FILM_JSON = "{\"name\":\"Фильм\",\"description\":\"Описание\","
            + "\"releaseDate\":\"2000-01-01\",\"duration\":100,\"mpa\":{\"id\":1}%s}";

    private final MockMvc mockMvc;

    @Autowired
    ErrorHandlerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    private String film(String extraFields) {
        return String.format(FILM_JSON, extraFields);
    }

    @Test
    void shouldReturn400WhenPathVariableHasWrongType() throws Exception {
        mockMvc.perform(get("/films/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenRequiredParameterIsMissing() throws Exception {
        mockMvc.perform(get("/films/search").param("query", "фильм"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Не указан обязательный параметр 'by'"));
    }

    @Test
    void shouldReturn400WhenJsonIsBroken() throws Exception {
        mockMvc.perform(post("/films").contentType(MediaType.APPLICATION_JSON).content("{не json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenUpdatingFilmWithoutId() throws Exception {
        mockMvc.perform(put("/films").contentType(MediaType.APPLICATION_JSON).content(film("")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenUpdatingUserWithoutId() throws Exception {
        String user = "{\"email\":\"anna@mail.ru\",\"login\":\"anna\",\"birthday\":\"1990-01-01\"}";
        mockMvc.perform(put("/users").contentType(MediaType.APPLICATION_JSON).content(user))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenMpaHasNoId() throws Exception {
        String filmWithEmptyMpa = film("").replace("{\"id\":1}", "{}");
        mockMvc.perform(post("/films").contentType(MediaType.APPLICATION_JSON).content(filmWithEmptyMpa))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenSortByIsUnknown() throws Exception {
        mockMvc.perform(get("/films/director/1").param("sortBy", "rating"))
                .andExpect(status().isBadRequest());
    }

    // Значения by проверяются без учёта регистра — и поиск тоже должен их понимать
    @Test
    void shouldSearchWhenByIsInAnyCase() throws Exception {
        mockMvc.perform(get("/films/search").param("query", "фильм").param("by", "Title,DIRECTOR"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn404ForUnknownAddress() throws Exception {
        mockMvc.perform(get("/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn415WhenContentTypeIsNotJson() throws Exception {
        mockMvc.perform(post("/films").contentType(MediaType.TEXT_PLAIN).content("фильм"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void shouldReturn405WhenMethodIsNotSupported() throws Exception {
        mockMvc.perform(post("/films/1"))
                .andExpect(status().isMethodNotAllowed());
    }

    // Режиссёр сравнивается по id: дубль в запросе не должен ронять запись связи
    @Test
    void shouldIgnoreDuplicateDirectorInFilm() throws Exception {
        String director = mockMvc.perform(post("/directors")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Режиссёр\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer directorId = JsonPath.read(director, "$.id");
        String directors = ",\"directors\":[{\"id\":" + directorId + "},{\"id\":" + directorId
                + ",\"name\":\"Режиссёр\"}]";

        mockMvc.perform(post("/films").contentType(MediaType.APPLICATION_JSON).content(film(directors)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.directors.length()").value(1));
    }
}
