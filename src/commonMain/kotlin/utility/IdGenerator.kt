package cuboc_core.utility

import kotlin.random.Random

class IdGenerator {
    fun generateId(): String {
        return Random.nextLong().toString()
    }

    fun generateId(name: String): String {
        return name + "_" + Random.nextLong().toString()
    }
}