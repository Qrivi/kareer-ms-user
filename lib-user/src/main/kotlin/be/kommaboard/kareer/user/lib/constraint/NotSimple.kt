package be.kommaboard.kareer.user.lib.constraint

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [NotSimpleValidator::class])
annotation class NotSimple(
    val message: String = "constraint.NotSimple.message",
    val groups: Array<KClass<out Any>> = [],
    val payload: Array<KClass<out Any>> = [],
)

class NotSimpleValidator : ConstraintValidator<NotSimple, String> {

    override fun initialize(constraintAnnotation: NotSimple) {}

    // Regex checks for 1 uppercase, 1 lowercase and 1 number
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        return if (value == null) {
            true
        } else {
            Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+\$").matches(value)
        }
    }
}
