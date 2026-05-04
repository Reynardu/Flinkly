package dev.reynardus.flinkly.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class FlinklyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = FlinklyWidget()
}
