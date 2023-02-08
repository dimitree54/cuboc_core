package cuboc.recipe

import cuboc.ingredient.PieceOfUserResource
import cuboc.ingredient.RecipeInput
import cuboc.ingredient.Resource

class Scenario(val request: Resource, val recipe: Recipe, val resources: Map<RecipeInput, List<PieceOfUserResource>>) {
    init {
        for (input in recipe.inputs) {
            require(resources.containsKey(input))
            var resourceAmount = 0.0
            for (resource in resources[input]!!) {
                resourceAmount += resource.amount
            }
            require(resourceAmount >= input.amount)
        }
    }
}