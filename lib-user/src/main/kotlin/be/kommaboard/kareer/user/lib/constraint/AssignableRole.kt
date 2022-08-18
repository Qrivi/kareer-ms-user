package be.kommaboard.kareer.user.lib.constraint

import javax.validation.Constraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [AssignableRoleValidator::class])
annotation class AssignableRole(
    val message: String = "constraint.AssignableRole.message",
    val groups: Array<KClass<out Any>> = [],
    val payload: Array<KClass<out Any>> = [],
)

class AssignableRoleValidator : ConstraintValidator<AssignableRole, String> {

    override fun initialize(constraintAnnotation: AssignableRole) {}

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        return if (value == null)
            true
        else
            value.equals("manager", true) || value.equals("user", true)
    }
}
