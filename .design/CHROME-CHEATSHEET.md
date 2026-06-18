# Chrome pass — Material Scaffold/TopAppBar -> Komi

Goal: replace raw Material3 `Scaffold` + `TopAppBar` chrome with `KomiScaffold` + `KomiTopBar` so every screen gets personality chrome (manga grid-paper scaffold + inked top bar / classic M3). Keep all screen CONTENT + behavior identical. Do NOT run gradle. Edit only.

## APIs
- `KomiScaffold(modifier, topBar: (@Composable ()->Unit)?, bottomBar: (@Composable ()->Unit)?, floatingActionButton: (@Composable ()->Unit)?, overlay: (@Composable ()->Unit)?, grid = true, screentone = false, dividers = true, content: @Composable (PaddingValues)->Unit)`
  - NO `snackbarHost` slot, NO `containerColor` (owns the personality background). Put a SnackbarHost in `overlay`.
- `KomiTopBar(title: String, modifier, titleAccent: String? = null, subtitle: String? = null, leading: (@Composable ()->Unit)? = null, actions: (@Composable RowScope.()->Unit)? = null, size: KomiTopBarSize = Masthead, centerTitle = false)`
  - `title` is a **String**, not a composable. `KomiTopBarSize`: Compact, Masthead.
- `KomiIconButton(icon: ImageVector, contentDescription: String, onClick, modifier, variant = KomiButtonVariant.Tonal, size, enabled)` — for back + action buttons.

Imports:
- `zed.rainxch.core.presentation.components.scaffold.KomiScaffold`
- `zed.rainxch.core.presentation.components.bars.KomiTopBar`
- `zed.rainxch.core.presentation.components.bars.KomiTopBarSize`
- `zed.rainxch.core.presentation.components.buttons.KomiIconButton`
- `zed.rainxch.core.presentation.components.buttons.KomiButtonVariant`

## Scaffold -> KomiScaffold
```
Scaffold(
    topBar = { <bar> },
    snackbarHost = { SnackbarHost(snackbarState) },          // OPTIONAL
    floatingActionButton = { <fab> },                        // OPTIONAL
    containerColor = MaterialTheme.colorScheme.background,    // drop
) { padding -> <content(padding)> }
```
->
```
KomiScaffold(
    topBar = { <KomiTopBar...> },
    overlay = { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) { SnackbarHost(hostState = snackbarState, modifier = Modifier.padding(bottom = <same bottom padding it had, else 16.dp>)) } },  // ONLY if it had a snackbarHost
    floatingActionButton = { <fab> },                        // only if present
) { padding -> <content(padding)> }
```
- DROP `containerColor` (and any `contentColor`). KomiScaffold owns it.
- If there was NO snackbarHost, omit `overlay`.
- Keep `bottomBar = { ... }` as-is if present.
- Keep the `content` lambda + its `padding` usage identical.

## TopAppBar / CenterAlignedTopAppBar -> KomiTopBar
```
TopAppBar(
    title = { Text(text = "Foo", style=..., color=...) },
    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = cd) } },
    actions = { IconButton(onClick = a) { Icon(X, cd2) } ; ... },
    colors = ..., scrollBehavior = ...,
)
```
->
```
KomiTopBar(
    title = "Foo",
    size = KomiTopBarSize.Compact,     // sub-screens WITH a back button -> Compact. Top-level / home-style mastheads -> Masthead.
    leading = {
        KomiIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = cd,
            onClick = onBack,
            variant = KomiButtonVariant.Text,
        )
    },
    actions = {
        KomiIconButton(icon = X, contentDescription = cd2, onClick = a, variant = KomiButtonVariant.Text)
        // ...one per old action IconButton
    },
)
```
Rules:
- `title` must become a plain String — extract the literal/stringResource from the inner `Text`. Drop the Text's style/color (KomiTopBar styles it). If the title Text used an accent-colored substring, you may pass `titleAccent = "<that substring>"`; otherwise omit.
- `CenterAlignedTopAppBar` -> add `centerTitle = true`.
- `navigationIcon` (back arrow) -> `leading = { KomiIconButton(... variant = Text) }`.
- `actions` lambda: convert each Material `IconButton { Icon(...) }` to `KomiIconButton(icon=, contentDescription=, onClick=, variant = KomiButtonVariant.Text)`. If an action is NOT a plain icon button (e.g. a custom composable, a dropdown trigger), keep it as-is inside the `actions` lambda.
- DROP `colors`, `scrollBehavior`, `windowInsets`, `expandedHeight`. KomiTopBar owns styling + insets.

## DO NOT TOUCH — leave the Material widget + FLAG (lead handles)
- `LargeTopAppBar` / `MediumTopAppBar` or any TopAppBar wired to a **collapsing** `scrollBehavior` (TopAppBarDefaults.enterAlwaysScrollBehavior / exitUntilCollapsed) where the collapse is visually important — KomiTopBar does not collapse. Leave + FLAG.
- A top bar that bakes in a **search field** / segmented control / tabs (not a plain title+nav+actions) — leave + FLAG.
- `PullToRefreshBox` and other content wrappers — leave them; only swap the outer `Scaffold` shell + the `topBar`.
- Keep every `MaterialTheme.colorScheme.*` read in the CONTENT.

## Output
Per file: Scaffold swapped? topBar swapped (Compact/Masthead)? snackbar moved to overlay? any action you left as Material? FLAGGED items. No success claims (cannot compile).
