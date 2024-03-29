package utility

import kotlinx.serialization.Serializable

@Serializable
data class MeasureUnit(val name: Name) {
    override fun toString(): String {
        return name.toString()
    }
}