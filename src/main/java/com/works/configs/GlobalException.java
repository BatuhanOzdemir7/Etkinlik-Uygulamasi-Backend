package com.works.configs;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalException {

    // 1. Validation Hataları
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity handleMethodArgumentNotValidException(MethodArgumentNotValidException e){
        List<HashMap<String, Object>> errors = parseError(e.getFieldErrors());
        return ResponseEntity.badRequest().body(errors);
    }

    private List<HashMap<String, Object>> parseError(List<FieldError> fieldErrors) {
        List<HashMap<String, Object>> errors = new ArrayList<>();
        for (FieldError error : fieldErrors) {
            HashMap<String, Object> errorMap = new HashMap<>();
            errorMap.put("field", error.getField());
            errorMap.put("message", error.getDefaultMessage());
            errorMap.put("rejectedValue", error.getRejectedValue());
            errors.add(errorMap);
        }
        return errors;
    }

    // 2. Parametre Tip Uyuşmazlığı Hataları
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        Object value = ex.getValue();
        Class<?> requiredType = ex.getRequiredType();

        String message = "";

        if (requiredType != null) {
            if (requiredType == Long.class || requiredType == Integer.class) {
                message = paramName + " sayısal bir değer olmalıdır. Gönderilen değer: " + value;
            }
            else if (requiredType == Boolean.class) {
                message = paramName + " true veya false olmalıdır. Gönderilen değer: " + value;
            }
            else {
                message = paramName + " parametresi için geçersiz değer: " + value;
            }
        } else {
            message = "Geçersiz parametre: " + paramName;
        }

        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return ResponseEntity.badRequest().body(error);
    }

    // 3.Kayıt Bulunamadı (404 Not Found)
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleEntityNotFoundException(EntityNotFoundException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", ex.getMessage() != null ? ex.getMessage() : "İstenen kayıt veritabanında bulunamadı.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // 4.Yetkisiz İşlem (401 Unauthorized / 403 Forbidden)
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Object> handleSecurityException(SecurityException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", ex.getMessage() != null ? ex.getMessage() : "Bu işlemi gerçekleştirmek için yetkiniz bulunmamaktadır.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // 5. Sistem Hataları (500 Internal Server Error)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllUncaughtException(Exception ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", "Beklenmeyen bir sistem hatası oluştu. Lütfen daha sonra tekrar deneyin.");

        // Geliştirme aşamasında hatanın ne olduğunu konsolda görebilmek için:
        ex.printStackTrace();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}