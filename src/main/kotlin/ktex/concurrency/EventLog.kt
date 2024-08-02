package ktex.concurrency

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.toList

enum class EventName {
    OPERATION_STARTED,
    OPERATION_INFO,
    OPERATION_COMPLETED,
    WORKER_STARTED,
    WORKER_COMPLETED,
    TASK_CREATED,
    TASK_STARTED,
    TASK_COMPLETED,
    CHANNEL_SEND,
    CHANNEL_RECEIVE,
}

data class Event(
    val name: EventName,
    val taskId: String? = null,
    val workerId: String? = null,
    val metadata: String? = null,
    val channelName: String? = null,
)

class EventLogCollector() {
    private val eventCh = Channel<Event>()

    suspend fun operationStarted() {
        eventCh.send(Event(EventName.OPERATION_STARTED))
    }
    suspend fun operationInfo(data: String? = null) {
        eventCh.send(Event(EventName.OPERATION_INFO, metadata = data))
    }
    suspend fun operationCompleted() {
        eventCh.send(Event(EventName.OPERATION_COMPLETED))
    }

    suspend fun workerStarted(workerId: String) {
        eventCh.send(Event(EventName.WORKER_STARTED, workerId = workerId))
    }
    suspend fun workerStarted(workerId: Int) =
        workerStarted(workerId.toString())
    suspend fun workerCompleted(workerId: String) {
        eventCh.send(Event(EventName.WORKER_COMPLETED, workerId = workerId))
    }
    suspend fun workerCompleted(workerId: Int) =
        workerCompleted(workerId.toString())

    suspend fun taskCreated(taskId: String) {
        eventCh.send(Event(EventName.TASK_CREATED, taskId = taskId))
    }
    suspend fun taskCreated(taskId: Int) =
        taskCreated(taskId.toString())
    suspend fun taskStarted(taskId: String, workerId: String? = null) {
        eventCh.send(Event(EventName.TASK_STARTED, taskId = taskId, workerId = workerId, metadata = currentCoroutineContext().toString()))
    }
    suspend fun taskStarted(taskId: Int, workerId: Int? = null) =
        taskStarted(taskId.toString(), workerId?.toString())
    suspend fun taskCompleted(taskId: String, workerId: String? = null) {
        eventCh.send(Event(EventName.TASK_COMPLETED, taskId = taskId, workerId = workerId, metadata = currentCoroutineContext().toString()))
    }
    suspend fun taskCompleted(taskId: Int, workerId: Int? = null) =
        taskCompleted(taskId.toString(), workerId?.toString())

    suspend fun channelSend(
        name: String,
        data: String,
        workerId: String? = null,
        taskId: String? = null,
    ) {
        eventCh.send(
            Event(
                EventName.CHANNEL_SEND,
                taskId = taskId,
                workerId = workerId,
                channelName = name,
                metadata = data,
            )
        )
    }
    suspend fun channelReceive(
        name: String,
        data: String,
        workerId: String? = null,
        taskId: String? = null,
    ) {
        eventCh.send(
            Event(
                EventName.CHANNEL_RECEIVE,
                taskId = taskId,
                workerId = workerId,
                channelName = name,
                metadata = data,
            )
        )
    }

    var receiver: Deferred<List<Event>>? = null
    suspend fun startCollector() {
        receiver = GlobalScope.async(Dispatchers.Unconfined) {
            eventCh.receiveAsFlow().toList()
        }
    }

    suspend fun receiveEvents() = coroutineScope {
        val eventReceiver = receiver

        check(eventReceiver != null)
        eventCh.close()

        eventReceiver.await()
    }

    suspend fun <T> run(block: suspend (EventLogCollector) -> Collection<T>): ResultsWithLog<T> {
        val collector = this

        return coroutineScope {
            startCollector()

            ResultsWithLog<T>(block(collector), receiveEvents())
        }
    }
}

fun Event.channelInfo() = "(W:${workerId} | T:${taskId}) $channelName "

fun Collection<Event>.printLog() {
    for (event in this) {
        when (event.name) {
            EventName.OPERATION_STARTED     -> println("[OP] started ->")
            EventName.OPERATION_COMPLETED   -> println("[OP] <- completed")
            EventName.OPERATION_INFO        -> println("[OP] ... info: ${event.metadata}")
            EventName.WORKER_STARTED        -> println("  [WKR ${event.workerId}] started ->")
            EventName.WORKER_COMPLETED      -> println("  [WKR ${event.workerId}] <- completed")
            EventName.TASK_CREATED          -> println("  [TASK ${event.taskId}] creating")
            EventName.TASK_STARTED          -> println("    [TASK ${event.taskId}] started ->")
            EventName.TASK_COMPLETED        -> println("    [TASK ${event.taskId}] <- completed")
            EventName.CHANNEL_SEND          -> println("[CH ${event.channelInfo()}] send -> ${event.metadata}")
            EventName.CHANNEL_RECEIVE       -> println("[CH ${event.channelInfo()}] <- received: ${event.metadata}")
        }
    }
}

data class ResultsWithLog<T>(
    val results: Collection<T>,
    val eventLog: Collection<Event>,
) {
    fun printLog() = eventLog.printLog()

    fun printRunDiagram() {
        for(x in 0..<results.size) {
            var running = false
            val runLine = eventLog.mapNotNull { event ->
                when (event.taskId) {
                    x.toString() -> when (event.name) {
                        EventName.TASK_CREATED -> "#"
                        EventName.TASK_STARTED -> {
                            running = true
                            "[${event.workerId ?: ""}"
                        }
                        EventName.TASK_COMPLETED -> {
                            running = false
                            "]"
                        }
                        else -> if (running) "." else null
                    }
                    else -> when (event.name) {
                        EventName.TASK_STARTED,
                        EventName.TASK_COMPLETED -> if (running) "." else " "
                        else -> null
                    }
                }
            }

            println("${x.toString().padStart(4)}: ${runLine.joinToString("")}")
        }
    }
}