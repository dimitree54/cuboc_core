package utility

import kotlinx.serialization.Serializable

@Serializable
data class Name(val name: String) {
    override fun toString(): String {
        return name
    }
}