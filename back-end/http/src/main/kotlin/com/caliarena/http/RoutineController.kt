package com.caliarena.http

import com.caliarena.http.model.Problem
import com.caliarena.http.model.routine.CreateExerciseInput
import com.caliarena.http.model.routine.CreateRoutineInput
import com.caliarena.http.utils.toResponse
import com.caliarena.service.RoutineError
import com.caliarena.service.RoutineService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/routines")
class RoutineController(
    private val routineService: RoutineService,
) {
    @PostMapping
    fun createRoutine(
        @RequestBody input: CreateRoutineInput,
    ): ResponseEntity<Any> =
        routineService
            .createRoutine(
                input.name,
                input.timeCapSeconds,
            ).toResponse(
                onSuccess = { routine ->
                    ResponseEntity
                        .status(HttpStatus.CREATED)
                        .header(HttpHeaders.LOCATION, "/api/routines/${routine.id}")
                        .body(routine)
                },
                onError = { it.toResponseEntity() },
            )

    @PostMapping("/exercises")
    fun createExercise(
        @RequestBody input: CreateExerciseInput,
    ): ResponseEntity<Any> =
        routineService
            .createExercise(
                input.routineId,
                input.name,
                input.targetReps,
                input.addedWeight,
                input.exerciseOrder,
                input.supersetOrder,
                input.type,
            ).toResponse(
                onSuccess = { exercise ->
                    ResponseEntity
                        .status(HttpStatus.CREATED)
                        .header(HttpHeaders.LOCATION, "/api/routines/${exercise.routineId}/exercises/${exercise.id}")
                        .body(exercise)
                },
                onError = { it.toResponseEntity() },
            )

    @GetMapping("/{routineName}/overview")
    fun getRoutineOverview(
        @PathVariable routineName: String,
    ): ResponseEntity<Any> =
        routineService
            .getRoutineOverview(routineName)
            .toResponse(
                onSuccess = { overview ->
                    ResponseEntity
                        .status(HttpStatus.OK)
                        .body(overview)
                },
                onError = { it.toResponseEntity() },
            )

    private fun RoutineError.toResponseEntity(): ResponseEntity<Any> =
        when (this) {
            RoutineError.RoutineNotFound ->
                Problem.RoutineNotFound.response(HttpStatus.NOT_FOUND)

            RoutineError.ExerciseTypeNotFound ->
                Problem.ExerciseTypeNotFound.response(HttpStatus.NOT_FOUND)

            RoutineError.RoutineAlreadyExists ->
                Problem.RoutineAlreadyExists.response(HttpStatus.CONFLICT)
        }
}
