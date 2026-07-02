package com.example.handler

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SettingsChangeHandler {
    private val _restartServiceRequired = MutableStateFlow(false)
    val restartServiceRequired: StateFlow<Boolean> = _restartServiceRequired.asStateFlow()

    private val _uiUpdateRequired = MutableStateFlow(false)
    val uiUpdateRequired: StateFlow<Boolean> = _uiUpdateRequired.asStateFlow()

    fun triggerServiceRestart() {
        _restartServiceRequired.value = true
    }

    fun consumeServiceRestart(): Boolean {
        val result = _restartServiceRequired.value
        if (result) {
            _restartServiceRequired.value = false
        }
        return result
    }

    fun triggerUiUpdate() {
        _uiUpdateRequired.value = true
    }

    fun consumeUiUpdate(): Boolean {
        val result = _uiUpdateRequired.value
        if (result) {
            _uiUpdateRequired.value = false
        }
        return result
    }
}
