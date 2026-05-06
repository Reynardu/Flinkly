package dev.reynardus.flinkly.ui.screens.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RaccoonCard(
    mood: RaccoonMood,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = raccoonCardColor(mood),
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = mood.drawableRes),
                contentDescription = mood.caption,
                modifier = Modifier.size(100.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = raccoonSituationLabel(mood),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = mood.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun raccoonCardColor(mood: RaccoonMood) = when (mood) {
    is RaccoonMood.DoneBroom,
    is RaccoonMood.DoneCelebrating,
    -> MaterialTheme.colorScheme.primaryContainer

    is RaccoonMood.LazyLaundry,
    is RaccoonMood.LazyDishwasher,
    -> MaterialTheme.colorScheme.errorContainer

    is RaccoonMood.PausedSunglasses,
    is RaccoonMood.PausedHammock,
    -> MaterialTheme.colorScheme.tertiaryContainer

    is RaccoonMood.MorningSleepy,
    is RaccoonMood.MorningYawning,
    -> MaterialTheme.colorScheme.secondaryContainer

    else -> MaterialTheme.colorScheme.surfaceVariant
}

private fun raccoonSituationLabel(mood: RaccoonMood) = when (mood) {
    is RaccoonMood.MorningSleepy,
    is RaccoonMood.MorningYawning,
    -> "Guten Morgen!"

    is RaccoonMood.LazyLaundry,
    is RaccoonMood.LazyDishwasher,
    -> "Gestern Pause gemacht?"

    is RaccoonMood.DoneBroom,
    is RaccoonMood.DoneCelebrating,
    -> "Tagesziel erreicht!"

    is RaccoonMood.PausedSunglasses,
    is RaccoonMood.PausedHammock,
    -> "Haushaltspause"

    is RaccoonMood.ProgressMotivated,
    is RaccoonMood.ProgressCleaning,
    -> "Auf Kurs!"

    is RaccoonMood.ReadyChecklist,
    is RaccoonMood.ReadySupplies,
    -> "Bereit für heute!"
}
