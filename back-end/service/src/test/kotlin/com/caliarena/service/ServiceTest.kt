package com.caliarena.service

import com.caliarena.domain.token.TokenEncoder
import com.caliarena.domain.user.UsersDomainConfig
import com.caliarena.repo.AthleteRepository
import com.caliarena.repo.BracketRepository
import com.caliarena.repo.ClubRepository
import com.caliarena.repo.EnduranceRoutineRepository
import com.caliarena.repo.ExerciseRepository
import com.caliarena.repo.MatchProgressRepository
import com.caliarena.repo.MatchRepository
import com.caliarena.repo.ScreenRoutineRepository
import com.caliarena.repo.TokenRepository
import com.caliarena.repo.TournamentRepository
import com.caliarena.repo.TournamentStateRepository
import com.caliarena.repo.UserRepository
import com.caliarena.repo.trx.Transaction
import com.caliarena.repo.trx.TransactionManager
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
    protected lateinit var users: UserRepository

    @Mock
    protected lateinit var tokens: TokenRepository

    @Mock
    protected lateinit var athletes: AthleteRepository

    @Mock
    protected lateinit var clubs: ClubRepository

    @Mock
    protected lateinit var routines: EnduranceRoutineRepository

    @Mock
    protected lateinit var exercises: ExerciseRepository

    @Mock
    protected lateinit var screenRoutines: ScreenRoutineRepository

    @Mock
    protected lateinit var matches: MatchRepository

    @Mock
    protected lateinit var matchProgresses: MatchProgressRepository

    @Mock
    protected lateinit var brackets: BracketRepository

    @Mock
    protected lateinit var tournaments: TournamentRepository

    @Mock
    protected lateinit var tournamentStates: TournamentStateRepository

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
