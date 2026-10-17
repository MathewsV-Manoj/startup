package com.focuno.core.common

/** Minimal result type for operations that can fail without throwing. */
sealed interface FocunoResult<out T> {
    data class Success<T>(val value: T) : FocunoResult<T>
    data class Failure(val error: FocunoError) : FocunoResult<Nothing>

    fun getOrNull(): T? = (this as? Success)?.value
}

/** Domain-level errors, mapped from platform exceptions in the data layer. */
sealed class FocunoError(val message: String, cause: Throwable? = null) : Exception(message, cause) {
    class PermissionMissing(permission: String) :
        FocunoError("Missing required permission: $permission")

    class Platform(override val message: String, cause: Throwable? = null) :
        FocunoError(message, cause)

    data object NotFound : FocunoError("Not found")
}
