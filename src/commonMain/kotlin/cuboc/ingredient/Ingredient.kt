package cuboc.ingredient

import utility.MeasureUnit
import utility.Name

class Ingredient(val name: Name, val measureUnit: MeasureUnit) {
    override fun hashCode(): Int {
        return name.hashCode() + measureUnit.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is Ingredient && other.name == name && other.measureUnit == measureUnit
    }
}

