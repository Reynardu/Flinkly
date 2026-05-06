package dev.reynardus.flinkly.ui.screens.dashboard

import dev.reynardus.flinkly.R

sealed class RaccoonMood(
    val caption: String,
    val drawableRes: Int,
) {
    // ── Morgens (vor 10 Uhr) ───────────────────────────────────────────────
    data object MorningSleepy : RaccoonMood(
        caption = "Noch nicht ganz wach… ☕",
        drawableRes = R.drawable.raccoon_morning_sleepy,
    )
    data object MorningYawning : RaccoonMood(
        caption = "Schon wieder ein neuer Tag? 🥱",
        drawableRes = R.drawable.raccoon_morning_yawning,
    )

    // ── Gestern keine Aufgaben erledigt ────────────────────────────────────
    data object LazyLaundry : RaccoonMood(
        caption = "Der Wäscheberg hat gewonnen… 👀",
        drawableRes = R.drawable.raccoon_lazy_laundry,
    )
    data object LazyDishwasher : RaccoonMood(
        caption = "Da versteckt sich jemand! 🍽️",
        drawableRes = R.drawable.raccoon_lazy_dishwasher,
    )

    // ── Tagesziel erreicht ─────────────────────────────────────────────────
    data object DoneBroom : RaccoonMood(
        caption = "Alles blitzeblank! ✨",
        drawableRes = R.drawable.raccoon_done_broom,
    )
    data object DoneCelebrating : RaccoonMood(
        caption = "Was für ein produktiver Tag! 🎉",
        drawableRes = R.drawable.raccoon_done_celebrate,
    )

    // ── Haushaltspause ─────────────────────────────────────────────────────
    data object PausedSunglasses : RaccoonMood(
        caption = "Wohlverdienter Urlaub! 🏖️",
        drawableRes = R.drawable.raccoon_paused_sunglasses,
    )
    data object PausedHammock : RaccoonMood(
        caption = "Bitte nicht stören! 😎",
        drawableRes = R.drawable.raccoon_paused_hammock,
    )

    // ── Guter Fortschritt (≥ 50 %) ────────────────────────────────────────
    data object ProgressMotivated : RaccoonMood(
        caption = "Weiter so – fast am Ziel! 💪",
        drawableRes = R.drawable.raccoon_progress_motivated,
    )
    data object ProgressCleaning : RaccoonMood(
        caption = "Fleißig am Putzen! 🧹",
        drawableRes = R.drawable.raccoon_progress_cleaning,
    )

    // ── Bereit (normaler Tagesstart) ───────────────────────────────────────
    data object ReadyChecklist : RaccoonMood(
        caption = "Was steht heute an? 📋",
        drawableRes = R.drawable.raccoon_ready_checklist,
    )
    data object ReadySupplies : RaccoonMood(
        caption = "Bereit für alles! 🧺",
        drawableRes = R.drawable.raccoon_ready_supplies,
    )
}
