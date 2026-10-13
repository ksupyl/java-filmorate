package ru.yandex.practicum.filmorate.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationException(ValidationException e) {
        log.warn("400: {}", e.getMessage());
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFoundException(NotFoundException e) {
        log.warn("404: {}", e.getMessage());
        return Map.of("error", e.getMessage());
    }

    // Нарушены аннотации валидации (@NotBlank, @Positive и т.п.): в ответе — поле и что с ним не так
    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("400: {}", message);
        return Map.of("error", message);
    }

    // Нет обязательного параметра, например GET /films/search без by
    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("400: {}", e.getMessage());
        return Map.of("error", "Не указан обязательный параметр '" + e.getParameterName() + "'");
    }

    // Параметр не того типа, например GET /films/abc вместо числового id
    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("400: {}", e.getMessage());
        return Map.of("error", "Некорректное значение параметра '" + e.getName() + "': " + e.getValue());
    }

    // Тело запроса не читается как JSON нужной структуры
    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleNotReadableBody(HttpMessageNotReadableException e) {
        log.warn("400: {}", e.getMessage());
        return Map.of("error", "Некорректное тело запроса");
    }

    // Запрос на адрес, которого нет в API
    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNoResource(NoResourceFoundException e) {
        log.warn("404: {}", e.getMessage());
        return Map.of("error", "Адрес /" + e.getResourcePath() + " не найден");
    }

    // Адрес есть, но с другим HTTP-методом, например POST /films/1
    @ExceptionHandler
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Map<String, String> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("405: {}", e.getMessage());
        return Map.of("error", "Метод " + e.getMethod() + " для этого адреса не поддерживается");
    }

    // Тело пришло не в JSON, например с Content-Type: text/plain
    @ExceptionHandler
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Map<String, String> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("415: {}", e.getMessage());
        return Map.of("error", "Тело запроса должно быть в формате JSON");
    }

    // Непредвиденная ошибка: подробности только в лог, клиенту — общее сообщение без SQL и стектрейсов
    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleException(Exception e) {
        log.error("500: {}", e.getMessage(), e);
        return Map.of("error", "Внутренняя ошибка сервера");
    }
}
