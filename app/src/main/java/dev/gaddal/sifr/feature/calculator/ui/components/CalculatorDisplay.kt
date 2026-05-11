package dev.gaddal.sifr.feature.calculator.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import dev.gaddal.sifr.core.ui.util.UiText

private const val PROMOTION_ANIMATION_MS = 340

// Fixed height for the preview slot so the Column's total height — and
// therefore the outer Box's vertical centering — never shifts when the
// preview content appears or disappears. 32dp comfortably holds 22sp text
// plus default line metrics on every density we ship to.
private val PREVIEW_SLOT_HEIGHT = 32.dp

// A no-op TextToolbar that suppresses the floating Cut/Copy/Paste menu when
// `showSelectionToolbar` is off in settings. status is always Hidden and the
// show/hide methods don't do anything, so Compose never draws the toolbar.
private object EmptyTextToolbar : TextToolbar {
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden
    override fun hide() {}
    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
    }
}

// Wraps the system text toolbar so each selection state offers exactly one
// useful action:
//   * Range selected → Copy only (Cut and Paste are no-ops because the
//     writer's defensive filter blocks the field's text mutations).
//   * Collapsed selection → Select All only (Paste is broken for the same
//     reason; Select All flows naturally into the range case where Copy
//     then becomes available).
// Compose's `onCopyRequested != null` is its "selection is non-empty"
// signal and is the reliable way to discriminate the two cases.
private class CalculatorTextToolbar(
    private val delegate: TextToolbar,
) : TextToolbar {

    override val status: TextToolbarStatus get() = delegate.status

    override fun hide() {
        delegate.hide()
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        val hasRange = onCopyRequested != null
        if (hasRange) {
            delegate.showMenu(
                rect = rect,
                onCopyRequested = onCopyRequested,
                onPasteRequested = null,
                onCutRequested = null,
                onSelectAllRequested = null,
            )
        } else {
            delegate.showMenu(
                rect = rect,
                onCopyRequested = null,
                onPasteRequested = null,
                onCutRequested = null,
                onSelectAllRequested = onSelectAllRequested,
            )
        }
    }
}

