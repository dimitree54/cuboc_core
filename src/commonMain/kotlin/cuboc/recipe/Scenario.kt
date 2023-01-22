package cuboc.recipe

import cuboc.ingredient.IngredientRequest
import cuboc.ingredient.PieceOfResource
import cuboc.ingredient.RecipeInput

class Scenario(val request: IngredientRequest, val recipe: Recipe, val resources: Map<RecipeInput, List<PieceOfResource>>){
    init {
        for (input in recipe.inputs){
            require(resources.containsKey(input))
            var resourceAmount = 0.0
            for (resource in resources[input]!!){
                resourceAmount += resource.amount
            }
            require(resourceAmount >= input.amount)
        }
    }
}