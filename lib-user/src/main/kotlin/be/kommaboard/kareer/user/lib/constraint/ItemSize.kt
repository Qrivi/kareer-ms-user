package be.kommaboard.kareer.user.lib.constraint

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ItemSizeValidator::class])
annotation class ItemSize(
    val message: String = "constraint.ItemSize.message",
    val groups: Array<KClass<out Any>> = [],
    val payload: Array<KClass<out Any>> = [],

    val min: Int = 0,
    val max: Int = Int.MAX_VALUE,
)

class ItemSizeValidator : ConstraintValidator<ItemSize, List<String>> {

    private var min: Int = 0
    private var max: Int = Int.MAX_VALUE

    override fun initialize(constraintAnnotation: ItemSize) {
        this.min = constraintAnnotation.min
        this.max = constraintAnnotation.max
    }

    override fun isValid(values: List<String>?, context: ConstraintValidatorContext) = values?.none { it.trim().length < min || it.trim().length > max } ?: true
}
