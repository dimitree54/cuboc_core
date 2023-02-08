package cuboc.ingredient

import cuboc_core.cuboc.ingredient.UserResource
import kotlinx.serialization.Serializable

@Serializable
data class PieceOfResource(val resource: UserResource, val amount: Double)