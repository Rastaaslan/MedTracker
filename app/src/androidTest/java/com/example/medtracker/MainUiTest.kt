package com.example.medtracker

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class MainUiTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    @Test fun todayAndTreatmentCreationAreReachable(){rule.onNodeWithText("Aujourd’hui").assertExists();rule.onNodeWithText("Traitements").performClick();rule.onNodeWithContentDescription("Ajouter un traitement").performClick();rule.onNodeWithText("Nouveau traitement").assertExists()}
}
