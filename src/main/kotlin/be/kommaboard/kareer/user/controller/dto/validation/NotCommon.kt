package be.kommaboard.kareer.user.controller.dto.validation

import javax.validation.Constraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [NotCommonValidator::class])
annotation class NotCommon(
    val message: String = "{validation.NotCommon.message}",
    val groups: Array<KClass<out Any>> = [],
    val payload: Array<KClass<out Any>> = [],
)

class NotCommonValidator : ConstraintValidator<NotCommon, String> {

    override fun initialize(constraintAnnotation: NotCommon) {}

    override fun isValid(value: String, context: ConstraintValidatorContext): Boolean {
        listOf(
            "password", "pass1234", "12345678", "01234567", "baseball", "trustno1", "superman", "testtest", "computer",
            "michelle", "123456789", "0123456789", "012345678", "1234567890", "corvette", "00000000", "test1234", "kommaboard",
        ).find { it.equals(value, ignoreCase = true) }?.let {
            return false
        }
        return true
    }
}
