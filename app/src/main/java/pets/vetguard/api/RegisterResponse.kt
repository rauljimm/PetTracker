package pets.vetguard.api

import java.io.Serializable

data class RegisterResponse(
    val id: Long?,
    val username: String?,
    val email: String?,
    val urlImage: String?,
    val role: String?,
    val enabled: Boolean?,
    val authorities: List<Authority>?,
    val accountNonLocked: Boolean?,
    val accountNonExpired: Boolean?,
    val credentialsNonExpired: Boolean?
) : Serializable

data class Authority(
    val authority: String?
) : Serializable