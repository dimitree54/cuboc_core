package cuboc.recipe

import utility.Text
import kotlin.math.ceil

class Instruction(durationMinutes: Int, val text: Text) {
    var durationMinutes: Int = durationMinutes
        private set

    fun scale(scaleFactor: Double) {
        durationMinutes = ceil(durationMinutes * scaleFactor).toInt()
    }
}