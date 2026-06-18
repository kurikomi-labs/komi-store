# Komi migration cheatsheet (authoritative swap rules)

Goal: replace every deleted old-vocab symbol with its Komi successor so the file compiles.
The `theme.*` and `color.*` packages are DELETED. Material bridge is active: **keep every
`MaterialTheme.colorScheme.*` and `MaterialTheme.typography.*` read unchanged** — they still resolve.

Do NOT run gradle. Edit only. After editing, double-check imports are added/removed.

## Import paths (add as needed)
- `zed.rainxch.core.presentation.components.buttons.KomiButton`
- `zed.rainxch.core.presentation.components.buttons.KomiButtonVariant`
- `zed.rainxch.core.presentation.components.buttons.KomiButtonSize`
- `zed.rainxch.core.presentation.components.buttons.KomiIconButton`
- `zed.rainxch.core.presentation.components.inputs.KomiTextField`
- `zed.rainxch.core.presentation.components.inputs.KomiTextFieldSize`
- `zed.rainxch.core.presentation.components.overlays.KomiSheet`
- `zed.rainxch.core.presentation.components.overlays.KomiSheetPlacement`
- `zed.rainxch.core.presentation.components.surfaces.KomiSurface`
- `zed.rainxch.core.presentation.components.surfaces.KomiSurfaceElevation`
- `zed.rainxch.core.presentation.components.surfaces.KomiSurfacePaper`
- `zed.rainxch.core.presentation.components.text.KomiText`
- `zed.rainxch.core.presentation.components.text.KomiTextRole`
- `zed.rainxch.core.presentation.components.chips.KomiChip`
- `zed.rainxch.core.presentation.components.chips.KomiChipKind`
- `zed.rainxch.core.presentation.components.chips.KomiChipSize`
- `zed.rainxch.core.presentation.locals.LocalPersonality`
- `zed.rainxch.core.presentation.personality.utils.PersonalityPreview`
- `zed.rainxch.core.presentation.personality.classicPersonality`
- `androidx.compose.foundation.shape.RoundedCornerShape`

## Komi APIs (exact)
- `KomiButton(onClick, label: String, modifier, variant = KomiButtonVariant.Primary, size = KomiButtonSize.Md, enabled, loading, emphasized, fullWidth, leadingIcon: ImageVector?, trailingIcon: ImageVector?)`
- `KomiButtonVariant`: Primary, Tonal, Outline, Text, Destructive
- `KomiButtonSize`: Sm, Md, Lg
- `KomiIconButton(icon: ImageVector, contentDescription: String, onClick, modifier, variant = Tonal, size, enabled)`
- `KomiTextField(value, onValueChange, modifier, label: String?, helper: String?, error: String?, placeholder: String?, leadingIcon: ImageVector?, trailing: @Composable (()->Unit)?, clearable, multiline, rows, required, size, enabled, keyboardType, onCommit)`
- `KomiSheet(onDismiss, modifier, placement = KomiSheetPlacement.Auto, title: String?, titleJp: String?, maxWidth, dismissible, screentone, footer: @Composable (()->Unit)?, content: @Composable ColumnScope.()->Unit)`
- `KomiSheetPlacement`: Bottom, Center, Auto
- `KomiSurface(modifier, elevation = KomiSurfaceElevation.Card, paper, screentone, screentoneBoost, onClick: (()->Unit)?, hoverEnabled, tilt, topEdgeOnly, contentPadding: PaddingValues, content: @Composable ()->Unit)`
- `KomiText(text, modifier, role = KomiTextRole.Body, color, maxLines, overflow, textAlign, uppercase, fontSize, fontWeight, ...)`
- `KomiTextRole`: Display, Title, Stamp, Body, Label, Mono
- `KomiChip(label, modifier, kind = KomiChipKind.Info, size = KomiChipSize.Md, selected, index, tilt, leadingIcon: ImageVector?, leadingContent: @Composable (()->Unit)?, count: Int?, onClick: (()->Unit)?, onRemove: (()->Unit)?)`
- `KomiChipKind`: Info, Filter, Input ; `KomiChipSize`: Sm, Md
- Shape SSOT: `LocalPersonality.current.shape.corner` (Dp, normal) / `.cornerSmall` (Dp, small/chips)
- Personality colors: `LocalPersonality.current.colors.<field>` (primary, onPrimary, surface, onSurface, background, outline, error, ...). Prefer keeping `MaterialTheme.colorScheme.*` where already used.

## MECHANICAL swaps (do these)

