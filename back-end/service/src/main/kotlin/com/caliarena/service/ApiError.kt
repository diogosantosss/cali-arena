package com.caliarena.service

import org.springframework.http.HttpStatus

/**
 * Shared set of errors returned by services via Either.
 *
 * `problemType` matches the suffix of the RFC 7807 problem URI, and is used by
 * the web app to look up a user-facing description.
 */
enum class ApiError(
    val problemType: String,
    val status: HttpStatus,
) {
    // User related
    ALREADY_USED_USERNAME("already-used-username", HttpStatus.CONFLICT),

    INSECURE_PASSWORD("insecure-password", HttpStatus.BAD_REQUEST),

    ERROR_UPDATING_USER_ROLE("error-updating-user-role", HttpStatus.BAD_REQUEST),

    USER_NOT_FOUND("user-not-found", HttpStatus.NOT_FOUND),

    INVALID_ROLE("invalid-role", HttpStatus.BAD_REQUEST),

    USER_OR_PASSWORD_ARE_INVALID("user-or-password-are-invalid", HttpStatus.UNAUTHORIZED),

    NOT_AUTHORIZED("not-authorized", HttpStatus.FORBIDDEN),

    // Athlete related
    ATHLETE_NOT_FOUND("athlete-not-found", HttpStatus.NOT_FOUND),

    INVALID_GENDER("invalid-gender", HttpStatus.BAD_REQUEST),

    ERROR_CREATING_ATHLETE("error-creating-athlete", HttpStatus.BAD_REQUEST),

    UPDATING_ATHLETE("updating-athlete", HttpStatus.BAD_REQUEST),

    // Club related
    CLUB_NOT_FOUND("club-not-found", HttpStatus.NOT_FOUND),

    CLUB_ALREADY_EXISTS("club-already-exists", HttpStatus.CONFLICT),

    UPDATING_CLUB("creating-club", HttpStatus.BAD_REQUEST),

    // Routine related
    ROUTINE_NOT_FOUND("routine-not-found", HttpStatus.NOT_FOUND),

    ROUTINE_ALREADY_EXISTS("routine-already-exists", HttpStatus.CONFLICT),

    EXERCISE_TYPE_NOT_FOUND("exercise-type-not-found", HttpStatus.NOT_FOUND),

    // Tournament related
    TOURNAMENT_NOT_FOUND("tournament-not-found", HttpStatus.NOT_FOUND),

    TOURNAMENT_ALREADY_EXISTS("tournament-already-exists", HttpStatus.CONFLICT),

    INVALID_TOURNAMENT_STATUS("invalid-tournament-status", HttpStatus.BAD_REQUEST),

    TOURNAMENT_STATE_NOT_FOUND("tournament-state-not-found", HttpStatus.NOT_FOUND),

    TOURNAMENT_STATE_ALREADY_EXISTS("tournament-state-already-exists", HttpStatus.CONFLICT),

    INVALID_SCREEN_STATE("invalid-screen-state", HttpStatus.BAD_REQUEST),

    // Bracket related
    BRACKET_NOT_FOUND("bracket-not-found", HttpStatus.NOT_FOUND),

    BRACKET_ALREADY_EXISTS("bracket-already-exists", HttpStatus.CONFLICT),

    INVALID_BRACKET_STAGE("invalid-bracket-stage", HttpStatus.BAD_REQUEST),

    INVALID_BRACKET_DIVISION("invalid-bracket-division", HttpStatus.BAD_REQUEST),

    // Match related
    MATCH_NOT_FOUND("match-not-found", HttpStatus.NOT_FOUND),

    ATHLETE_NOT_IN_MATCH("athlete-not-in-match", HttpStatus.BAD_REQUEST),

    MATCH_NOT_RUNNING("match-not-running", HttpStatus.CONFLICT),

    PROGRESS_NOT_FOUND("progress-not-found", HttpStatus.NOT_FOUND),

    PROGRESS_ALREADY_EXISTS("progress-already-exists", HttpStatus.CONFLICT),

    ATHLETES_NOT_ASSIGNED("athletes-not-assigned", HttpStatus.BAD_REQUEST),

    JUDGE_NOT_FOUND("judge-not-found", HttpStatus.NOT_FOUND),

    SAME_ATHLETE_ON_BOTH_SIDES("same-athlete-on-both-sides", HttpStatus.BAD_REQUEST),

    ERROR_CREATING_MATCH_PROG("error-creating-match-prog", HttpStatus.INTERNAL_SERVER_ERROR),

    EXERCISE_NOT_FOUND("exercise-not-found", HttpStatus.NOT_FOUND),

    MATCH_ALREADY_STARTED("match-already-started", HttpStatus.CONFLICT),

    OPPONENT_NOT_FINISHED("opponent-not-finished", HttpStatus.CONFLICT),

    MATCH_NOT_FINISHED("match-not-finished", HttpStatus.CONFLICT),

    // Screen routine related
    SCREEN_ROUTINE_NOT_FOUND("screen-routine-not-found", HttpStatus.NOT_FOUND),

    TOURNAMENT_MISMATCH("tournament-mismatch", HttpStatus.CONFLICT),

    ERROR_UPDATING_SCREEN_ROUTINE("error-updating-screen-routine", HttpStatus.INTERNAL_SERVER_ERROR),
}
