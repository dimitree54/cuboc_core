package cuboc.ingredient

import kotlinx.serialization.Serializable

@Serializable
data class UserResource(val id: String, val resource: Resource) {
    val amount: Double
        get() = resource.amount
}

