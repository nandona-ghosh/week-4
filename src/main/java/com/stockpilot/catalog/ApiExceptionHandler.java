package com.stockpilot.catalog;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice
class ApiExceptionHandler {
    record ErrorResponse(String code, String message, Map<String,String> details) {}
    @ExceptionHandler(ProductNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(ProductNotFoundException e) { return error(HttpStatus.NOT_FOUND,"NOT_FOUND",e.getMessage(),Map.of()); }
    @ExceptionHandler({DuplicateSkuException.class, DataIntegrityViolationException.class})
    ResponseEntity<ErrorResponse> conflict(Exception e) { return error(HttpStatus.CONFLICT,"DUPLICATE_SKU","A product with this SKU already exists",Map.of()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> invalid(MethodArgumentNotValidException e) {
        Map<String,String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> errors.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","Please check the supplied fields",errors);
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> malformed(HttpMessageNotReadableException e) { return error(HttpStatus.BAD_REQUEST,"INVALID_JSON","Request body is missing or malformed",Map.of()); }
    @ExceptionHandler(InvalidBarcodeException.class)
    ResponseEntity<ErrorResponse> invalidBarcode(InvalidBarcodeException e) { return error(HttpStatus.BAD_REQUEST,"INVALID_BARCODE",e.getMessage(),Map.of()); }
    @ExceptionHandler(FoodNotFoundException.class)
    ResponseEntity<ErrorResponse> foodNotFound(FoodNotFoundException e) { return error(HttpStatus.NOT_FOUND,"FOOD_NOT_FOUND",e.getMessage(),Map.of()); }
    @ExceptionHandler(FoodProviderException.class)
    ResponseEntity<ErrorResponse> providerFailure(FoodProviderException e) { return error(HttpStatus.BAD_GATEWAY,"FOOD_PROVIDER_ERROR",e.getMessage(),Map.of()); }
    private ResponseEntity<ErrorResponse> error(HttpStatus status,String code,String message,Map<String,String> details) {
        return ResponseEntity.status(status).body(new ErrorResponse(code,message,details));
    }
}