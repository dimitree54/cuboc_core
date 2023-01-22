package cuboc.database

import cuboc.ingredient.Ingredient
import cuboc.ingredient.PieceOfResource
import cuboc.ingredient.Resource

interface ResourcesDatabase {
    fun search(ingredient: Ingredient): List<Resource>
    fun get(request: PieceOfResource): PieceOfResource?
    fun put(ingredient: Ingredient, amount: Double): Resource?
}