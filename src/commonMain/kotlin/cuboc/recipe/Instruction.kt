package cuboc.recipe

import kotlin.math.ceil

class Instruction(durationMinutes: Int, val text: String){
    var durationMinutes: Int = durationMinutes
        private set
    fun scale(scaleFactor: Double) {
        durationMinutes = ceil(durationMinutes * scaleFactor).toInt()
    }
}