package com.example

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notifications.ShiftNotificationHelper
import com.example.ui.RibbonTab
import com.example.ui.ShiftUiState
import com.example.ui.ShiftViewModel
import com.example.ui.components.AeroTitleBar
import com.example.ui.components.OfficeRibbonTabs
import com.example.ui.i18n.AppStrings
import com.example.ui.screens.AboutTab
import com.example.ui.screens.RemindersTab
import com.example.ui.screens.ShiftDashboardTab
import com.example.ui.screens.TimetableTab
import com.example.ui.theme.LocalAeroPalette
import com.example.ui.theme.ShiftCompanionTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ShiftViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ShiftNotificationHelper.ensureChannels(this)
        handleNotificationIntent(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val strings = AppStrings.forLanguage(uiState.preferences.language)

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) {
                viewModel.refreshNowAndPermissions()
            }

            val settingsResultLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                viewModel.refreshNowAndPermissions()
            }

            LifecycleAndTimeObserver(
                onRefresh = { viewModel.refreshNowAndPermissions() }
            )

            ShiftCompanionTheme(
                darkTheme = uiState.preferences.darkTheme,
                language = uiState.preferences.language
            ) {
                ShiftCompanionScaffold(
                    uiState = uiState,
                    strings = strings,
                    onSelectTab = { viewModel.selectTab(it) },
                    onToggleLanguage = { viewModel.toggleLanguage() },
                    onToggleTheme = { viewModel.setDarkTheme(!uiState.preferences.darkTheme) },
                    onSelectDate = { viewModel.setShiftStartDate(it) },
                    onActivateShift = { viewModel.activateShift() },
                    onDeactivateShift = { viewModel.deactivateShift() },
                    onMarkBriefingComplete = { viewModel.markBriefingComplete() },
                    onToggleReminderOffset = { viewModel.toggleReminderOffset(it) },
                    onSetNotifyAtTransition = { viewModel.setNotifyAtTransition(it) },
                    onSetOngoingNotification = { viewModel.setOngoingStatusNotification(it) },
                    onSetUseDeviceTimezone = { viewModel.setUseDeviceTimezone(it) },
                    onRequestPostNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        } else {
                            viewModel.refreshNowAndPermissions()
                        }
                    },
                    onOpenExactAlarmSettings = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val settingsIntent = Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:$packageName")
                            )
                            settingsResultLauncher.launch(settingsIntent)
                        } else {
                            viewModel.refreshNowAndPermissions()
                        }
                    },
                    onOpenLiveUpdatesSettings = {
                        val promoIntent = Intent("android.settings.MANAGE_APP_PROMOTED_NOTIFICATIONS").apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                        }
                        val appNotifIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                        }
                        runCatching { settingsResultLauncher.launch(promoIntent) }
                            .onFailure {
                                runCatching { settingsResultLauncher.launch(appNotifIntent) }
                                    .onFailure { viewModel.refreshNowAndPermissions() }
                            }
                    },
                    onSendTestNotification = {
                        viewModel.sendTestNotification(
                            successMessage = strings.testNotificationSent,
                            deniedMessage = strings.testNotificationNeedsPermission
                        )
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(ShiftNotificationHelper.EXTRA_OPEN_SHIFT, false) == true) {
            viewModel.openShiftTabFromIntent()
        }
    }
}

@Composable
private fun LifecycleAndTimeObserver(
    onRefresh: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = androidx.compose.ui.platform.LocalContext.current

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        val timeReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                onRefresh()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(
            context,
            timeReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching { context.unregisterReceiver(timeReceiver) }
        }
    }
}

@Composable
fun ShiftCompanionScaffold(
    uiState: ShiftUiState,
    strings: AppStrings,
    onSelectTab: (RibbonTab) -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    onSelectDate: (java.time.LocalDate) -> Unit,
    onActivateShift: () -> Unit,
    onDeactivateShift: () -> Unit,
    onMarkBriefingComplete: () -> Unit,
    onToggleReminderOffset: (Int) -> Unit,
    onSetNotifyAtTransition: (Boolean) -> Unit,
    onSetOngoingNotification: (Boolean) -> Unit,
    onSetUseDeviceTimezone: (Boolean) -> Unit,
    onRequestPostNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenLiveUpdatesSettings: () -> Unit,
    onSendTestNotification: () -> Unit
) {
    val palette = LocalAeroPalette.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = palette.windowBg,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.windowBg)
                .padding(innerPadding)
        ) {
            AeroTitleBar(
                title = strings.appTitle,
                currentTimeFormatted = uiState.snapshot.currentTimeFormatted,
                zoneLabel = uiState.snapshot.zoneId.id,
                language = uiState.preferences.language,
                darkTheme = uiState.preferences.darkTheme,
                onToggleLanguage = onToggleLanguage,
                onToggleTheme = onToggleTheme
            )

            OfficeRibbonTabs(
                selectedTab = uiState.selectedTab,
                strings = strings,
                onSelectTab = onSelectTab
            )

            when (uiState.selectedTab) {
                RibbonTab.SHIFT -> ShiftDashboardTab(
                    uiState = uiState,
                    strings = strings,
                    onSelectDate = onSelectDate,
                    onActivateShift = onActivateShift,
                    onDeactivateShift = onDeactivateShift,
                    onMarkBriefingComplete = onMarkBriefingComplete,
                    modifier = Modifier.weight(1f)
                )
                RibbonTab.TIMETABLE -> TimetableTab(
                    snapshot = uiState.snapshot,
                    strings = strings,
                    modifier = Modifier.weight(1f)
                )
                RibbonTab.REMINDERS -> RemindersTab(
                    uiState = uiState,
                    strings = strings,
                    onToggleReminderOffset = onToggleReminderOffset,
                    onSetNotifyAtTransition = onSetNotifyAtTransition,
                    onSetOngoingNotification = onSetOngoingNotification,
                    onSetUseDeviceTimezone = onSetUseDeviceTimezone,
                    onRequestPostNotifications = onRequestPostNotifications,
                    onOpenExactAlarmSettings = onOpenExactAlarmSettings,
                    onOpenLiveUpdatesSettings = onOpenLiveUpdatesSettings,
                    onSendTestNotification = onSendTestNotification,
                    modifier = Modifier.weight(1f)
                )
                RibbonTab.ABOUT -> AboutTab(
                    strings = strings,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
