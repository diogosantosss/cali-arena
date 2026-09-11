
export const errorDescriptions: Record<string, string> = {
  // User related errors
  "already-used-username":
    "The username is already in use. Please choose a different one.",
  "insecure-password":
    "The password provided is not secure enough. Please choose a stronger password.",
  "error-updating-user-role":
    "There was an error updating the user's role. Please try again later.",
  "user-not-found":
    "The specified user could not be found. Please check the username and try again.",
  "invalid-role":
    "The role specified is invalid. Please choose a valid role.",
  "user-or-password-are-invalid":
    "The username or password provided is incorrect. Please check your credentials and try again.",
  "not-authorized":
    "You are not authorized to perform this action. Please check your permissions and try again.",

  // Athlete related errors
  "athlete-not-found":
    "The athlete could not be found. Please check the provided details and try again.",
  "error-creating-athlete":
    "There was an error creating the athlete. Please try again later.",
  "invalid-gender":
    "The gender provided is invalid. Please choose a valid option.",
  "updating-athlete":
    "There was an error updating the athlete. Please try again later.",

  // Club related errors
  "club-not-found":
    "The club could not be found. Please check the provided details and try again.",
  "club-already-exists":
    "The club already exists. Please choose a different name.",
  "creating-club":
    "There was an error creating the club. Please try again later.",

  // Routines related errors
  "routine-not-found":
    "The routine could not be found. Please check the provided details and try again.",
  "routine-already-exists":
    "The routine already exists. Please choose a different name.",
  "exercise-type-not-found":
    "The exercise type could not be found. Please check the provided details and try again.",

  // Tournament related errors
  "tournament-not-found":
    "The tournament could not be found. Please check the provided details and try again.",
  "tournament-already-exists":
    "The tournament already exists. Please choose a different name.",
  "bracket-already-exists":
    "The bracket already exists for this tournament.",
  "tournament-state-not-found":
    "The tournament state could not be found. Please try again later.",
  "tournament-state-already-exists":
    "The tournament state already exists. Please try again later.",
  "invalid-bracket-stage":
    "The bracket stage is invalid for the current tournament state.",
  "invalid-bracket-division":
    "The bracket division is invalid or has no brackets. Please choose a valid division.",
  "invalid-screen-state":
    "The screen state is invalid for the current tournament flow.",
  "invalid-tournament-status":
    "The tournament status is invalid. Please check the current state and try again.",

  // Match related errors
  "match-not-found":
    "The match could not be found. Please check the provided details and try again.",
  "bracket-not-found":
    "The bracket could not be found. Please check the provided details and try again.",
  "athlete-not-in-match":
    "The athlete is not assigned to this match.",
  "match-not-running":
    "The match is not currently running.",
  "progress-not-found":
    "The match progress could not be found. Please try again later.",
  "progress-already-exists":
    "The match progress already exists for this match.",
  "athletes-not-assigned":
    "The athletes have not been assigned to this match yet.",
  "judge-not-found":
    "The judge could not be found. Please check the provided details and try again.",
  "same-athlete-on-both-sides":
    "The same athlete cannot be assigned to both sides of the match.",
  "error-creating-match-prog":
    "There was an error creating the match progress. Please try again later.",
  "exercise-not-found":
    "The exercise could not be found. Please check the provided details and try again.",
  "opponent-not-finished":
    "You can only finish an athlete after the opponent has finished their routine.",
  "match-already-started":
    "The match has already started.",
  "match-not-finished":
    "The match has not finished yet.",

  // Screen routine related errors
  "screen-routine-not-found":
    "The screen routine could not be found. Please check the provided details and try again.",
  "tournament-mismatch":
    "The screen routine does not belong to this tournament.",
  "error-updating-screen-routine":
    "There was an error updating the screen routine. Please try again later.",
};

export function getErrorDescription(errorType: string): string {
  return errorDescriptions[errorType] || errorType;
}