package com.caliarena.repo

import com.caliarena.repo.trx.TransactionManagerJpa
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Instant
import java.time.temporal.ChronoUnit

@SpringBootApplication(scanBasePackages = ["com.caliarena.repo"])
class TestConfig

@SpringBootTest(classes = [TestConfig::class])
abstract class AbstractRepositoryTest {
    @Autowired
    lateinit var trx: TransactionManagerJpa

    @Autowired
    lateinit var jdbc: JdbcTemplate

    @BeforeEach
    fun cleanup() {
        truncateAll()
    }

    private fun truncateAll() =
        jdbc.execute(
            """
            TRUNCATE TABLE 
            match_progress, matches, screen_routines, tournament_state, brackets, tournaments, exercises, endurance_routines, tokens, users, athletes, clubs 
            CASCADE
            """.trimIndent(),
        )

    protected fun now(): Instant = Instant.now().truncatedTo(ChronoUnit.SECONDS)

    companion object {
        @JvmStatic
        @ServiceConnection
        val postgres: PostgreSQLContainer =
            PostgreSQLContainer("postgres:16.0")
                .withInitScript("create-schema.sql")
                .apply { start() }
    }
}
