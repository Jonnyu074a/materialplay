package com.materialplay.launcher.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.core.graphics.drawable.toBitmap
import coil.request.ImageRequest
import com.materialplay.launcher.core.Text
import com.materialplay.launcher.data.db.GameEntity
import java.io.File
import kotlin.math.absoluteValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val fallbackGradients = listOf(
    Color(0xFF1E2A44) to Color(0xFF0C1018),
    Color(0xFF352442) to Color(0xFF12101B),
    Color(0xFF1C3A38) to Color(0xFF0B1514),
    Color(0xFF3B2A1C) to Color(0xFF17110B),
    Color(0xFF2C1F3B) to Color(0xFF100D16),
    Color(0xFF20303B) to Color(0xFF0B1116)
)

fun gradientFor(seed: String): Brush {
    val index = seed.hashCode().absoluteValue % fallbackGradients.size
    val (start, end) = fallbackGradients[index]
    return Brush.linearGradient(listOf(start, end))
}

@Composable
fun AppIcon(
    packageName: String,
    modifier: Modifier = Modifier.size(56.dp)
) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(initialValue = null, key1 = packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val drawable: Drawable = context.packageManager.getApplicationIcon(packageName)
                drawable.toBitmapSafely(192).asImageBitmap()
            }.getOrNull()
        }
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val bitmap = icon
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(gradientFor(packageName)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Text.initials(packageName.substringAfterLast('.')),
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

private fun Drawable.toBitmapSafely(size: Int): android.graphics.Bitmap {
    return if (this is android.graphics.drawable.BitmapDrawable && bitmap != null) {
        bitmap
    } else {
        this.toBitmap(size, size)
    }
}

/**
 * Game artwork with a real icon fallback. If neither cover nor hero exists the
 * composable renders the installed app icon over a deterministic gradient.
 */
@Composable
fun GameArt(
    game: GameEntity,
    modifier: Modifier = Modifier,
    hero: Boolean = false,
    corner: Dp = 20.dp,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val path = if (hero) game.heroPath ?: game.coverPath else game.coverPath ?: game.heroPath
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .clip(shape)
            .background(gradientFor(game.packageName))
    ) {
        AppIcon(
            packageName = game.packageName,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(if (hero) 0.22f else 0.42f)
                .aspectRatio(1f)
        )
        if (path != null && File(path).exists()) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(File(path)).crossfade(true).build(),
                contentDescription = game.label,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }
    }
}

@Composable
fun GameCover(
    game: GameEntity,
    modifier: Modifier = Modifier,
    corner: Dp = 18.dp
) {
    GameArt(game = game, modifier = modifier, hero = false, corner = corner)
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
    content: @Composable () -> Unit
) {
    val base = modifier
        .clip(RoundedCornerShape(16.dp))
        .background(containerColor)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
    Surface(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
        color = Color.Transparent
    ) {
        content()
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun Tag(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text("◎", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

@Composable
fun StatBlock(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ScreenshotImage(path: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context).data(File(path)).crossfade(true).build(),
        contentDescription = null,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentScale = ContentScale.Crop
    )
}

@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    )
}
