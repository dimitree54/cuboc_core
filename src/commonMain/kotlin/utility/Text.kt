package utility

import kotlinx.serialization.Serializable

@Serializable
data class Text(val text: String) {
    override fun toString(): String {
        return text
    }
}