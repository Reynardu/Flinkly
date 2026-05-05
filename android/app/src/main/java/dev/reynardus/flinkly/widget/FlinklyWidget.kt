package dev.reynardus.flinkly.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.reynardus.flinkly.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val KEY_OPEN_COUNT = intPreferencesKey("open_count")
val KEY_TITLES = stringPreferencesKey("titles")
val KEY_TODAY_POINTS = intPreferencesKey("today_points")

class FlinklyWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { WidgetContent() }
    }

    @Composable
    private fun WidgetContent() {
        val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
        val openCount = prefs[KEY_OPEN_COUNT] ?: 0
        val titles = prefs[KEY_TITLES]
            ?.split("|")
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        val todayPoints = prefs[KEY_TODAY_POINTS] ?: 0

        GlanceTheme {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.surface)
                    .padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>()),
            ) {
                // Header
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "🏠 Flinkly",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Text(
                        text = "$todayPoints Pkt. heute",
                        style = TextStyle(
                            color = GlanceTheme.colors.secondary,
                            fontSize = 11.sp,
                        ),
                    )
                }

                Spacer(GlanceModifier.height(6.dp))

                // Task count badge
                Text(
                    text = when (openCount) {
                        0 -> "✅ Alle Aufgaben erledigt!"
                        1 -> "1 Aufgabe offen"
                        else -> "$openCount Aufgaben offen"
                    },
                    style = TextStyle(
                        color = if (openCount == 0)
                            GlanceTheme.colors.secondary
                        else
                            GlanceTheme.colors.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )

                if (titles.isNotEmpty()) {
                    Spacer(GlanceModifier.height(6.dp))
                    titles.take(4).forEach { title ->
                        Text(
                            text = "• $title",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp,
                            ),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

object WidgetUpdater {
    fun updateTasks(context: Context, openCount: Int, titles: List<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            applyState(context) { p ->
                p[KEY_OPEN_COUNT] = openCount
                p[KEY_TITLES] = titles.take(4).joinToString("|")
            }
        }
    }

    fun updatePoints(context: Context, todayPoints: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            applyState(context) { p -> p[KEY_TODAY_POINTS] = todayPoints }
        }
    }

    private suspend fun applyState(
        context: Context,
        block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit,
    ) {
        val manager = GlanceAppWidgetManager(context)
        val ids = manager.getGlanceIds(FlinklyWidget::class.java)
        ids.forEach { id ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                prefs.toMutablePreferences().also(block)
            }
            FlinklyWidget().update(context, id)
        }
    }
}
