package cuboc.ingredient

class RecipeInput(val ingredient: Ingredient, amount: Double, val scalable: Boolean){
    var amount: Double = amount
        private set
    fun scale(scaleFactor: Double){
        if (scalable){
            amount *= scaleFactor
        }
    }
}