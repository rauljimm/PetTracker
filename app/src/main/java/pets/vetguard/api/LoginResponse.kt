package pets.vetguard.api

import java.io.Serializable

data class LoginResponse(
    val username: String,
    val token: String
) : Serializable