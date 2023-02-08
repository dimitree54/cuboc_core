package cuboc.recipe

import kotlinx.serialization.Serializable
import utility.Text
import kotlin.math.ceil

@Serializable
data class Instruction(val durationMinutes: Int, val text: Text) {
    fun getScaled(scaleFactor: Double): Instruction {
        return Instruction(ceil(durationMinutes * scaleFactor).toInt(), text)
    }
}