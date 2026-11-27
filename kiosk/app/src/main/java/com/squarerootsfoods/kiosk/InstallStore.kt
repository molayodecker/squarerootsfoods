package com.squarerootsfoods.kiosk

import com.squarerootsfoods.kiosk.CoilCheck
import com.squarerootsfoods.kiosk.InstallUi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InstallStore(context: android.content.Context) {
    private val dao = KioskDatabase.get(context).installDao()
    private val gate = Mutex()
    private var visitId: Long = 0

    suspend fun latest(): InstallUi? = gate.withLock {
        val visit = dao.latestVisit() ?: return@withLock null
        visitId = visit.id
        toUi(visit, dao.marks(visit.id))
    }

    suspend fun record(machineNo: String, state: InstallUi, note: InstallNote?, finalize: Boolean) = gate.withLock {
        val current = if (visitId != 0L) dao.visit(visitId) else dao.latestVisit()
        val writingNew = current == null || (current.status == "saved" && !finalize)
        val now = System.currentTimeMillis()
        val status = if (finalize) "saved" else "draft"
        val id = if (writingNew) {
            dao.insertVisit(state.toVisit(machineNo, status, now))
        } else {
            dao.updateVisit(state.toVisit(machineNo, status, now).copy(id = current!!.id))
            current.id
        }
        visitId = id
        dao.clearMarks(id)
        if (state.checks.isNotEmpty()) {
            dao.insertMarks(state.checks.map { it.toMark(id) })
        }
        if (note != null) {
            dao.insertEvent(
                InstallEvent(
                    machineNo = machineNo,
                    visitId = id,
                    kind = note.kind,
                    lane = note.lane,
                    row = note.row,
                    coil = note.coil,
                    result = note.result,
                    detail = note.detail,
                    serial = state.serial,
                    baud = state.baud,
                    paymentReference = state.paymentReference,
                    paymentStatus = state.paymentStatus,
                    recordedAt = now,
                ),
            )
        }
    }

    private fun InstallUi.toVisit(machineNo: String, status: String, updatedAt: Long) = InstallVisit(
        machineNo = machineNo,
        rows = rows,
        coils = coils,
        board = board,
        serial = serial,
        baud = baud,
        simulate = simulate,
        phase = phase,
        coilIndex = index,
        paymentLane = paymentLane,
        paymentReference = paymentReference,
        paymentStatus = paymentStatus,
        status = status,
        updatedAt = updatedAt,
    )

    private fun CoilCheck.toMark(visitId: Long) = LaneMark(
        visitId = visitId,
        lane = lane,
        row = row,
        coil = coil,
        result = result,
        detail = detail,
    )

    private fun toUi(visit: InstallVisit, marks: List<LaneMark>) = InstallUi(
        rows = visit.rows,
        coils = visit.coils,
        board = visit.board,
        serial = visit.serial,
        baud = visit.baud,
        simulate = visit.simulate,
        checks = marks.map { CoilCheck(it.row, it.coil, it.lane, it.result, it.detail) },
        index = visit.coilIndex,
        phase = visit.phase,
        paymentLane = visit.paymentLane,
        paymentReference = visit.paymentReference,
        paymentStatus = visit.paymentStatus,
    )
}
