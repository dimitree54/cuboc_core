package cuboc.ingredient

class ResourcePrototype(val ingredient: Ingredient, val amount: Double) {
    fun toResource(id: String): Resource = Resource(id, ingredient, amount)
}