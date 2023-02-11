package cuboc.ingredient

import kotlinx.serialization.Serializable

@Serializable
data class PieceOfUserResource(val userResource: UserResource, val amount: Double) {
    val ingredient: Ingredient
        get() = userResource.resource.ingredient
    val resource: Resource
        get() = userResource.resource
}