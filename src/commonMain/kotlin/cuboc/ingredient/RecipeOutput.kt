package cuboc.ingredient

class RecipeOutput(val ingredient: Ingredient, amount: Double, val scalable: Boolean){
    var amount: Double = amount
        private set
    fun scale(scaleFactor: Double){
        if (scalable){
            amount *= scaleFactor
        }
    }
}