### GithubStoreTheme { } wrapper  (mostly in @Preview funcs)
- Remove `import ...theme.GithubStoreTheme`.
- In a `@Preview` fun: `GithubStoreTheme { X }` -> `PersonalityPreview { X }`. If two previews exist (light/variant), make the second `PersonalityPreview(personality = classicPersonality()) { X }`.
- In NON-preview screen code: just delete the wrapper, keep inner content as-is (root already provides the theme).

### GhsButton -> KomiButton
- `GhsButton(onClick = f, variant = GhsButtonVariant.X, size = GhsButtonSize.Y) { Text("Foo") }`
  -> `KomiButton(onClick = f, label = "Foo", variant = KomiButtonVariant.X, size = KomiButtonSize.Y)`
- Variant names map 1:1 (Primary/Tonal/Outline/Text/Destructive). Size 1:1 (Sm/Md/Lg).
- The content lambda is almost always `{ Text(stringResource(...)) }` -> move that string into `label =`.
- If content had an icon + text -> `leadingIcon = <ImageVector>, label = "<text>"`.
- DROP `containerColorOverride` / `contentColorOverride`. If the override expressed a destructive/delete action -> `variant = KomiButtonVariant.Destructive`.
- Remove imports GhsButton/GhsButtonVariant/GhsButtonSize; add KomiButton/KomiButtonVariant/KomiButtonSize.

### GhsTextField -> KomiTextField
- Map: value, onValueChange, modifier, label, placeholder, leadingIcon -> same.
- `trailingIcon = { ... }` -> `trailing = { ... }`.
- `supportingText = "s"` -> `helper = "s"`. If `isError = true`, put the message into `error = "s"` instead of helper.
- `singleLine = false` -> `multiline = true` (and `minLines`/`maxLines` -> `rows = <n>`). `singleLine = true` (default) -> omit multiline.
- `keyboardType = X` -> keep.
- If `readOnly = true` is present: KomiTextField has no readOnly -> keep `enabled = true` and ADD it to your FLAGGED list (do not invent).
- Remove GhsTextField import; add KomiTextField (+ KomiTextFieldSize only if you set size).

### GhsBottomSheet -> KomiSheet(Bottom)
- `GhsBottomSheet(onDismissRequest = f) { content }` -> `KomiSheet(onDismiss = f, placement = KomiSheetPlacement.Bottom) { content }`.
- DROP sheetState / dragHandle args (KomiSheet owns them). Content body is the same; it now runs in `ColumnScope`.

### ExpressiveCard -> KomiSurface
- `ExpressiveCard(modifier = m, onClick = f) { content }` -> `KomiSurface(modifier = m, onClick = f) { content }`.
- Remove any `Modifier.clip(Radii.row)` you find on it (KomiSurface owns the shape).
- If `onLongClick = g` is present: KomiSurface has NO onLongClick -> keep `onClick`, drop `onLongClick`, and ADD to FLAGGED list.

### StatChip -> KomiChip
- `StatChip(label = l, leading = { Icon(...) }, background = .., border = .., contentColor = ..)`
  -> `KomiChip(label = l, kind = KomiChipKind.Info, size = KomiChipSize.Sm, leadingContent = { Icon(...) })`.
- DROP background/border/contentColor (KomiChip owns styling).

### OfficialBadge() -> inline Icon
- `OfficialBadge()` -> `Icon(imageVector = Icons.Filled.Verified, contentDescription = stringResource(Res.string.self_owned_badge), modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)`
- Add imports: `androidx.compose.material.icons.Icons`, `androidx.compose.material.icons.filled.Verified`, `androidx.compose.material3.Icon`, `androidx.compose.ui.unit.dp`, `androidx.compose.foundation.layout.size`. The `Res.string.self_owned_badge` import is `zed.rainxch.githubstore.core.presentation.res.self_owned_badge` (+ `...res.Res`).
- Remove `import ...components.OfficialBadge`.

### GhsSectionHeader(text) / SectionHeader(title) -> KomiText Title
- `GhsSectionHeader(text = t)` -> `KomiText(text = t, role = KomiTextRole.Title)`.
- `SectionHeader(title = t, ...)` -> `KomiText(text = t, role = KomiTextRole.Title)` (drop leading/subCount/onSeeAll; if onSeeAll was important add to FLAGGED).

