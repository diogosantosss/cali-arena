package com.caliarena.repo.jpa

import com.caliarena.repo.UserRepository
import com.caliarena.repo.jpa.athlete.AthleteRepositoryJpa
import com.caliarena.repo.jpa.club.ClubRepositoryJpa
import com.caliarena.repo.jpa.match.MatchEventRepositoryJpa
import com.caliarena.repo.jpa.match.MatchProgRepositoryJpa
import com.caliarena.repo.jpa.match.MatchRepositoryJpa
import com.caliarena.repo.jpa.routine.EndRoutineRepositoryJpa
import com.caliarena.repo.jpa.tournament.BracketRepositoryJpa
import com.caliarena.repo.jpa.tournament.TournamentRepositoryJpa
import com.caliarena.repo.jpa.tournament.TournamentStateRepositoryJpa
import com.caliarena.repo.jpa.user.TokenRepositoryJpa
import com.caliarena.repo.jpa.user.UserRepositoryJpa
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pt.isel.repo.Transaction
import pt.isel.repo.TransactionManager

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
    private val exerciseRepositoryJpa: ClubRepositoryJpa,
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
        val repoUser = UserRepository(userRepositoryJpa, tokenRepositoryJpa)

        return TransactionJpa(repoUser).block()
    }
}
