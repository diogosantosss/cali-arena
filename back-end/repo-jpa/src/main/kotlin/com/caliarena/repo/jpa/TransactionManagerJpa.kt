package com.caliarena.repo.jpa

import com.caliarena.Transaction
import com.caliarena.TransactionManager
import com.caliarena.repo.AthleteRepository
import com.caliarena.repo.ClubRepository
import com.caliarena.repo.EnduranceRoutineRepository
import com.caliarena.repo.MatchRepository
import com.caliarena.repo.TournamentRepository
import com.caliarena.repo.UserRepository
import com.caliarena.repo.jpa.athlete.AthleteRepositoryJpa
import com.caliarena.repo.jpa.club.ClubRepositoryJpa
import com.caliarena.repo.jpa.match.MatchEventRepositoryJpa
import com.caliarena.repo.jpa.match.MatchProgRepositoryJpa
import com.caliarena.repo.jpa.match.MatchRepositoryJpa
import com.caliarena.repo.jpa.routine.EndRoutineRepositoryJpa
import com.caliarena.repo.jpa.routine.ExerciseRepositoryJpa
import com.caliarena.repo.jpa.tournament.BracketRepositoryJpa
import com.caliarena.repo.jpa.tournament.TournamentRepositoryJpa
import com.caliarena.repo.jpa.tournament.TournamentStateRepositoryJpa
import com.caliarena.repo.jpa.user.TokenRepositoryJpa
import com.caliarena.repo.jpa.user.UserRepositoryJpa
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TransactionManagerJpa(
    // athlete and club related repos
    private val athleteRepositoryJpa: AthleteRepositoryJpa,
    private val clubRepositoryJpa: ClubRepositoryJpa,
    // match related repos
    private val matchRepositoryJpa: MatchRepositoryJpa,
    private val matchProgRepositoryJpa: MatchProgRepositoryJpa,
    private val matchEventRepositoryJpa: MatchEventRepositoryJpa,
    // routine related repos
    private val endRoutineRepositoryJpa: EndRoutineRepositoryJpa,
    private val exerciseRepositoryJpa: ExerciseRepositoryJpa,
    // tournament related repos
    private val bracketRepositoryJpa: BracketRepositoryJpa,
    private val tournamentRepositoryJpa: TournamentRepositoryJpa,
    private val tournamentStateRepositoryJpa: TournamentStateRepositoryJpa,
    // user related repos
    private val userRepositoryJpa: UserRepositoryJpa,
    private val tokenRepositoryJpa: TokenRepositoryJpa,
) : TransactionManager {
    @Transactional
    override fun <R> run(block: Transaction.() -> R): R {
        val repoAthlete = AthleteRepository(athleteRepositoryJpa, clubRepositoryJpa)
        val repoClub = ClubRepository(clubRepositoryJpa)
        val repoMatch =
            MatchRepository(
                matchEventRepositoryJpa,
                matchProgRepositoryJpa,
                matchRepositoryJpa,
                bracketRepositoryJpa,
                userRepositoryJpa,
            )
        val repoEnduranceRoutine =
            EnduranceRoutineRepository(
                endRoutineRepositoryJpa,
                exerciseRepositoryJpa,
            )
        val repoTournament =
            TournamentRepository(
                bracketRepositoryJpa,
                tournamentRepositoryJpa,
                tournamentStateRepositoryJpa,
            )
        val repoUser = UserRepository(userRepositoryJpa, tokenRepositoryJpa)

        return TransactionJpa(
            repoAthlete,
            repoClub,
            repoMatch,
            repoEnduranceRoutine,
            repoTournament,
            repoUser,
        ).block()
    }
}
