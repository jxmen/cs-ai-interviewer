package dev.jxmen.cs.ai.interviewer.common.dto

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiErrorResponse? = null,
) {
    companion object {
        fun failure(
            code: String,
            status: Int,
            message: String,
        ): ApiResponse<Nothing> =
            ApiResponse(
                success = false,
                data = null,
                error =
                    ApiErrorResponse(
                        code = code,
                        status = status,
                        message = message,
                    ),
            )

        fun success(): ApiResponse<Nothing> =
            ApiResponse(
                success = true,
                data = null,
                error = null,
            )

        fun <T> success(list: ListDataResponse<T>): ApiResponse<List<T>> = success(list.data)

        fun <T> success(data: T?): ApiResponse<T> =
            ApiResponse(
                success = true,
                data = data,
                error = null,
            )
    }
}

data class ApiErrorResponse(
    val code: String,
    val status: Int,
    val message: String,
)
