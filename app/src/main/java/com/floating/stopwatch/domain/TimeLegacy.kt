package com.floating.stopwatch.domain

enum class LegacyStatus {
    ACTIVE,
    PAUSED,
    COMPLETED,
    ARCHIVED
}

enum class LegacyProgressStatus {
    NOT_STARTED,
    IN_PROGRESS,
    ON_TRACK,
    AT_RISK,
    COMPLETED,
    OVERDUE
}

data class LegacyPhase(
    val id: String,
    val title: String,
    val startAt: Long? = null,
    val endAt: Long? = null,
    val targetDurationMillis: Long = 0L,
    val order: Int = 0
)

data class LegacyGoal(
    val id: String,
    val title: String,
    val targetDurationMillis: Long = 0L,
    val actualDurationMillis: Long = 0L,
    val phaseId: String? = null
)

data class LegacyMoment(
    val id: String,
    val legacyId: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String = "",
    val durationMillis: Long = 0L
)

data class LegacyJournalEntry(
    val id: String,
    val legacyId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val text: String
)

data class LegacyManualEntry(
    val id: String,
    val legacyId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMillis: Long = 0L,
    val note: String = ""
)

data class LegacyPostponement(
    val id: String,
    val legacyId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val addedDays: Int = 0,
    val reason: String = ""
)

