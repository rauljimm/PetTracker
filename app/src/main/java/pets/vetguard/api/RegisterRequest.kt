package pets.vetguard.api

import java.io.Serializable

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
) : Serializable