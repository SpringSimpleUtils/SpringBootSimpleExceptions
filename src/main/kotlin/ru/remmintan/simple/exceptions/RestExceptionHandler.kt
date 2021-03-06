package ru.remmintan.simple.exceptions

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.validation.BindingResult
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import ru.remmintan.simple.exceptions.dtos.ErrorDto
import ru.remmintan.simple.exceptions.exceptions.ApiException

private val logger = KotlinLogging.logger {}

@ControllerAdvice
class RestExceptionHandler {


    @Value("${'$'}{simple.exceptions.messages.global-handler:An error has occurred}")
    private lateinit var globalHandlerMessage: String

    @Value("${'$'}{simple.exceptions.messages.not-readable:Can't read request message}")
    private lateinit var notReadableMessage: String

    @ExceptionHandler(ApiException::class)
    fun handleApiException(ex: ApiException) : ResponseEntity<ErrorDto> =
        ResponseEntity.status(ex.code).body(ErrorDto(ex.message!!))

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun handleReadingException(ex: HttpMessageNotReadableException): ErrorDto {
        logger.error(ex) {"Can't read input message"}
        return ErrorDto(notReadableMessage)
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    fun handleAll(ex: Exception): ErrorDto {
        logger.error(ex) {"Unhandled exception in application!"}
        return ErrorDto(globalHandlerMessage)
    }

    @ExceptionHandler(BindException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun handleBindException(ex: BindException): ErrorDto = parseBindingResult(ex.bindingResult)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException): ErrorDto
        = parseBindingResult(ex.bindingResult)

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotAllowedException(ex: HttpRequestMethodNotSupportedException): ResponseEntity<ErrorDto> {
        val allowedMethods = ex.supportedMethods?.toList()?.joinToString(", ") ?: ""
        val errorMessage = "Method ${ex.method} not allowed. Allowed methods: $allowedMethods."

        val dto = ErrorDto(errorMessage)
        var responseEntity = ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED);

        val allowedHeaders = ex.supportedHttpMethods?.toTypedArray()
        if(allowedHeaders != null) {
            responseEntity = responseEntity.allow(*allowedHeaders)
        }

        return responseEntity.body(dto)
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    @ResponseBody
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    fun handleUnsupportedMediaTypeException(ex: HttpMediaTypeNotSupportedException): ErrorDto =
        ErrorDto(ex.localizedMessage)

    private fun parseBindingResult(br: BindingResult): ErrorDto {
        val errors = br.allErrors.map {it.defaultMessage}.joinToString("\n")
        return ErrorDto(errors)
    }
}