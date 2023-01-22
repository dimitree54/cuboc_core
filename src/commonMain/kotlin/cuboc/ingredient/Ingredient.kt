package cuboc.ingredient

import utility.MeasureUnit

class Ingredient(val name: String, val measureUnit: MeasureUnit){
    override fun hashCode(): Int {
        return name.hashCode() + measureUnit.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is Ingredient && other.name == name && other.measureUnit == measureUnit
    }
}

