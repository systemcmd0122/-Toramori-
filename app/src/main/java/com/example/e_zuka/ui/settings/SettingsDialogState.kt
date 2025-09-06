package com.example.e_zuka.ui.settings

sealed class SettingsDialogState {
    data object None : SettingsDialogState()
    data class AddSkill(val initialValue: String = "") : SettingsDialogState()
    data class EditSkill(val skillId: Int, val currentValue: String) : SettingsDialogState()
    data class DeleteSkill(val skillId: Int, val skillName: String) : SettingsDialogState()
    data object LogoutConfirm : SettingsDialogState()
    data object PrivacyPolicy : SettingsDialogState()
}
