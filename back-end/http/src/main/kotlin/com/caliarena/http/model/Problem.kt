package com.caliarena.http.model

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.net.URI

private const val PROBLEM_URI_PATH = "/problem"

sealed class Problem(
    typeUri: URI,
) {
    val type = typeUri.toString()
    val title = typeUri.toString().split("/").last()

    fun response(status: HttpStatus): ResponseEntity<Any> =
        ResponseEntity
            .status(status)
            .header(HttpHeaders.CONTENT_TYPE, MEDIA_TYPE)
            .body(ProblemBody(type, title, status.value()))

    data object InvalidRequestContent : Problem(URI("$PROBLEM_URI_PATH/invalid-request-content"))

    // User related Errors
    data object AlreadyUsedUsername : Problem(URI("$PROBLEM_URI_PATH/already-used-username"))

    data object InsecurePassword : Problem(URI("$PROBLEM_URI_PATH/insecure-password"))

    data object ErrorUpdatingUserRole : Problem(URI("$PROBLEM_URI_PATH/error-updating-user-role"))

    data object UserNotFound : Problem(URI("$PROBLEM_URI_PATH/not-found"))

    data object InvalidRole : Problem(URI("$PROBLEM_URI_PATH/invalid-role"))

    data object UserOrPasswordAreInvalid : Problem(URI("$PROBLEM_URI_PATH/user-or-password-are-invalid"))

    data object NotAuthorized : Problem(URI("$PROBLEM_URI_PATH/not-authorized"))

    // Athlete related Errors
    data object AthleteNotFound : Problem(URI("$PROBLEM_URI_PATH/athlete-not-found"))

    data object ErrorCreatingAthlete : Problem(URI("$PROBLEM_URI_PATH/error-creating-athlete"))

    data object InvalidGender : Problem(URI("$PROBLEM_URI_PATH/gender"))

    data object UpdatingAthlete : Problem(URI("$PROBLEM_URI_PATH/updating-athlete"))

    // Club related Errors
    data object ClubNotFound : Problem(URI("$PROBLEM_URI_PATH/club-not-found"))

    data object ClubAlreadyExists : Problem(URI("$PROBLEM_URI_PATH/club-already-exists"))

    data object CreatingClub : Problem(URI("$PROBLEM_URI_PATH/creating-club"))

    // Routines related Errors
    data object RoutineNotFound : Problem(URI("$PROBLEM_URI_PATH/routine-not-found"))

    data object RoutineAlreadyExists : Problem(URI("$PROBLEM_URI_PATH/routine-already-exists"))

    data object ExerciseTypeNotFound : Problem(URI("$PROBLEM_URI_PATH/exercise-type-not-found"))

    // Tournament related Errors
    data object TournamentNotFound : Problem(URI("$PROBLEM_URI_PATH/tournament-not-found"))

    data object TournamentAlreadyExists : Problem(URI("$PROBLEM_URI_PATH/tournament-already-exists"))

    data object BracketAlreadyExists : Problem(URI("$PROBLEM_URI_PATH/bracket-already-exists"))

    data object TournamentStateNotFound : Problem(URI("$PROBLEM_URI_PATH/tournament-state-not-found"))

    data object TournamentStateAlreadyExists : Problem(URI("$PROBLEM_URI_PATH/tournament-state-already-exists"))

    data object InvalidBracketStage : Problem(URI("$PROBLEM_URI_PATH/invalid-bracket-stage"))

    data object InvalidScreenState : Problem(URI("$PROBLEM_URI_PATH/invalid-screen-state"))

    data object InvalidTournamentStatus : Problem(URI("$PROBLEM_URI_PATH/invalid-tournament-status"))

    // Match related Errors
    data object MatchNotFound : Problem(URI("$PROBLEM_URI_PATH/match-not-found"))

    data object BracketNotFound : Problem(URI("$PROBLEM_URI_PATH/bracket-not-found"))

    data object AthleteNotInMatch : Problem(URI("$PROBLEM_URI_PATH/athlete-not-in-match"))

    data object MatchNotRunning : Problem(URI("$PROBLEM_URI_PATH/match-not-running"))

    data object ProgressNotFound : Problem(URI("$PROBLEM_URI_PATH/progress-not-found"))

    data object ProgressAlreadyExists : Problem(URI("$PROBLEM_URI_PATH/progress-already-exists"))

    data object AthletesNotAssigned : Problem(URI("$PROBLEM_URI_PATH/athletes-not-assigned"))

    data object JudgeNotFound : Problem(URI("$PROBLEM_URI_PATH/judge-not-found"))

    data object SameAthleteOnBothSides : Problem(URI("$PROBLEM_URI_PATH/same-athlete-on-both-sides"))

    data object ErrorCreatingMatchProg : Problem(URI("$PROBLEM_URI_PATH/error-creating-match-prog"))

    data object ExerciseNotFound : Problem(URI("$PROBLEM_URI_PATH/exercise-not-found"))

    data object MatchAlreadyStarted : Problem(URI("$PROBLEM_URI_PATH/match-already-started"))

    // Screen Routine related Errors
    data object ScreenRoutineNotFound : Problem(URI("$PROBLEM_URI_PATH/screen-routine-not-found"))

    data object TournamentMismatch : Problem(URI("$PROBLEM_URI_PATH/tournament-mismatch"))

    data object ErrorUpdatingScreenRoutine : Problem(URI("$PROBLEM_URI_PATH/error-updating-screen-routine"))

    private data class ProblemBody(
        val type: String,
        val title: String,
        val status: Int,
    )

    companion object {
        private const val MEDIA_TYPE = "application/problem+json"
    }
}
