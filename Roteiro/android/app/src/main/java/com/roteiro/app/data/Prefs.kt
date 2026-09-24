package com.roteiro.app.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Onde o usuário está agora, segundo o app. */
data class ContextState(
    val currentPlaceId: Long? = null,
    val arrivedAt: Long = 0,
    val lastLeftPlaceId: Long? = null,
    val lastLeftAt: Long = 0,
    /** Perto de um estabelecimento não salvo (lembretes "qualquer mercado"). */
    val storeId: String? = null,
    val storeName: String? = null,
    val storeCategory: String? = null,
    val storeAt: Long = 0,
) {
    /** O contexto de estabelecimento vale por 30 min, ou até sair de perto dele. */
    fun nearStore(now: Long = System.currentTimeMillis()) = storeCategory != null && now - storeAt < 30 * 60_000
}

data class Settings(
    val onboardingDone: Boolean = false,
    /** Tempo mínimo no lugar antes de avisar a chegada. */
    val dwellMinutes: Int = 2,
    /** Entre 22h e 7h os avisos chegam sem som. */
    val quietHours: Boolean = true,
)

/** Preferências pequenas (SharedPreferences) expostas como StateFlow. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("roteiro", Context.MODE_PRIVATE)

    private val _context = MutableStateFlow(readContext())
    val context: StateFlow<ContextState> = _context.asStateFlow()

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private fun readContext() = ContextState(
        currentPlaceId = sp.getLong(K_CURRENT, -1).takeIf { it >= 0 },
        arrivedAt = sp.getLong(K_ARRIVED_AT, 0),
        lastLeftPlaceId = sp.getLong(K_LAST_LEFT, -1).takeIf { it >= 0 },
        lastLeftAt = sp.getLong(K_LAST_LEFT_AT, 0),
        storeId = sp.getString(K_STORE_ID, null),
        storeName = sp.getString(K_STORE_NAME, null),
        storeCategory = sp.getString(K_STORE_CAT, null),
        storeAt = sp.getLong(K_STORE_AT, 0),
    )

    private fun readSettings() = Settings(
        onboardingDone = sp.getBoolean(K_ONBOARDING, false),
        dwellMinutes = sp.getInt(K_DWELL, 2),
        quietHours = sp.getBoolean(K_QUIET, true),
    )

    fun arrive(placeId: Long, at: Long = System.currentTimeMillis()) {
        sp.edit { putLong(K_CURRENT, placeId); putLong(K_ARRIVED_AT, at) }
        _context.value = readContext()
    }

    fun leave(placeId: Long, at: Long = System.currentTimeMillis()) {
        sp.edit {
            if (sp.getLong(K_CURRENT, -1) == placeId) remove(K_CURRENT)
            putLong(K_LAST_LEFT, placeId)
            putLong(K_LAST_LEFT_AT, at)
        }
        _context.value = readContext()
    }

    fun nearStore(id: String, name: String?, category: String, at: Long = System.currentTimeMillis()) {
        sp.edit { putString(K_STORE_ID, id); putString(K_STORE_NAME, name); putString(K_STORE_CAT, category); putLong(K_STORE_AT, at) }
        _context.value = readContext()
    }

    fun clearStore() {
        sp.edit { remove(K_STORE_ID); remove(K_STORE_NAME); remove(K_STORE_CAT); remove(K_STORE_AT) }
        _context.value = readContext()
    }

    fun clearCurrent() {
        sp.edit { remove(K_CURRENT) }
        _context.value = readContext()
    }

    fun setOnboardingDone() = updateSettings { putBoolean(K_ONBOARDING, true) }
    fun setDwellMinutes(min: Int) = updateSettings { putInt(K_DWELL, min) }
    fun setQuietHours(on: Boolean) = updateSettings { putBoolean(K_QUIET, on) }

    fun clearAll() {
        sp.edit { clear() }
        _context.value = readContext()
        _settings.value = readSettings()
    }

    private fun updateSettings(block: android.content.SharedPreferences.Editor.() -> Unit) {
        sp.edit(action = block)
        _settings.value = readSettings()
    }

    private companion object {
        const val K_CURRENT = "current_place"
        const val K_ARRIVED_AT = "arrived_at"
        const val K_LAST_LEFT = "last_left_place"
        const val K_LAST_LEFT_AT = "last_left_at"
        const val K_ONBOARDING = "onboarding_done"
        const val K_DWELL = "dwell_min"
        const val K_QUIET = "quiet_hours"
        const val K_STORE_ID = "store_id"
        const val K_STORE_NAME = "store_name"
        const val K_STORE_CAT = "store_cat"
        const val K_STORE_AT = "store_at"
    }
}
