package com.caliarena.data

enum class ErrorCode {
    UNKNOWN_ERROR,
    NO_CONNECTION,
    INVALID_CREDENTIALS,
    USERNAME_REQUIRED,
    PASSWORD_REQUIRED,
    INSECURE_PASSWORD,
    USERNAME_ALREADY_USED,
    USER_NOT_FOUND,
    NOT_AUTHORIZED,
    INVALID_ROLE,
    ERROR_UPDATING_USER_ROLE,
    ATHLETE_NOT_FOUND,
    INVALID_GENDER,
    ERROR_CREATING_ATHLETE,
    ERROR_UPDATING_ATHLETE,
    CLUB_NOT_FOUND,
    CLUB_ALREADY_EXISTS,
    ERROR_UPDATING_CLUB,
    ROUTINE_NOT_FOUND,
    ROUTINE_ALREADY_EXISTS,
    EXERCISE_TYPE_NOT_FOUND,
    TOURNAMENT_NOT_FOUND,
    TOURNAMENT_ALREADY_EXISTS,
    INVALID_TOURNAMENT_STATUS,
    TOURNAMENT_STATE_NOT_FOUND,
    TOURNAMENT_STATE_ALREADY_EXISTS,
    INVALID_SCREEN_STATE,
    BRACKET_NOT_FOUND,
    BRACKET_ALREADY_EXISTS,
    INVALID_BRACKET_STAGE,
    INVALID_BRACKET_DIVISION,
    MATCH_NOT_FOUND,
    ATHLETE_NOT_IN_MATCH,
    MATCH_NOT_RUNNING,
    PROGRESS_NOT_FOUND,
    PROGRESS_ALREADY_EXISTS,
    ATHLETES_NOT_ASSIGNED,
    JUDGE_NOT_FOUND,
    SAME_ATHLETE_ON_BOTH_SIDES,
    ERROR_CREATING_MATCH_PROG,
    EXERCISE_NOT_FOUND,
    MATCH_ALREADY_STARTED,
    OPPONENT_NOT_FINISHED,
    MATCH_NOT_FINISHED,
    SCREEN_ROUTINE_NOT_FOUND,
    TOURNAMENT_MISMATCH,
    ERROR_UPDATING_SCREEN_ROUTINE,
    INTERNAL_SERVER_ERROR,
    ;

    companion object {
        fun fromType(type: String?): ErrorCode = types[type] ?: UNKNOWN_ERROR

        private val types =
            mapOf(
                // User related
                "already-used-username" to USERNAME_ALREADY_USED,
                "insecure-password" to INSECURE_PASSWORD,
                "error-updating-user-role" to ERROR_UPDATING_USER_ROLE,
                "user-not-found" to USER_NOT_FOUND,
                "invalid-role" to INVALID_ROLE,
                "user-or-password-are-invalid" to INVALID_CREDENTIALS,
                "not-authorized" to NOT_AUTHORIZED,
                // Athlete related
                "athlete-not-found" to ATHLETE_NOT_FOUND,
                "invalid-gender" to INVALID_GENDER,
                "error-creating-athlete" to ERROR_CREATING_ATHLETE,
                "updating-athlete" to ERROR_UPDATING_ATHLETE,
                // Club related
                "club-not-found" to CLUB_NOT_FOUND,
                "club-already-exists" to CLUB_ALREADY_EXISTS,
                "creating-club" to ERROR_UPDATING_CLUB,
                // Routine related
                "routine-not-found" to ROUTINE_NOT_FOUND,
                "routine-already-exists" to ROUTINE_ALREADY_EXISTS,
                "exercise-type-not-found" to EXERCISE_TYPE_NOT_FOUND,
                // Tournament related
                "tournament-not-found" to TOURNAMENT_NOT_FOUND,
                "tournament-already-exists" to TOURNAMENT_ALREADY_EXISTS,
                "invalid-tournament-status" to INVALID_TOURNAMENT_STATUS,
                "tournament-state-not-found" to TOURNAMENT_STATE_NOT_FOUND,
                "tournament-state-already-exists" to TOURNAMENT_STATE_ALREADY_EXISTS,
                "invalid-screen-state" to INVALID_SCREEN_STATE,
                // Bracket related
                "bracket-not-found" to BRACKET_NOT_FOUND,
                "bracket-already-exists" to BRACKET_ALREADY_EXISTS,
                "invalid-bracket-stage" to INVALID_BRACKET_STAGE,
                "invalid-bracket-division" to INVALID_BRACKET_DIVISION,
                // Match related
                "match-not-found" to MATCH_NOT_FOUND,
                "athlete-not-in-match" to ATHLETE_NOT_IN_MATCH,
                "match-not-running" to MATCH_NOT_RUNNING,
                "progress-not-found" to PROGRESS_NOT_FOUND,
                "progress-already-exists" to PROGRESS_ALREADY_EXISTS,
                "athletes-not-assigned" to ATHLETES_NOT_ASSIGNED,
                "judge-not-found" to JUDGE_NOT_FOUND,
                "same-athlete-on-both-sides" to SAME_ATHLETE_ON_BOTH_SIDES,
                "error-creating-match-prog" to ERROR_CREATING_MATCH_PROG,
                "exercise-not-found" to EXERCISE_NOT_FOUND,
                "match-already-started" to MATCH_ALREADY_STARTED,
                "opponent-not-finished" to OPPONENT_NOT_FINISHED,
                "match-not-finished" to MATCH_NOT_FINISHED,
                // Screen routine related
                "screen-routine-not-found" to SCREEN_ROUTINE_NOT_FOUND,
                "tournament-mismatch" to TOURNAMENT_MISMATCH,
                "error-updating-screen-routine" to ERROR_UPDATING_SCREEN_ROUTINE,
                // General
                "internal-server-error" to INTERNAL_SERVER_ERROR,
            )
    }
}
