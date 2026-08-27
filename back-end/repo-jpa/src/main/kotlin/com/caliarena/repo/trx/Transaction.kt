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

interface Transaction {
    val users: UserRepository
    val tokens: TokenRepository
    val matches: MatchRepository
    val matchProgresses: MatchProgressRepository
    val athletes: AthleteRepository
    val clubs: ClubRepository
    val routines: EnduranceRoutineRepository
    val exercises: ExerciseRepository
    val screenRoutines: ScreenRoutineRepository
    val tournaments: TournamentRepository
    val brackets: BracketRepository
    val tournamentStates: TournamentStateRepository

    fun rollback()
}
