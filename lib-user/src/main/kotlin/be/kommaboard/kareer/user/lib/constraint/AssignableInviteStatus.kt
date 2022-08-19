package be.kommaboard.kareer.user.lib.constraint

import javax.validation.Constraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [AssignableInviteStatusValidator::class])
annotation class AssignableInviteStatus(
    val message: String = "constraint.AssignableInviteStatus.message",
    val groups: Array<KClass<out Any>> = [],
    val payload: Array<KClass<out Any>> = [],
)

class AssignableInviteStatusValidator : ConstraintValidator<AssignableInviteStatus, String> {

    override fun initialize(constraintAnnotation: AssignableInviteStatus) {}

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        return if (value == null)
            true
        else
            value.equals("pending", true) || value.equals("retracted", true)
    }
}
