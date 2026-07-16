package com.caliarena.service

import com.caliarena.RepositoryAthlete
import com.caliarena.RepositoryClub
import com.caliarena.RepositoryEnduranceRoutine
import com.caliarena.RepositoryMatch
import com.caliarena.RepositoryTournament
import com.caliarena.RepositoryUser
import com.caliarena.Transaction
import com.caliarena.TransactionManager
import com.caliarena.domain.token.TokenEncoder
import com.caliarena.domain.user.UsersDomainConfig
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

@ExtendWith(MockitoExtension::class)
abstract class ServiceTest {
    @Mock
    protected lateinit var trxManager: TransactionManager

    @Mock
    protected lateinit var transaction: Transaction

    @Mock
    protected lateinit var repoUser: RepositoryUser

    @Mock
    protected lateinit var repoAthlete: RepositoryAthlete

    @Mock
    protected lateinit var repoClub: RepositoryClub

    @Mock
    protected lateinit var repoTournament: RepositoryTournament

    @Mock
    protected lateinit var repoMatch: RepositoryMatch

    @Mock
    protected lateinit var repoEnduranceRoutine: RepositoryEnduranceRoutine

    @Mock
    protected lateinit var passwordEncoder: PasswordEncoder

    @Mock
    protected lateinit var tokenEncoder: TokenEncoder

    protected val clock: Clock =
        Clock.fixed(
            Instant.parse("2025-01-01T00:00:00Z"),
            ZoneOffset.UTC,
        )

    protected val config =
        UsersDomainConfig(
            tokenSizeInBytes = 32,
            tokenTtl = Duration.ofHours(24),
            tokenRollingTtl = Duration.ofHours(1),
            maxTokensPerUser = 3,
        )
}