@Composable
fun CalculatorDisplay(
    expression: String,
    cursor: Int,
    selectionStart: Int,
    livePreview: String?,
    error: UiText?,
    showSelectionToolbar: Boolean,
    rangeSelectionEnabled: Boolean,
    onCursorChange: (Int) -> Unit,
    onSelectionChange: (start: Int, end: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val mainColor = if (error != null) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    val previewColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
    val mainStyle = TextStyle(color = mainColor, textAlign = TextAlign.End)
    val previewFontSizeSp = 22f

    // Detect a calculate-promotion transition: the expression that just landed
    // equals the live preview the user was seeing on the previous frame. We
    // bump promotionKey on detection and drive the animation off that key in a
    // separate effect, so any state change during the animation cancels the
    // animator cleanly without losing detection state.
    val previousPreviewState = remember { mutableStateOf<String?>(null) }
    var promotionKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(expression, livePreview, error) {
        val oldPreview = previousPreviewState.value
        previousPreviewState.value = livePreview
        if (error == null &&
            livePreview == null &&
            expression.isNotBlank() &&
            expression == oldPreview
        ) {
            promotionKey++
        }
    }

    val promotionProgress = remember { Animatable(initialValue = 1f) }
    LaunchedEffect(promotionKey) {
        if (promotionKey > 0) {
            promotionProgress.snapTo(0f)
            promotionProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(PROMOTION_ANIMATION_MS, easing = FastOutSlowInEasing),
            )
        }
    }

    // Composition-level read of progress is gated to a coarse boolean so we
    // recompose only when the animation begins or ends. Continuous reads of
    // `promotionProgress.value` happen inside `graphicsLayer` blocks below,
    // which observe state via the deferred-read API — those re-run per frame
    // without invalidating the surrounding composition.
    val isAnimating by remember {
        derivedStateOf { promotionProgress.value < 1f }
    }

    // Geometry captured while the layout is idle, used to drive the morph.
    // We only update these while !isAnimating so the targets stay fixed
    // throughout the transition.
    //   * mainTextCenterYPx — the *text's* visual center, not the slot's: the
    //     auto-sized field's slot wraps the text tightly so the two coincide.
    //   * previewSlotCenterYPx — the preview row's slot center; the BasicText
    //     inside is vertically centered in this slot via the Box wrapper, so
    //     scaling around its own pivotFractionY=0.5 lands the text right on
    //     the slot center.
    var mainTextCenterYPx by remember { mutableFloatStateOf(0f) }
    var previewSlotCenterYPx by remember { mutableFloatStateOf(0f) }
    var mainFontSizeSp by remember { mutableFloatStateOf(80f) }

    // Hold the most recent non-blank preview so the in-flight text still has
    // content to render once livePreview flips to null on Calculate.
    var lastShownPreview by remember { mutableStateOf("") }
    LaunchedEffect(livePreview) {
        val current = livePreview
        if (current != null && current.isNotBlank()) lastShownPreview = current
    }

    Box(
        modifier = modifier,
        contentAlignment = if (isRtl) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        // Force math content to render LTR regardless of system locale.
        // The outer Box already picked the locale-correct anchor above; this
        // inner scope stops the Unicode bidi algorithm from flipping trailing
        // weak operators (e.g. `+` in `10+`) to the visual leading edge.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End,
            ) {
                if (error != null) {
                    BasicText(
                        text = error.asString(),
                        style = mainStyle.copy(fontSize = 56.sp),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.StartEllipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    val fieldValue = remember(expression, cursor, selectionStart) {
                        val maxLen = expression.length
                        TextFieldValue(
                            text = expression,
                            selection = TextRange(
                                start = selectionStart.coerceIn(0, maxLen),
                                end = cursor.coerceIn(0, maxLen),
                            ),
                        )
                    }
                    // Customize the long-press text toolbar:
                    //   - showSelectionToolbar = false: route to an EmptyTextToolbar
                    //     so no menu appears at all.
                    //   - showSelectionToolbar = true + range selected: show only
                    //     Copy (Cut/Paste are no-ops in the calculator, hiding them
                    //     prevents user confusion).
                    //   - showSelectionToolbar = true + collapsed selection: pass
                    //     through to the system's default behavior (typically
                    //     Paste + Select All).
                    // Scoped to the field — history and settings text still use
                    // the system toolbar.
                    val systemToolbar = LocalTextToolbar.current
                    val fieldToolbar: TextToolbar =
                        remember(showSelectionToolbar, systemToolbar) {
                            if (showSelectionToolbar) {
                                CalculatorTextToolbar(systemToolbar)
                            } else {
                                EmptyTextToolbar
                            }
                        }
                    CompositionLocalProvider(LocalTextToolbar provides fieldToolbar) {
                        AutoSizingExpressionField(
                            value = fieldValue,
                            onValueChange = { newValue ->
                                // IME is blocked at the platform-input layer inside
                                // AutoSizingExpressionField and the field also drops
                                // text mutations defensively; we react to selection
                                // changes driven by tap or long-press + drag.
                                val newStart = newValue.selection.start
                                val newEnd = newValue.selection.end
                                if (rangeSelectionEnabled) {
                                    if (newStart != selectionStart || newEnd != cursor) {
                                        onSelectionChange(newStart, newEnd)
                                    }
                                } else {
                                    // Range support off: collapse any range the user
                                    // forms back to a single caret at the field's
                                    // selection.start (anchor). The next state push
                                    // re-collapses the visual selection.
                                    if (newStart != cursor) {
                                        onCursorChange(newStart)
                                    }
                                }
                            },
                            style = mainStyle,
                            onFontSizePicked = { mainFontSizeSp = it.value },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coords ->
                                    if (!isAnimating) {
                                        mainTextCenterYPx =
                                            coords.positionInParent().y + coords.size.height / 2f
                                    }
                                }
                                .graphicsLayer {
                                // Main fades in only in the second half of the
                                // promotion, after the preview has finished its
                                // trip up. Until then, it's invisible so the
                                // preview text is the sole visual actor.
                                    alpha = if (isAnimating) {
                                        val p = promotionProgress.value
                                        ((p - 0.55f) / 0.45f).coerceIn(0f, 1f)
                                    } else {
                                        1f
                                    }
                                },
                        )
                    }
                }

                // Fixed-height preview slot — always rendered so the Column
                // height stays constant. The text content is centered inside
                // (contentAlignment = Center) so the graphicsLayer pivot at
                // (1f, 0.5f) on the text lines up with the slot's center,
                // making translationY land the morph exactly on the main row.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PREVIEW_SLOT_HEIGHT)
                        .onGloballyPositioned { coords ->
                            if (!isAnimating) {
                                previewSlotCenterYPx =
                                    coords.positionInParent().y + coords.size.height / 2f
                            }
                        },
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    val showPreviewContent =
                        error == null && (!livePreview.isNullOrBlank() || isAnimating)
                    if (showPreviewContent) {
                        BasicText(
                            text = if (isAnimating) lastShownPreview else (livePreview ?: ""),
                            style = TextStyle(
                                fontSize = previewFontSizeSp.sp,
                                color = previewColor,
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.StartEllipsis,
                            modifier = Modifier.graphicsLayer {
                                if (isAnimating) {
                                    val p = promotionProgress.value
                                    val deltaY = mainTextCenterYPx - previewSlotCenterYPx
                                    val targetScale = mainFontSizeSp / previewFontSizeSp
                                    val scaleAmount = 1f + (targetScale - 1f) * p
                                    // Pivot at the text's right edge / vertical
                                    // center. Because the BasicText is wrap-
                                    // content here (no fillMaxWidth), the layer
                                    // bounds equal the text bounds, so the
                                    // pivot lands on the text's actual center.
                                    transformOrigin = TransformOrigin(
                                        pivotFractionX = 1f,
                                        pivotFractionY = 0.5f,
                                    )
                                    scaleX = scaleAmount
                                    scaleY = scaleAmount
                                    translationY = deltaY * p
                                    // Stay opaque through the first half of
                                    // the trip so the eye tracks the moving
                                    // text; then crossfade out as main fades
                                    // in (main starts fading in at p=0.55).
                                    alpha =
                                        (1f - ((p - 0.5f) / 0.5f).coerceAtLeast(0f))
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
