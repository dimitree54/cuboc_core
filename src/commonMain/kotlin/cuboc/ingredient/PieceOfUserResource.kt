package cuboc.ingredient

import cuboc_core.cuboc.ingredient.UserResource
import kotlinx.serialization.Serializable

@Serializable
data class PieceOfUserResource(val resource: UserResource, val amount: Double)