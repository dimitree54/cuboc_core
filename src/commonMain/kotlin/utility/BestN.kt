package cuboc_core.utility

class BestN<T>(val n: Int, initialMaxAllowedCost: Double? = null) : Collection<T> {
    private val collection = mutableListOf<T>()
    private val costs = mutableListOf<Double>()
    var maxAllowedCost: Double = initialMaxAllowedCost ?: Double.MAX_VALUE
        private set

    fun add(item: T, cost: Double): Boolean {
        var added = false
        if (collection.size < n) {
            collection.add(item)
            added = true
        } else if (cost < maxAllowedCost) {
            collection.removeLast()
            costs.removeLast()
            collection.add(item)
            costs.add(cost)
            added = true
        }
        if (added) {
            collection.sortBy { costs[collection.indexOf(it)] }
            costs.sort()
            if (collection.size == n) {
                maxAllowedCost = costs.maxOrNull() ?: Double.MAX_VALUE
            }
        }
        return added
    }

    override val size = collection.size
    override fun isEmpty() = collection.isEmpty()
    override fun iterator() = collection.iterator()
    override fun containsAll(elements: Collection<T>) = collection.containsAll(elements)
    override fun contains(element: T) = collection.contains(element)
}