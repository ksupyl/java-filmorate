package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

// Равенство по id, как у Genre и Mpa: дубль режиссёра в запросе схлопнется в один
@Data
@EqualsAndHashCode(of = "id")
public class Director {
    private Long id;

    @NotBlank(message = "Имя режиссёра не может быть пустым")
    private String name;
}
