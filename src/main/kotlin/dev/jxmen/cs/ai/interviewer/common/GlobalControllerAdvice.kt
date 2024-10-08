package dev.jxmen.cs.ai.interviewer.common

import dev.jxmen.cs.ai.interviewer.common.dto.ApiResponse
import dev.jxmen.cs.ai.interviewer.common.exceptions.ServerError
import dev.jxmen.cs.ai.interviewer.common.exceptions.UnAuthorizedException
import dev.jxmen.cs.ai.interviewer.domain.chat.exceptions.AllAnswersUsedException
import dev.jxmen.cs.ai.interviewer.domain.chat.exceptions.NoAnswerException
import dev.jxmen.cs.ai.interviewer.domain.subject.exceptions.SubjectNotFoundException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalControllerAdvice {
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.badRequest().body(
            ApiResponse.failure(
                code = "BAD_REQUEST",
                status = 400,
                message = e.message ?: "Bad request",
            ),
        )

    @ExceptionHandler(NoAnswerException::class)
    fun handleNoAnswerException(e: NoAnswerException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.badRequest().body(
            ApiResponse.failure(
                code = "NO_ANSWER",
                status = 400,
                message = e.message ?: "No answer",
            ),
        )

    @ExceptionHandler(AllAnswersUsedException::class)
    fun handleAllAnswersUsedException(e: AllAnswersUsedException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.badRequest().body(
            ApiResponse.failure(
                code = "ALL_ANSWERS_USED",
                status = 400,
                message = e.message ?: "All answers used",
            ),
        )

    @ExceptionHandler(SubjectNotFoundException::class)
    fun handleSubjectNotFoundException(e: SubjectNotFoundException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(404).body(
            ApiResponse.failure(
                code = "SUBJECT_NOT_FOUND",
                status = 404,
                message = e.message ?: "Subject not found",
            ),
        )

    @ExceptionHandler(UnAuthorizedException::class)
    fun handleUnAuthorizedException(e: UnAuthorizedException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(401).body(
            ApiResponse.failure(
                code = e.errorType.toString(),
                status = 401,
                message = e.message ?: "Unauthorized",
            ),
        )

    @ExceptionHandler(ServerError::class)
    fun handleServerError(e: ServerError): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(500).body(
            ApiResponse.failure(
                code = e.errorType.toString(),
                status = 500,
                message = e.message ?: "Internal Server Error",
            ),
        )
}
