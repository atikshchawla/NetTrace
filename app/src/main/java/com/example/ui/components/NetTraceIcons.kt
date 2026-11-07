package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 10 lightweight custom vector icons designed specifically for NetTrace.
 * Completely eliminates heavy icon-extended libraries to guarantee <3MB APK size.
 */
object NetTraceIcons {

  val Wifi: ImageVector by lazy {
    ImageVector.Builder(
      name = "Wifi",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round
      ) {
        moveTo(5f, 9f)
        curveTo(8.5f, 5.5f, 15.5f, 5.5f, 19f, 9f)
        moveTo(8f, 13f)
        curveTo(10.2f, 10.8f, 13.8f, 10.8f, 16f, 13f)
      }
      path(fill = SolidColor(Color.White)) {
        moveTo(12f, 17f)
        lineTo(11f, 18f)
        lineTo(12f, 19f)
        lineTo(13f, 18f)
        close()
      }
    }.build()
  }

  val Cellular: ImageVector by lazy {
    ImageVector.Builder(
      name = "Cellular",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      path(fill = SolidColor(Color.White)) {
        // 4 ascending bars
        moveTo(4f, 18f); lineTo(6f, 18f); lineTo(6f, 14f); lineTo(4f, 14f); close()
        moveTo(9f, 18f); lineTo(11f, 18f); lineTo(11f, 11f); lineTo(9f, 11f); close()
        moveTo(14f, 18f); lineTo(16f, 18f); lineTo(16f, 8f); lineTo(14f, 8f); close()
        moveTo(19f, 18f); lineTo(21f, 18f); lineTo(21f, 5f); lineTo(19f, 5f); close()
      }
    }.build()
  }

  val SimCard: ImageVector by lazy {
    ImageVector.Builder(
      name = "SimCard",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineJoin = StrokeJoin.Round
      ) {
        moveTo(7f, 4f)
        lineTo(14f, 4f)
        lineTo(18f, 8f)
        lineTo(18f, 20f)
        lineTo(7f, 20f)
        close()
      }
      path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.5f
      ) {
        moveTo(10f, 11f); lineTo(15f, 11f)
        moveTo(10f, 14f); lineTo(15f, 14f)
        moveTo(10f, 17f); lineTo(15f, 17f)
      }
    }.build()
  }

  val Gear: ImageVector by lazy {
    ImageVector.Builder(
      name = "Gear",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      path(
        fill = SolidColor(Color.White)
      ) {
        moveTo(19.14f, 12.94f)
        curveTo(19.18f, 12.64f, 19.2f, 12.33f, 19.2f, 12f)
        curveTo(19.2f, 11.68f, 19.18f, 11.36f, 19.13f, 11.06f)
        lineTo(21.16f, 9.48f)
        curveTo(21.34f, 9.34f, 21.39f, 9.07f, 21.28f, 8.87f)
        lineTo(19.36f, 5.55f)
        curveTo(19.24f, 5.33f, 18.99f, 5.26f, 18.77f, 5.33f)
        lineTo(16.38f, 6.29f)
        curveTo(15.88f, 5.91f, 15.35f, 5.59f, 14.76f, 5.35f)
        lineTo(14.4f, 2.81f)
        curveTo(14.36f, 2.57f, 14.16f, 2.4f, 13.92f, 2.4f)
        lineTo(10.08f, 2.4f)
        curveTo(9.84f, 2.4f, 9.65f, 2.57f, 9.61f, 2.81f)
        lineTo(9.25f, 5.35f)
        curveTo(8.66f, 5.59f, 8.12f, 5.92f, 7.63f, 6.29f)
        lineTo(5.24f, 5.33f)
        curveTo(5.02f, 5.25f, 4.77f, 5.33f, 4.65f, 5.55f)
        lineTo(2.73f, 8.87f)
        curveTo(2.61f, 9.08f, 2.65f, 9.34f, 2.85f, 9.48f)
        lineTo(4.88f, 11.06f)
        curveTo(4.83f, 11.36f, 4.8f, 11.69f, 4.8f, 12f)
        curveTo(4.8f, 12.31f, 4.83f, 12.64f, 4.88f, 12.94f)
        lineTo(2.85f, 14.52f)
        curveTo(2.67f, 14.66f, 2.62f, 14.93f, 2.73f, 15.13f)
        lineTo(4.65f, 18.45f)
        curveTo(4.77f, 18.67f, 5.02f, 18.74f, 5.24f, 18.67f)
        lineTo(7.63f, 17.71f)
        curveTo(8.13f, 18.09f, 8.66f, 18.41f, 9.25f, 18.65f)
        lineTo(9.61f, 21.19f)
        curveTo(9.65f, 21.43f, 9.85f, 21.6f, 10.08f, 21.6f)
        lineTo(13.92f, 21.6f)
        curveTo(14.16f, 21.6f, 14.36f, 21.43f, 14.4f, 21.19f)
        lineTo(14.76f, 18.65f)
        curveTo(15.35f, 18.41f, 15.89f, 18.08f, 16.38f, 17.71f)
        lineTo(18.77f, 18.67f)
        curveTo(18.99f, 18.75f, 19.24f, 18.67f, 19.36f, 18.45f)
        lineTo(21.28f, 15.13f)
        curveTo(21.4f, 14.92f, 21.35f, 14.66f, 21.16f, 14.52f)
        close()
        // Concentric inner hole
        moveTo(12f, 15.5f)
        curveTo(10.07f, 15.5f, 8.5f, 13.93f, 8.5f, 12f)
        curveTo(8.5f, 10.07f, 10.07f, 8.5f, 12f, 8.5f)
        curveTo(13.93f, 8.5f, 15.5f, 10.07f, 15.5f, 12f)
        curveTo(15.5f, 13.93f, 13.93f, 15.5f, 12f, 15.5f)
        close()
      }
    }.build()
  }

  val ArrowBack: ImageVector by lazy {
    ImageVector.Builder(
      name = "ArrowBack",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.5f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
      ) {
        moveTo(19f, 12f)
        lineTo(5f, 12f)
        moveTo(12f, 19f)
        lineTo(5f, 12f)
        lineTo(12f, 5f)
      }
    }.build()
  }

  val Sun: ImageVector by lazy {
    ImageVector.Builder(
      name = "Sun",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round
      ) {
        moveTo(12f, 8f)
        curveTo(9.8f, 8f, 8f, 9.8f, 8f, 12f)
        curveTo(8f, 14.2f, 9.8f, 16f, 12f, 16f)
        curveTo(14.2f, 16f, 16f, 14.2f, 16f, 12f)
        curveTo(16f, 9.8f, 14.2f, 8f, 12f, 8f)
        close()
        moveTo(12f, 2f); lineTo(12f, 4f)
        moveTo(12f, 20f); lineTo(12f, 22f)
        moveTo(2f, 12f); lineTo(4f, 12f)
        moveTo(20f, 12f); lineTo(22f, 12f)
        moveTo(4.93f, 4.93f); lineTo(6.34f, 6.34f)
        moveTo(17.66f, 17.66f); lineTo(19.07f, 19.07f)
        moveTo(4.93f, 19.07f); lineTo(6.34f, 17.66f)
        moveTo(17.66f, 6.34f); lineTo(19.07f, 4.93f)
      }
    }.build()
  }

  val Moon: ImageVector by lazy {
    ImageVector.Builder(
      name = "Moon",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      path(
        fill = SolidColor(Color.White)
      ) {
        moveTo(12.3f, 2f)
        curveTo(6.9f, 2f, 2.5f, 6.4f, 2.5f, 11.8f)
        curveTo(2.5f, 17.2f, 6.9f, 21.6f, 12.3f, 21.6f)
        curveTo(16.5f, 21.6f, 20.1f, 19f, 21.5f, 15.2f)
        curveTo(20.7f, 15.4f, 19.8f, 15.5f, 18.9f, 15.5f)
        curveTo(13.9f, 15.5f, 9.9f, 11.5f, 9.9f, 6.5f)
        curveTo(9.9f, 4.9f, 10.3f, 3.4f, 11.1f, 2.1f)
        curveTo(11.5f, 2f, 11.9f, 2f, 12.3f, 2f)
        close()
      }
    }.build()
  }

  val Calendar: ImageVector by lazy {
    ImageVector.Builder(
      name = "Calendar",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineJoin = StrokeJoin.Round
      ) {
        moveTo(4f, 7f)
        lineTo(20f, 7f)
        lineTo(20f, 20f)
        lineTo(4f, 20f)
        close()
        moveTo(4f, 11f); lineTo(20f, 11f)
        moveTo(8f, 3f); lineTo(8f, 7f)
        moveTo(16f, 3f); lineTo(16f, 7f)
      }
    }.build()
  }

  val Apps: ImageVector by lazy {
    ImageVector.Builder(
      name = "Apps",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      path(fill = SolidColor(Color.White)) {
        moveTo(4f, 4f); lineTo(9.5f, 4f); lineTo(9.5f, 9.5f); lineTo(4f, 9.5f); close()
        moveTo(14.5f, 4f); lineTo(20f, 4f); lineTo(20f, 9.5f); lineTo(14.5f, 9.5f); close()
        moveTo(4f, 14.5f); lineTo(9.5f, 14.5f); lineTo(9.5f, 20f); lineTo(4f, 20f); close()
        moveTo(14.5f, 14.5f); lineTo(20f, 14.5f); lineTo(20f, 20f); lineTo(14.5f, 20f); close()
      }
    }.build()
  }

  val History: ImageVector by lazy {
    ImageVector.Builder(
      name = "History",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
      ) {
        moveTo(12f, 4f)
        curveTo(7.6f, 4f, 4f, 7.6f, 4f, 12f)
        curveTo(4f, 16.4f, 7.6f, 20f, 12f, 20f)
        curveTo(16.4f, 20f, 20f, 16.4f, 20f, 12f)
        curveTo(20f, 8.5f, 17.7f, 5.5f, 14.5f, 4.4f)
        moveTo(12f, 8f); lineTo(12f, 12f); lineTo(15f, 14f)
      }
    }.build()
  }

  val Home: ImageVector by lazy {
    ImageVector.Builder(
      name = "Home",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
      ) {
        moveTo(4f, 10f)
        lineTo(12f, 4f)
        lineTo(20f, 10f)
        lineTo(20f, 20f)
        lineTo(4f, 20f)
        close()
        moveTo(9f, 20f); lineTo(9f, 13f); lineTo(15f, 13f); lineTo(15f, 20f)
      }
    }.build()
  }

  val Export: ImageVector by lazy {
    ImageVector.Builder(
      name = "Export",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
      ) {
        moveTo(12f, 3f); lineTo(12f, 15f)
        moveTo(8f, 7f); lineTo(12f, 3f); lineTo(16f, 7f)
        moveTo(5f, 12f); lineTo(5f, 20f); lineTo(19f, 20f); lineTo(19f, 12f)
      }
    }.build()
  }

  val Warning: ImageVector by lazy {
    ImageVector.Builder(
      name = "Warning",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f
    ).apply {
      path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
      ) {
        moveTo(12f, 4f)
        lineTo(21f, 19f)
        lineTo(3f, 19f)
        close()
        moveTo(12f, 9f); lineTo(12f, 13f)
        moveTo(12f, 16f); lineTo(12f, 16.5f)
      }
    }.build()
  }
}
