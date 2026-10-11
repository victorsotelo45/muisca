package com.unicauca.muisca.infrastructure.web;

import com.unicauca.muisca.application.exception.ConflictException;
import com.unicauca.muisca.application.exception.ForbiddenOperationException;
import com.unicauca.muisca.application.exception.InvalidArtifactException;
import com.unicauca.muisca.application.exception.ResourceNotFoundException;
import com.unicauca.muisca.application.port.ModelingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Traduce las excepciones a respuestas JSON con un mensaje claro para el cliente. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(ResourceNotFoundException e) {
        return build(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> noRoute(NoResourceFoundException e) {
        return build(HttpStatus.NOT_FOUND, "No existe el recurso /" + e.getResourcePath());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> conflict(ConflictException e) {
        return build(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integrity(DataIntegrityViolationException e) {
        return build(HttpStatus.CONFLICT, "La operación viola una restricción de la base de datos (registro "
                + "duplicado o referenciado por otros datos)");
    }

    @ExceptionHandler({ ForbiddenOperationException.class, AccessDeniedException.class })
    public ResponseEntity<Map<String, Object>> forbidden(RuntimeException e) {
        return build(HttpStatus.FORBIDDEN, e instanceof ForbiddenOperationException ? e.getMessage()
                : "Su rol no tiene permiso para realizar esta acción.");
    }

    @ExceptionHandler(InvalidArtifactException.class)
    public ResponseEntity<Map<String, Object>> invalidArtifact(InvalidArtifactException e) {
        Map<String, Object> body = body(HttpStatus.BAD_REQUEST, e.getMessage());
        if (!e.getProblems().isEmpty()) {
            body.put("problems", e.getProblems());
        }
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ModelingException.class)
    public ResponseEntity<Map<String, Object>> modeling(ModelingException e) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalidBody(MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        Map<String, Object> body = body(HttpStatus.BAD_REQUEST, "La petición tiene campos inválidos");
        body.put("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> missingPart(MissingServletRequestPartException e) {
        return build(HttpStatus.BAD_REQUEST, "Falta el archivo '" + e.getRequestPartName()
                + "' en el formulario multipart (form-data)");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> missingParameter(MissingServletRequestParameterException e) {
        return build(HttpStatus.BAD_REQUEST, "Falta el parámetro '" + e.getParameterName() + "'");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> typeMismatch(MethodArgumentTypeMismatchException e) {
        return build(HttpStatus.BAD_REQUEST, "Valor inválido para '" + e.getName() + "': " + e.getValue());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadableBody(HttpMessageNotReadableException e) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la petición es inválido o está vacío (se espera JSON).");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> tooLarge(MaxUploadSizeExceededException e) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el tamaño máximo permitido (10 MB)");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> unsupportedMediaType(HttpMediaTypeNotSupportedException e) {
        String expected = e.getSupportedMediaTypes().isEmpty() ? "otro formato"
                : e.getSupportedMediaTypes().get(0).toString();
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type no soportado; esta ruta espera " + expected
                + (expected.startsWith("multipart") ? " (en Postman: Body > form-data)" : ""));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> methodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "El método " + e.getMethod() + " no está soportado en esta ruta");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> badCredentials(BadCredentialsException e) {
        return build(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos.");
    }

    @ExceptionHandler({ DisabledException.class, LockedException.class })
    public ResponseEntity<Map<String, Object>> accountBlocked(AuthenticationException e) {
        return build(HttpStatus.UNAUTHORIZED, "La cuenta está deshabilitada o bloqueada.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> authentication(AuthenticationException e) {
        return build(HttpStatus.UNAUTHORIZED, "No se pudo autenticar: " + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception e) {
        log.error("Error no controlado", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor: " + e.getMessage());
    }

    private static ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(body(status, message));
    }

    private static Map<String, Object> body(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return body;
    }
}
