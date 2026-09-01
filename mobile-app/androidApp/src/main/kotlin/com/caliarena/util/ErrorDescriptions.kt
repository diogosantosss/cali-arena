package com.caliarena.util

import com.caliarena.R
import com.caliarena.data.ErrorCode

object ErrorDescriptions {
    private val descriptions =
        mapOf(
            // General errors
            ErrorCode.UNKNOWN_ERROR to R.string.error_unknown,
            ErrorCode.INTERNAL_SERVER_ERROR to R.string.error_internal_server,
            ErrorCode.USERNAME_REQUIRED to R.string.error_username_required,
            ErrorCode.PASSWORD_REQUIRED to R.string.error_password_required,
            ErrorCode.NO_CONNECTION to R.string.error_no_connection,
            // User related errors
            ErrorCode.INVALID_CREDENTIALS to R.string.error_invalid_credentials,
            ErrorCode.INSECURE_PASSWORD to R.string.error_insecure_password,
            ErrorCode.USERNAME_ALREADY_USED to R.string.error_already_used_username,
            ErrorCode.USER_NOT_FOUND to R.string.error_user_not_found,
            ErrorCode.NOT_AUTHORIZED to R.string.error_not_authorized,
            ErrorCode.INVALID_ROLE to R.string.error_invalid_role,
            ErrorCode.ERROR_UPDATING_USER_ROLE to R.string.error_updating_user_role,
            // Athlete related errors
            ErrorCode.ATHLETE_NOT_FOUND to R.string.error_athlete_not_found,
            ErrorCode.INVALID_GENDER to R.string.error_invalid_gender,
            ErrorCode.ERROR_CREATING_ATHLETE to R.string.error_creating_athlete,
            ErrorCode.ERROR_UPDATING_ATHLETE to R.string.error_updating_athlete,
            // Club related errors
            ErrorCode.CLUB_NOT_FOUND to R.string.error_club_not_found,
            ErrorCode.CLUB_ALREADY_EXISTS to R.string.error_club_already_exists,
            ErrorCode.ERROR_UPDATING_CLUB to R.string.error_updating_club,
            // Routine related errors
            ErrorCode.ROUTINE_NOT_FOUND to R.string.error_routine_not_found,
            ErrorCode.ROUTINE_ALREADY_EXISTS to R.string.error_routine_already_exists,
            ErrorCode.EXERCISE_TYPE_NOT_FOUND to R.string.error_exercise_type_not_found,
            // Tournament related errors
            ErrorCode.TOURNAMENT_NOT_FOUND to R.string.error_tournament_not_found,
            ErrorCode.TOURNAMENT_ALREADY_EXISTS to R.string.error_tournament_already_exists,
            ErrorCode.INVALID_TOURNAMENT_STATUS to R.string.error_invalid_tournament_status,
            ErrorCode.TOURNAMENT_STATE_NOT_FOUND to R.string.error_tournament_state_not_found,
            ErrorCode.TOURNAMENT_STATE_ALREADY_EXISTS to R.string.error_tournament_state_already_exists,
            ErrorCode.INVALID_SCREEN_STATE to R.string.error_invalid_screen_state,
            // Bracket related errors
            ErrorCode.BRACKET_NOT_FOUND to R.string.error_bracket_not_found,
            ErrorCode.BRACKET_ALREADY_EXISTS to R.string.error_bracket_already_exists,
            ErrorCode.INVALID_BRACKET_STAGE to R.string.error_invalid_bracket_stage,
            ErrorCode.INVALID_BRACKET_DIVISION to R.string.error_invalid_bracket_division,
            // Match related errors
            ErrorCode.MATCH_NOT_FOUND to R.string.error_match_not_found,
            ErrorCode.ATHLETE_NOT_IN_MATCH to R.string.error_athlete_not_in_match,
            ErrorCode.MATCH_NOT_RUNNING to R.string.error_match_not_running,
            ErrorCode.PROGRESS_NOT_FOUND to R.string.error_progress_not_found,
            ErrorCode.PROGRESS_ALREADY_EXISTS to R.string.error_progress_already_exists,
            ErrorCode.ATHLETES_NOT_ASSIGNED to R.string.error_athletes_not_assigned,
            ErrorCode.JUDGE_NOT_FOUND to R.string.error_judge_not_found,
            ErrorCode.SAME_ATHLETE_ON_BOTH_SIDES to R.string.error_same_athlete_on_both_sides,
            ErrorCode.ERROR_CREATING_MATCH_PROG to R.string.error_creating_match_prog,
            ErrorCode.EXERCISE_NOT_FOUND to R.string.error_exercise_not_found,
            ErrorCode.MATCH_ALREADY_STARTED to R.string.error_match_already_started,
            ErrorCode.OPPONENT_NOT_FINISHED to R.string.error_opponent_not_finished,
            ErrorCode.MATCH_NOT_FINISHED to R.string.error_match_not_finished,
            // Screen routine related errors
            ErrorCode.SCREEN_ROUTINE_NOT_FOUND to R.string.error_screen_routine_not_found,
            ErrorCode.TOURNAMENT_MISMATCH to R.string.error_tournament_mismatch,
            ErrorCode.ERROR_UPDATING_SCREEN_ROUTINE to R.string.error_updating_screen_routine,
        )

    fun getErrorDescription(code: ErrorCode): Int = descriptions[code] ?: R.string.error_unknown
}
