package com.caliarena.repo.trx

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
import org.springframework.stereotype.Component
import org.springframework.transaction.interceptor.TransactionAspectSupport

@Component
class TransactionJpa(
    override val users: UserRepository,
    override val tokens: TokenRepository,
    override val matches: MatchRepository,
    override val matchProgresses: MatchProgressRepository,
    override val athletes: AthleteRepository,
    override val clubs: ClubRepository,
    override val routines: EnduranceRoutineRepository,
    override val exercises: ExerciseRepository,
    override val screenRoutines: ScreenRoutineRepository,
    override val tournaments: TournamentRepository,
    override val brackets: BracketRepository,
    override val tournamentStates: TournamentStateRepository,
) : Transaction {
    override fun rollback() {
        TransactionAspectSupport
            .currentTransactionStatus()
            /*
             * This instructs the transaction manager that the only possible outcome of the transaction may be a rollback,
             * as alternative to throwing an exception which would in turn trigger a rollback.
             */
            .setRollbackOnly()
    }
}