### Shapes: WonkySquircleShape / CornerRadii / Radii -> personality shape
- `WonkySquircleShape(...)` or `WonkySquircleShape.Sheet` etc -> `RoundedCornerShape(LocalPersonality.current.shape.corner)`.
- `Radii.row` / big radius -> `RoundedCornerShape(LocalPersonality.current.shape.corner)`.
- `Radii.chip` / `Radii.small` / small radius -> `RoundedCornerShape(LocalPersonality.current.shape.cornerSmall)`.
- `CornerRadii.x` -> same rule.
- NOTE: `LocalPersonality.current` is a @Composable read — only valid inside a @Composable. If the old code declared a shape as a top-level `private val X = WonkySquircleShape(...)`, move it INSIDE the composable as a local `val X = RoundedCornerShape(LocalPersonality.current.shape.corner)`.
- Remove theme.shapes.* / theme.tokens.Radii imports; add RoundedCornerShape + LocalPersonality.

### GhsAccents.X -> primary
- `GhsAccents.Gold` (and Rose/Sky/Mint/Lavender/Sage/Aqua/etc) -> `MaterialTheme.colorScheme.primary`.

### GithubStoreButton -> KomiButton  (deleted top-level component)
- `GithubStoreButton(text = "Foo", onClick = f)` -> `KomiButton(label = "Foo", onClick = f)` (default Primary/Md unless the old call set a variant — there was none).
- Remove `import ...components.GithubStoreButton`; add KomiButton.

### bare `components.buttons.IconButton` -> Material3 IconButton  (deleted custom)
- The deleted custom `IconButton(onClick) { Icon(...) }` had the SAME shape as Material3's. Just change the import `zed.rainxch.core.presentation.components.buttons.IconButton` -> `androidx.compose.material3.IconButton`. Leave the call body as-is.

### GhsHomeTopBar(title, actions) -> KomiTopBar
- `GhsHomeTopBar(title = t, actions = { ... })` -> `KomiTopBar(title = t, actions = { ... })`.
- DROP `applyStatusBarPadding` (KomiTopBar handles insets). `actions` slot is `@Composable RowScope.() -> Unit` in both — keep as-is.
- Import: remove `components.chrome.GhsHomeTopBar`; add `zed.rainxch.core.presentation.components.bars.KomiTopBar`.

### GhsConfirmDialog -> KomiSheet(Center) + footer
- `GhsConfirmDialog(title=t, body=b, confirmLabel=c, onConfirm=oc, onDismiss=od, cancelLabel=cl, destructive=dz)` ->
```
KomiSheet(
    onDismiss = od,
    placement = KomiSheetPlacement.Center,
    title = t,
    footer = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KomiButton(onClick = od, label = cl, variant = KomiButtonVariant.Text)
            KomiButton(onClick = oc, label = c, variant = if (dz) KomiButtonVariant.Destructive else KomiButtonVariant.Primary)
        }
    },
) {
    KomiText(text = b, role = KomiTextRole.Body)
}
```
- If `cancelLabel` was not passed, the old default was "Cancel" -> use `stringResource(Res.string.cancel)` if such a key is obvious, else keep the literal that was there. If a `note`/`leading` arg was used, add to FLAGGED.

## DO NOT TOUCH — leave as-is and ADD to FLAGGED (the lead finishes these)

### Password fields: GhsPasswordVisibilityIcon -> KomiTextField(password = true)
Old pattern: a caller-managed `var visible by remember { mutableStateOf(false) }`, a `GhsTextField(..., visualTransformation = if (visible) None else PasswordVisualTransformation(), trailingIcon = { GhsPasswordVisibilityIcon(visible = visible, onToggle = { visible = !visible }) })`.
New: `KomiTextField(..., password = true)`. KomiTextField self-manages the masking + the eye toggle. So:
- DELETE the caller's `var visible`/`var passwordVisible` state and its imports if now unused.
- DROP the `trailingIcon`/`trailing` that held `GhsPasswordVisibilityIcon`.
- DROP any `visualTransformation = ...` argument (KomiTextField has none; it masks internally).
- Add `password = true`. Keep `keyboardType = KeyboardType.Password` if it was set.
- Remove `GhsPasswordVisibilityIcon` import + any now-unused `PasswordVisualTransformation`/`VisualTransformation` imports.

### Banner / BannerTint -> KomiSurface  (NOTE: drops the tint color — acceptable for green build)
`Banner(tint = X, leading = { L }, trailing = { T }) { content }` ->
```
KomiSurface(modifier = Modifier.fillMaxWidth()) {
    Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // leading() if present
        Box(modifier = Modifier.weight(1f)) { content() }
        // trailing() if present
    }
}
```
- Drop `tint`/`BannerTint` (no per-tint background in the new system yet). Remove `components.section.Banner` + `BannerTint` imports; add KomiSurface (+ layout imports if missing).

