package cuboc.ingredient

import kotlinx.serialization.Serializable

@Serializable
data class Resource(val ingredient: Ingredient, val amount: Double)