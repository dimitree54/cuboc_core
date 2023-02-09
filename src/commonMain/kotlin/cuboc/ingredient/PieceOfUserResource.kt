package cuboc.ingredient

import kotlinx.serialization.Serializable

@Serializable
data class PieceOfUserResource(val resource: UserResource, val amount: Double)