package cuboc.recipe

import cuboc.ingredient.IngredientRequest
import cuboc.ingredient.RecipeInput
import cuboc.ingredient.RecipeOutput
import utility.Name
import utility.Text

class TrivialRecipe(request: IngredientRequest) : Recipe(
    Name("trivial " + request.ingredient.name.toString()),
    setOf(RecipeInput(request.ingredient, request.amount, true)),
    setOf(RecipeOutput(request.ingredient, request.amount, true)),
    Instruction(
        0,
        Text("Just take ${request.amount} ${request.ingredient.measureUnit} of ${request.ingredient.name}")
    )
)