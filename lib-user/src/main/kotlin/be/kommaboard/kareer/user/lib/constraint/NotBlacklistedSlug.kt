package be.kommaboard.kareer.user.lib.constraint

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [NotBlacklistedSlugValidator::class])
annotation class NotBlacklistedSlug(
    val message: String = "constraint.NotBlacklistedSlug.message",
    val groups: Array<KClass<out Any>> = [],
    val payload: Array<KClass<out Any>> = [],
)

class NotBlacklistedSlugValidator : ConstraintValidator<NotBlacklistedSlug, String> {

    override fun initialize(constraintAnnotation: NotBlacklistedSlug) {}

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        return if (value == null) {
            true
        } else {
            setOf(
                // Slugs we should not allow because e.g. they might be offensive or because they can collide with existing endpoint paths
                "invitations",
                "verification",
            ).none { it.equals(value, ignoreCase = true) }
        }
    }
}
