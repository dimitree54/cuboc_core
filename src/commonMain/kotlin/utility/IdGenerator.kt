package cuboc_core.utility

class IdGenerator {
    private var id = 0
    fun generateId(): String {
        return id++.toString()
    }
}