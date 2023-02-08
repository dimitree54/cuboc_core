package cuboc_core.cuboc.ingredient

import cuboc.ingredient.Resource
import kotlinx.serialization.Serializable

@Serializable
data class UserResource(val id: String, val resource: Resource)

