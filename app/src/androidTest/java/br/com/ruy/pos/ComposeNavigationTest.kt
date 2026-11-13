package br.com.ruy.pos

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.ruy.pos.ui.activity.PosActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<PosActivity>()
    @Test fun homologationBannerAndNavigationRemainAvailableAfterActivityRecreation() {
        check(BuildConfig.HOMOLOGATION)
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Coxinha").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(Homologation.LABEL + " • DADOS FICTÍCIOS").assertIsDisplayed()
        compose.onNode(hasText("Mesas") and hasClickAction()).performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Mesa 05").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Mesa 05").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Coxinha").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(Homologation.LABEL + " • DADOS FICTÍCIOS").assertIsDisplayed()
        compose.onNode(hasText("Pedidos") and hasClickAction()).performClick()
        compose.onNodeWithText("Histórico").assertExists()
    }
}
