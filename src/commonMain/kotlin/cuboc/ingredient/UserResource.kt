package cuboc.ingredient

import kotlinx.serialization.Serializable

@Serializable
data class UserResource(val id: String, val resource: Resource)