data class TimeLegacy(
    val id: String,
    val title: String,
    val description: String = "",
    val daysCount: Int = 30,
    val dailyTargetMinutes: Int = 120,
    val startAt: Long? = null,
    val endAt: Long? = null,
    val targetDurationMillis: Long = 0L,
    val status: LegacyStatus = LegacyStatus.ACTIVE,
    val phases: List<LegacyPhase> = emptyList(),
    val goals: List<LegacyGoal> = emptyList(),
    val moments: List<LegacyMoment> = emptyList(),
    val journalEntries: List<LegacyJournalEntry> = emptyList(),
    val manualEntries: List<LegacyManualEntry> = emptyList(),
    val postponements: List<LegacyPostponement> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJsonObject(): org.json.JSONObject {
        val obj = org.json.JSONObject()
        obj.put("id", id)
        obj.put("title", title)
        obj.put("description", description)
        obj.put("daysCount", daysCount)
        obj.put("dailyTargetMinutes", dailyTargetMinutes)
        if (startAt != null) obj.put("startAt", startAt)
        if (endAt != null) obj.put("endAt", endAt)
        obj.put("targetDurationMillis", targetDurationMillis)
        obj.put("status", status.name)
        obj.put("createdAt", createdAt)
        obj.put("updatedAt", updatedAt)

        val phasesArray = org.json.JSONArray()
        phases.forEach { p ->
            val pObj = org.json.JSONObject()
            pObj.put("id", p.id)
            pObj.put("title", p.title)
            if (p.startAt != null) pObj.put("startAt", p.startAt)
            if (p.endAt != null) pObj.put("endAt", p.endAt)
            pObj.put("targetDurationMillis", p.targetDurationMillis)
            pObj.put("order", p.order)
            phasesArray.put(pObj)
        }
        obj.put("phases", phasesArray)

        val goalsArray = org.json.JSONArray()
        goals.forEach { g ->
            val gObj = org.json.JSONObject()
            gObj.put("id", g.id)
            gObj.put("title", g.title)
            gObj.put("targetDurationMillis", g.targetDurationMillis)
            gObj.put("actualDurationMillis", g.actualDurationMillis)
            if (g.phaseId != null) gObj.put("phaseId", g.phaseId)
            goalsArray.put(gObj)
        }
        obj.put("goals", goalsArray)

        val momentsArray = org.json.JSONArray()
        moments.forEach { m ->
            val mObj = org.json.JSONObject()
            mObj.put("id", m.id)
            mObj.put("legacyId", m.legacyId)
            mObj.put("title", m.title)
            mObj.put("timestamp", m.timestamp)
            mObj.put("message", m.message)
            mObj.put("durationMillis", m.durationMillis)
            momentsArray.put(mObj)
        }
        obj.put("moments", momentsArray)

        val journalsArray = org.json.JSONArray()
        journalEntries.forEach { j ->
            val jObj = org.json.JSONObject()
            jObj.put("id", j.id)
            jObj.put("legacyId", j.legacyId)
            jObj.put("timestamp", j.timestamp)
            jObj.put("text", j.text)
            journalsArray.put(jObj)
        }
        obj.put("journalEntries", journalsArray)

        val manualsArray = org.json.JSONArray()
        manualEntries.forEach { me ->
            val mObj = org.json.JSONObject()
            mObj.put("id", me.id)
            mObj.put("legacyId", me.legacyId)
            mObj.put("timestamp", me.timestamp)
            mObj.put("durationMillis", me.durationMillis)
            mObj.put("note", me.note)
            manualsArray.put(mObj)
        }
        obj.put("manualEntries", manualsArray)

        val postponesArray = org.json.JSONArray()
        postponements.forEach { post ->
            val pObj = org.json.JSONObject()
            pObj.put("id", post.id)
            pObj.put("legacyId", post.legacyId)
            pObj.put("timestamp", post.timestamp)
            pObj.put("addedDays", post.addedDays)
            pObj.put("reason", post.reason)
            postponesArray.put(pObj)
        }
        obj.put("postponements", postponesArray)

        return obj
    }

    companion object {
        fun fromJsonObject(obj: org.json.JSONObject): TimeLegacy {
            val phasesList = mutableListOf<LegacyPhase>()
            val phasesArray = obj.optJSONArray("phases")
            if (phasesArray != null) {
                for (i in 0 until phasesArray.length()) {
                    val pObj = phasesArray.getJSONObject(i)
                    phasesList.add(
                        LegacyPhase(
                            id = pObj.getString("id"),
                            title = pObj.getString("title"),
                            startAt = if (pObj.has("startAt")) pObj.getLong("startAt") else null,
                            endAt = if (pObj.has("endAt")) pObj.getLong("endAt") else null,
                            targetDurationMillis = pObj.optLong("targetDurationMillis", 0L),
                            order = pObj.optInt("order", 0)
                        )
                    )
                }
            }

            val goalsList = mutableListOf<LegacyGoal>()
            val goalsArray = obj.optJSONArray("goals")
            if (goalsArray != null) {
                for (i in 0 until goalsArray.length()) {
                    val gObj = goalsArray.getJSONObject(i)
                    goalsList.add(
                        LegacyGoal(
                            id = gObj.getString("id"),
                            title = gObj.getString("title"),
                            targetDurationMillis = gObj.optLong("targetDurationMillis", 0L),
                            actualDurationMillis = gObj.optLong("actualDurationMillis", 0L),
                            phaseId = if (gObj.has("phaseId")) gObj.getString("phaseId") else null
                        )
                    )
                }
            }

            val momentsList = mutableListOf<LegacyMoment>()
            val momentsArray = obj.optJSONArray("moments")
            if (momentsArray != null) {
                for (i in 0 until momentsArray.length()) {
                    val mObj = momentsArray.getJSONObject(i)
                    momentsList.add(
                        LegacyMoment(
                            id = mObj.getString("id"),
                            legacyId = mObj.optString("legacyId", ""),
                            title = mObj.getString("title"),
                            timestamp = mObj.optLong("timestamp", System.currentTimeMillis()),
                            message = mObj.optString("message", ""),
                            durationMillis = mObj.optLong("durationMillis", 0L)
                        )
                    )
                }
            }

            val journalsList = mutableListOf<LegacyJournalEntry>()
            val journalsArray = obj.optJSONArray("journalEntries")
            if (journalsArray != null) {
                for (i in 0 until journalsArray.length()) {
                    val jObj = journalsArray.getJSONObject(i)
                    journalsList.add(
                        LegacyJournalEntry(
                            id = jObj.getString("id"),
                            legacyId = jObj.optString("legacyId", ""),
                            timestamp = jObj.optLong("timestamp", System.currentTimeMillis()),
                            text = jObj.getString("text")
                        )
                    )
                }
            }

            val manualsList = mutableListOf<LegacyManualEntry>()
            val manualsArray = obj.optJSONArray("manualEntries")
            if (manualsArray != null) {
                for (i in 0 until manualsArray.length()) {
                    val mObj = manualsArray.getJSONObject(i)
                    manualsList.add(
                        LegacyManualEntry(
                            id = mObj.getString("id"),
                            legacyId = mObj.optString("legacyId", ""),
                            timestamp = mObj.optLong("timestamp", System.currentTimeMillis()),
                            durationMillis = mObj.optLong("durationMillis", 0L),
                            note = mObj.optString("note", "")
                        )
                    )
                }
            }

            val postponesList = mutableListOf<LegacyPostponement>()
            val postponesArray = obj.optJSONArray("postponements")
            if (postponesArray != null) {
                for (i in 0 until postponesArray.length()) {
                    val pObj = postponesArray.getJSONObject(i)
                    postponesList.add(
                        LegacyPostponement(
                            id = pObj.getString("id"),
                            legacyId = pObj.optString("legacyId", ""),
                            timestamp = pObj.optLong("timestamp", System.currentTimeMillis()),
                            addedDays = pObj.optInt("addedDays", 0),
                            reason = pObj.optString("reason", "")
                        )
                    )
                }
            }

            return TimeLegacy(
                id = obj.getString("id"),
                title = obj.getString("title"),
                description = obj.optString("description", ""),
                daysCount = obj.optInt("daysCount", 30),
                dailyTargetMinutes = obj.optInt("dailyTargetMinutes", 120),
                startAt = if (obj.has("startAt")) obj.getLong("startAt") else null,
                endAt = if (obj.has("endAt")) obj.getLong("endAt") else null,
                targetDurationMillis = obj.optLong("targetDurationMillis", 0L),
                status = try { LegacyStatus.valueOf(obj.optString("status", "ACTIVE")) } catch (e: Exception) { LegacyStatus.ACTIVE },
                phases = phasesList,
                goals = goalsList,
                moments = momentsList,
                journalEntries = journalsList,
                manualEntries = manualsList,
                postponements = postponesList,
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            )
        }

        fun parseListJson(jsonString: String): List<TimeLegacy> {
            if (jsonString.isBlank() || jsonString == "[]") return emptyList()
            return try {
                val array = org.json.JSONArray(jsonString)
                val list = mutableListOf<TimeLegacy>()
                for (i in 0 until array.length()) {
                    list.add(fromJsonObject(array.getJSONObject(i)))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun serializeListJson(legacies: List<TimeLegacy>): String {
            val array = org.json.JSONArray()
            legacies.forEach { array.put(it.toJsonObject()) }
            return array.toString()
        }
    }
}
