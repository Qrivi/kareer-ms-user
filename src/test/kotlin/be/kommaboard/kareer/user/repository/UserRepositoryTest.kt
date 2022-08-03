package be.kommaboard.kareer.user.repository

import be.kommaboard.kareer.common.security.Role
import be.kommaboard.kareer.user.TestData
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.TestPropertySource

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:kareer",
    ]
)
class UserRepositoryTest(
    @Autowired private val userRepository: UserRepository,
) {
    lateinit var testData: TestData

    // region Setup

    @BeforeEach
    fun initUseCase() {
        testData = TestData()
        userRepository.saveAll(testData.users)
    }

    @AfterEach
    fun destroyAll() {
        userRepository.deleteAll()
    }

    // endregion

    @ParameterizedTest
    @EnumSource(value = Role::class, names = ["ADMIN", "MANAGER", "USER"])
    fun findAllByRole_success(role: Role) {
        val results = userRepository.findAllByRole(role, PageRequest.of(0, 10))

        assertThat(results.totalPages, `is`(equalTo(1)))
        assertThat(results.content.size, `is`(equalTo(1)))
        assertThat(results.content.first().role, `is`(equalTo(role)))
    }
}
