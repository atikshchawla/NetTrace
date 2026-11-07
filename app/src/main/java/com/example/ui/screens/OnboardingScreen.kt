package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.NetTraceIcons
import com.example.ui.theme.NetTraceColors

/**
 * Onboarding screen shown when Usage Access is not granted.
 * Provides clear explanation, offline privacy assurances, and opens system settings.
 */
@Composable
fun OnboardingScreen(
  onCheckPermissionAgain: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(NetTraceColors.Background)
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    // Hero telemetry pulse icon
    Box(
      modifier = Modifier
        .size(80.dp)
        .clip(CircleShape)
        .background(NetTraceColors.Raised)
        .border(1.dp, NetTraceColors.Hairline, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = NetTraceIcons.Cellular,
        contentDescription = "NetTrace Telemetry",
        tint = NetTraceColors.Wifi,
        modifier = Modifier.size(40.dp)
      )
    }

    Spacer(modifier = Modifier.height(28.dp))

    Text(
      text = "Usage Access Required",
      color = NetTraceColors.TextPrimary,
      fontSize = 28.sp,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = "NetTrace queries Android's NetworkStatsManager to measure accurate Wi-Fi and cellular traffic directly on your device.",
      color = NetTraceColors.TextSecondary,
      fontSize = 15.sp,
      lineHeight = 22.sp,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 8.dp)
    )

    Spacer(modifier = Modifier.height(32.dp))

    // Privacy cards
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(NetTraceColors.Surface)
        .border(1.dp, NetTraceColors.Hairline, RoundedCornerShape(24.dp))
        .padding(18.dp)
    ) {
      PrivacyBulletRow(
        title = "100% Offline by Design",
        subtitle = "NetTrace contains zero INTERNET permission. No bytes ever leave your device."
      )
      Spacer(modifier = Modifier.height(16.dp))
      PrivacyBulletRow(
        title = "Low Battery & Minimal Storage",
        subtitle = "Queries hardware counters on demand. Uses compact DataStore logs pruned to 60 days."
      )
    }

    Spacer(modifier = Modifier.height(36.dp))

    Button(
      onClick = {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
      },
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = NetTraceColors.Wifi,
        contentColor = NetTraceColors.TextPrimary
      ),
      shape = RoundedCornerShape(16.dp)
    ) {
      Text(
        text = "Open System Settings",
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Button(
      onClick = onCheckPermissionAgain,
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = NetTraceColors.Raised,
        contentColor = NetTraceColors.TextSecondary
      ),
      shape = RoundedCornerShape(16.dp)
    ) {
      Text(
        text = "I've Enabled It",
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium
      )
    }
  }
}

@Composable
private fun PrivacyBulletRow(
  title: String,
  subtitle: String
) {
  Row(verticalAlignment = Alignment.Top) {
    Box(
      modifier = Modifier
        .size(24.dp)
        .clip(CircleShape)
        .background(NetTraceColors.Raised),
      contentAlignment = Alignment.Center
    ) {
      Box(
        modifier = Modifier
          .size(8.dp)
          .clip(CircleShape)
          .background(NetTraceColors.Mobile)
      )
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column {
      Text(
        text = title,
        color = NetTraceColors.TextPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
      )
      Text(
        text = subtitle,
        color = NetTraceColors.TextTertiary,
        fontSize = 12.sp,
        lineHeight = 16.sp
      )
    }
  }
}