### Squiggle() -> delete
- `Squiggle()` was a decorative underline. DELETE the call entirely (no replacement). Remove the `vocabulary.Squiggle` import.

### GhsDropdownMenu + GhsDropdownMenuItem -> KomiDropdown  (declarative, owns its own expanded state)
Old shape:
```
var menuOpen by remember { mutableStateOf(false) }
Box {
    IconButton(onClick = { menuOpen = true }) { Icon(MoreVert, cd) }
    GhsDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
        GhsDropdownMenuItem(text = "A", onClick = { menuOpen = false; doA() })
        GhsDropdownMenuItem(text = "B", onClick = { menuOpen = false; doB() },
            trailingIcon = if (current == X) { { Icon(Check, ...) } } else null)
    }
}
```
New shape:
```
KomiDropdown(
    entries = persistentListOf(
        KomiMenuItem(id = "a", label = "A"),
        KomiMenuItem(id = "b", label = "B"),                 // tone = KomiMenuTone.Danger if the old item used error/destructive color
    ),
    onSelect = { item -> when (item.id) { "a" -> doA(); "b" -> doB() } },
    value = <selected-id-or-null>,                            // KomiDropdown checkmarks the entry whose id == value
    trigger = { onClick -> IconButton(onClick = onClick) { Icon(MoreVert, cd) } },
)
```
Rules:
- DELETE the caller's `var menuOpen by remember {...}` and the wrapping `Box {}` — KomiDropdown owns expanded state + its own Box. The trigger's `onClick` param replaces `menuOpen = true`. The trigger body is exactly the old trigger button (IconButton/clickable Box/etc).
- Build `entries` in the composable body. If labels come from a @Composable `displayName()`/`stringResource`, use `Type.entries.map { KomiMenuItem(id = it.name, label = it.displayName()) }.toImmutableList()` (map is inline, @Composable label calls are fine here). Otherwise `persistentListOf(...)`.
- Selection checkmark (old `trailingIcon = Check when current == item`): set each item `id = item.name` and pass `value = current.name`. Match is by **id**, not label.
- Per-item enabled -> `KomiMenuItem(enabled = ...)`. Per-item leading icon -> `icon = <ImageVector>`. Destructive/error item -> `tone = KomiMenuTone.Danger`.
- Imports: add `components.overlays.KomiDropdown`, `components.overlays.KomiMenuItem`, `components.overlays.KomiMenuTone` (only if used), `kotlinx.collections.immutable.persistentListOf` (and/or `toImmutableList`). Remove `GhsDropdownMenu`/`GhsDropdownMenuItem` imports.

## DO NOT TOUCH — leave as-is and ADD to FLAGGED list (the lead will finish these)
- `GhsDropdownMenu` + its items  (KomiDropdown has a different entries-based API)
- `RepositoryCard(...)`  (-> KomiRepoCard needs domain-model field mapping)
- `GhsEntryRow(...)`  (inline rebuild on KomiSurface)
- `PlatformsChip(...)`  (DiscoveryPlatform mapping)
- `GhsFullScreenSheet(...)`  (full-screen, not a sheet)
- `GhsConfirmDialog(...)`  (-> KomiSheet Center + footer; only if non-trivial)
- `GhsEntryRow(...)`  (inline rebuild on KomiSurface with combinedClickable — lead does these)
- Anything importing `theme.dynamicColorScheme` / `theme.isDynamicColorAvailable` / `theme.tokens.Tokens` / `theme.tokens.colorSchemeFor` (the dynamic-color path — lead deletes that branch)
- Anything importing the deleted `color.*` package (e.g. `color.avatarColorFor`, `color.AvatarColorStore`) or `theme.geist` / `theme.geistMono`
- Anything importing the deleted `vocabulary.*` EXCEPT `vocabulary.Squiggle` (Squiggle you DO delete per recipe). `vocabulary.PlatformKind` / `AppAccentResolver` / `FreshnessState` / `freshnessOf` -> FLAG (Cookie-era rebuild)
NOTE: Banner and password fields are now handled by the recipes above — DO them, do not flag.

## Output you must return
1. List of files you edited.
2. Per file: the swaps you made (symbol -> successor, count).
3. FLAGGED list: every call-site you intentionally left for the lead (with file:line + which pattern).
4. Any import you were unsure about.
Do not claim success — you cannot compile. Just report precisely what you changed.
