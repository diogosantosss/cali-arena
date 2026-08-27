package com.caliarena.service.sse

import com.caliarena.repo.trx.TransactionManager
import jakarta.annotation.PreDestroy
import jakarta.inject.Named
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Named
class SpectatorPublisher(
    private val trx: TransactionManager,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(SpectatorPublisher::class.java)

        private const val KEEP_ALIVE_PERIOD = 10L
        private const val N_OF_THREADS = 1
    }

    private val listeners = mutableMapOf<Int, List<SpectatorEmitter>>()
    private val guard = ReentrantLock()
    private var currentId = 0L

    // A scheduler to send the periodic keep-alive events
    private val scheduler: ScheduledExecutorService =
        Executors.newScheduledThreadPool(N_OF_THREADS).also {
            it.scheduleAtFixedRate(::keepAlive, 2, KEEP_ALIVE_PERIOD, TimeUnit.SECONDS)
        }

    fun publish(event: SpectatorEvent) =
        guard.withLock {
            val eventId = ++currentId
            logger.debug( // logging at dev environment level, to avoid cluttering production logs
                "Publishing eventId: {}, tournamentId: {}, action: {}",
                eventId,
                event.tournamentId,
                event.action,
            )
            listeners[event.tournamentId].orEmpty().forEach { listener ->
                try {
                    listener.emit(eventId, event)
                } catch (ex: Exception) {
                    logger.error("Exception while publishing event - {}", ex.message)
                }
            }
        }

    fun addEmitter(
        tournamentId: Int,
        listener: SpectatorEmitter,
    ) = guard.withLock {
        val tournament =
            trx.run {
                tournaments.findByIdOrNull(tournamentId)?.toDomain()
            }
        requireNotNull(tournament) { "Tournament with id $tournamentId not found" }

        listeners[tournamentId] = listeners.getOrDefault(tournamentId, emptyList()) + listener

        logger.info(
            "Adding listener to tournamentId: {}, currentListeners: {}",
            tournamentId,
            listeners[tournamentId]?.size ?: 0,
        )

        listener.onCompletion { removeEmitter(tournamentId, listener) }

        listener.onError { removeEmitter(tournamentId, listener) }

        listener
    }

    private fun removeEmitter(
        tournamentId: Int,
        listener: SpectatorEmitter,
    ) = guard.withLock {
        val oldListeners = listeners[tournamentId]
        requireNotNull(oldListeners)
        listeners.replace(tournamentId, oldListeners - listener)

        logger.info(
            "Removing listener from tournamentId: {}, currentListeners: {}",
            tournamentId,
            oldListeners.size - 1,
        )
    }

    private fun keepAlive() =
        guard.withLock {
            val listeners =
                listeners.values
                    .flatten()
                    .also { if (it.isEmpty()) return@withLock }

            logger.info("Sending keep-alive signal to {} listeners", listeners.size)

            val keepAliveEvent = KeepAliveEvent(Instant.now())
            listeners.forEach {
                try {
                    it.keepAlive(keepAliveEvent)
                } catch (ex: Exception) {
                    logger.error("Exception while sending keep-alive signal - {}", ex.message)
                }
            }
        }

    @PreDestroy
    fun shutdown() {
        logger.info("Shutting down spectator keep-alive scheduler")
        scheduler.shutdown()
    }
}
