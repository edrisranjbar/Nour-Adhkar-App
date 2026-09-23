# Achievements UI style specification

This document extracts the visual system from the supplied achievements reference and translates it into implementation rules for the Nour Adhkar Android app. The source is a composite concept image rather than an inspectable design file, so colors are close visual matches and dimensions are normalized to a `360 dp` phone viewport.

## Design direction

The interface should feel warm, celebratory, calm, and premium. It combines a parchment-like canvas with deep emerald controls, muted gold rewards, softly illustrated Islamic scenes, and small game-like progression badges. The screen stays visually rich without becoming dense: illustration carries most of the personality, while typography and borders remain restrained.

Use these principles consistently:

- Warm ivory replaces pure white on the main canvas.
- Deep emerald is the primary action and completion color.
- Gold is reserved for rewards, glow, and special progress.
- Cards use thin warm borders and almost no gray shadow.
- Illustrations have soft rounded forms, gentle depth, and warm directional light.
- Progress information stays compact and secondary to the artwork.
- Locked content remains visible but appears desaturated, faded, and marked with a lock.
- Persian and Arabic layouts remain RTL, while numerals are localized for the selected language.

## Visual tokens

The following colors are approximate samples derived visually from the reference.

| Token | Light value | Usage |
|---|---:|---|
| `achievementCanvas` | `#FBF7ED` | Screen background and bottom navigation |
| `achievementSurface` | `#FFFDF7` | Cards, sheets, and content panels |
| `achievementSurfaceWarm` | `#F7F0DF` | Chips, inactive controls, summary inset |
| `achievementBorder` | `#E8DFC9` | Card and control outlines |
| `achievementText` | `#222A20` | Main titles and values |
| `achievementTextMuted` | `#746F63` | Descriptions, metadata, secondary labels |
| `achievementEmerald` | `#246B3D` | Selected chips, completed progress, actions |
| `achievementEmeraldDark` | `#0E4B38` | Detail hero and celebration background |
| `achievementEmeraldLight` | `#DDE9D8` | Success tint and selected backgrounds |
| `achievementGold` | `#D1A52B` | Reward borders, highlights, shield accents |
| `achievementGoldLight` | `#F2E3AF` | Locked/reward background tint |
| `achievementPurple` | `#51407C` | Alternate badge family and streak progress |
| `achievementLocked` | `#A9A394` | Locked icon and disabled artwork |
| `achievementProgressTrack` | `#E9E2D3` | Progress-bar track |
| `achievementSuccess` | `#38813F` | Completed state and level badge |

### Shape tokens

| Element | Radius |
|---|---:|
| Main summary card | `22 dp` |
| Achievement card | `16 dp` |
| Artwork | `13 dp` |
| Category chip | `18 dp` |
| Toolbar icon button | `12 dp` |
| Detail progress/activity card | `18 dp` |
| Primary button | `14 dp` |
| Small level shield | Custom shield or `10 dp` rounded badge |

### Elevation and borders

- Use a `1 dp` warm border on cards and icon buttons.
- Main cards use `0–2 dp` tonal elevation.
- Use a soft shadow only for floating level badges and the overlapping detail emblem: black at `8–12%`, blur visually equivalent to `10–16 dp`.
- Avoid the strong gray Material default shadow.

## Typography

Use the app's Persian/Arabic UI font. `Vazirmatn` is the closest match for interface text. Avoid a decorative Quran typeface on this screen.

| Role | Size | Weight | Line height | Alignment |
|---|---:|---:|---:|---|
| Screen title | `22 sp` | `700` | `30 sp` | Center |
| Summary number | `24 sp` | `800` | `28 sp` | Center |
| Summary label | `11 sp` | `500` | `16 sp` | Center |
| Card title | `14 sp` | `700` | `20 sp` | Center |
| Card description | `10–11 sp` | `400` | `15 sp` | Center |
| Progress value | `11 sp` | `600` | `16 sp` | Center |
| Chip text | `12 sp` | `500` | `18 sp` | Center |
| Detail title | `24 sp` | `800` | `34 sp` | Center |
| Detail instruction | `14 sp` | `500` | `22 sp` | Center |
| Detail body | `12 sp` | `400` | `20 sp` | Center |
| Button text | `15 sp` | `650` | `22 sp` | Center |

Keep card descriptions to two lines. Use ellipsis only if localized text cannot fit after the minimum supported font scale is applied.

## Gallery screen

### Overall structure

At a `360 dp` width, build the screen in this order:

1. System status-bar inset.
2. Centered app bar.
3. Summary card.
4. Category chips.
5. Three-column achievement grid.
6. Persistent app bottom navigation.

Use `16 dp` horizontal screen padding and `12 dp` vertical section spacing. The grid scrolls beneath the app bar and above the bottom navigation.

### App bar

- Height: `56 dp`, excluding status-bar inset.
- Background: `achievementCanvas`.
- Title centered independently of actions.
- Back button remains visually on the left, matching the reference.
- Optional information button sits on the right.
- Icon containers are `40 × 40 dp`, ivory, with a warm border and `12 dp` radius.
- Icons are `22 dp` and use `achievementText`.
- Do not add a dividing line unless scroll content reaches the app bar.

### Summary card

- Outer margin: `16 dp` horizontal.
- Height: approximately `118 dp`.
- Padding: `14 dp` horizontal and `16 dp` vertical.
- Divide the width into three equal visual zones without vertical separators.
- Background: `achievementSurface` with a faint parchment texture or subtle radial pattern below `4%` opacity.

The three zones are:

1. **Completed:** a green eight-point reward medallion with the completed count in its center and «کامل شده» below.
2. **Total progress:** a `72 dp` circular progress ring with unlocked-level count in the center and «مجموع پیشرفت» below.
3. **Locked:** a pale-gold medallion with a lock icon and «قفل شده» below.

Ring stroke is `6 dp`. Completed arc uses emerald; track uses warm beige. All summary icons and values align vertically.

### Category chips

- Single horizontal row, RTL ordering.
- Height: `34 dp`.
- Gap: `8 dp`.
- Horizontal padding per chip: `15 dp`.
- Selected chip: emerald fill, white text, optional small sparkle icon.
- Unselected chip: `achievementSurfaceWarm`, dark muted text, no border.
- Labels for the current three-award release: «همه»، «استمرار»، «اعمال روزانه»، «اذکار».
- Selection filters the grid without changing scroll position abruptly.

### Achievement grid

- Exactly three columns on normal phones, matching the reference.
- Grid gap: `8 dp` horizontal and `12 dp` vertical.
- Card width at `360 dp`: approximately `104 dp`.
- Card height: content-driven, usually `214–230 dp`.
- Card padding: `5 dp` around artwork, `7 dp` around text.
- Card background: `achievementSurface`.
- Border: `1 dp achievementBorder`.
- Radius: `16 dp`.

#### Artwork

- Square, full card width minus `5 dp` padding.
- Radius: `13 dp`.
- Use `ContentScale.Crop`.
- Keep important subjects inside the central `70%` safe area.
- Illustration style: premium 2.5D storybook rendering, softly modeled objects, warm gold light, emerald/ivory environment, no embedded text.

#### Level shield

- Anchor to the lower-left edge of the artwork, overlapping by `7–9 dp`.
- Size: approximately `30 × 34 dp`.
- Use an emerald shield for active/completed levels, purple for streak-family rewards, and muted gold for habit-family rewards.
- Center the current threshold or level in bold white localized digits.
- Add a `1.5 dp` ivory outline and a small dark shadow.
- Locked cards replace the number with a `12 dp` lock in the upper artwork corner.

#### Text and progress

- Title begins `8 dp` below artwork and uses one centered line.
- Description sits `2 dp` below title and is limited to two centered lines.
- Progress value appears below the description, formatted as `current / target`.
- Progress bar is `5 dp` high, inset `7 dp` horizontally, with fully rounded ends.
- Track uses warm beige. Fill uses the achievement family's color.
- Do not show separate large buttons inside gallery cards.

#### Locked state

- Apply grayscale to artwork.
- Overlay ivory at approximately `42%` opacity.
- Reduce text opacity to `55–65%`.
- Keep the requirement readable.
- Show a small dark neutral lock badge in the upper corner.
- Preserve the same dimensions as unlocked cards to prevent layout shifts.

### Current three award mappings

| Award | Artwork concept | Metric | Levels | Accent |
|---|---|---|---|---|
| همراه پیوسته | Sunrise, mosque, glowing path | Unique active days | `3`, `7`, `30` | Emerald |
| یار اعمال روزانه | Open checklist journal, lantern, prayer beads | Completed checklist tasks | `10`, `30`, `100` | Muted gold |
| ذاکر پرتلاش | Prayer beads, lantern, night mosque | Saved tasbih count | `100`, `500`, `1000` | Purple/emerald |

## Achievement detail screen

The reference uses a full-screen detail page. Do not present this content as a small dialog. A modal bottom sheet is acceptable only as an interim implementation.

### Hero

- Height: `285–310 dp`, including status bar.
- Full-bleed illustration using the same award artwork or a portrait/detail variant.
- Use a deep emerald night treatment for the background.
- Add a very subtle bottom gradient into `achievementEmeraldDark` to keep the overlap clean.
- Back button is top-left; share button is top-right.
- Both are `40 dp` translucent dark glass buttons with white icons.

### Overlapping achievement emblem

- Center horizontally across the boundary between hero and content.
- Overall size: `112–126 dp`.
- Use an ivory multi-point seal with a gold outline.
- Place the award's hero object in the center.
- Attach an emerald level shield to the seal's bottom edge.
- Shadow: soft and broad, approximately `14 dp` visual blur.

### Content panel

- Begins below the hero with rounded top corners of `24 dp`.
- Background: `achievementSurface`.
- Top content padding must account for half the overlapping emblem.
- Center title, instruction, and description.
- Use at least `20 dp` horizontal padding.

### Progress card

- Margin top: `18 dp`.
- Radius: `18 dp`.
- Padding: `16 dp`.
- Right side label: «پیشرفت فعلی».
- Left side value: `current / next target`, with current value in emerald.
- Progress bar: `9 dp` high with rounded ends.
- Milestone row sits `16 dp` below the bar.

Milestones are connected by a thin beige line. Each milestone uses a small shield:

- Achieved: emerald fill, gold/ivory edge, white target value.
- Next: warm gray or pale gold.
- Future: neutral gray with a lock hanging below.
- Keep all milestone labels on one baseline.

### Recent activity card

- Radius: `18 dp`.
- Heading: «آخرین فعالیت».
- Show the most recent contribution in one concise line plus time/date metadata.
- Optional success chip uses a very pale green fill.
- Hide this section when there is no activity instead of showing dummy data.

### Celebration state

When a new level unlocks, show a dedicated full-screen celebration after progress is saved:

- Deep emerald background.
- Gold confetti and light rays.
- Large decorative arch/seal containing the related illustration.
- Newly unlocked level shield attached below the seal.
- Heading: «تبریک!».
- Award title and unlocked requirement below.
- Emerald primary action for sharing only when the user explicitly taps it.
- Ivory secondary action «ادامه».
- Do not automatically open Android's share chooser.

## Motion

Keep motion short and ceremonial:

- Gallery card press: scale to `0.98` over `100 ms`, then restore.
- Filter change: `180 ms` fade plus slight vertical movement.
- Detail emblem: spring in from `0.88` scale with low overshoot.
- Progress bar: animate from previous value to current value over `500–700 ms`.
- Newly unlocked shield: `220 ms` scale/fade, followed by one soft glow pulse.
- Respect the system's reduced-motion setting.

## RTL and localization

- The gallery and chips flow right to left.
- Card artwork is not mirrored.
- Back and share actions keep the visual positions shown in the reference: back on the left and share/info on the right.
- Persian uses Persian digits; Arabic uses Arabic-Indic digits.
- Avoid mixing Latin `/` spacing with RTL text. Render progress as isolated numeric spans if bidirectional ordering becomes unstable.
- All text must use the existing language catalog rather than branching layout by language.
- Verify Arabic titles at `1.3×` font scale.

## Accessibility

- Minimum tap target: `48 × 48 dp`, even when the visible chip or icon is smaller.
- Achievement cards expose one semantic click target with title, current progress, and lock state.
- Do not communicate unlocked state only through color; include the level shield, check, or lock.
- Body text contrast should meet `4.5:1`; large title contrast should meet `3:1`.
- Artwork descriptions should identify the award, not describe decorative scenery.
- Progress indicators expose current and target values to TalkBack.

## Compose implementation map

| Visual element | Recommended Compose primitive |
|---|---|
| Gallery scaffold | `Scaffold` with a centered custom top bar |
| Summary | `OutlinedCard` plus custom medallion shapes and `CircularProgressIndicator` |
| Filters | `LazyRow` with custom `Surface` chips |
| Grid | `LazyVerticalGrid(GridCells.Fixed(3))` |
| Artwork | `Image`, clipped before `ContentScale.Crop` |
| Locked artwork | `ColorMatrix.setToSaturation(0f)` plus translucent overlay |
| Level shield | Custom `Shape` or vector drawable; avoid a plain circular badge |
| Card progress | `LinearProgressIndicator` with rounded modifier |
| Detail | Dedicated navigation destination with full-bleed hero |
| Milestone path | `Canvas` line behind shield components |
| Celebration | Full-screen composable overlay after a persisted unlock event |

## Implementation acceptance checklist

- [ ] Warm ivory canvas replaces the default Material background on both achievement screens.
- [ ] Summary uses three reward zones and a real circular progress ring.
- [ ] Category filters are compact pills with emerald selected state.
- [ ] Normal phone layout shows three cards per row.
- [ ] Every card contains square artwork, an overlapping shield, two lines of copy, numeric progress, and a thin progress bar.
- [ ] Locked cards remain visible with grayscale artwork, reduced contrast, and an explicit lock.
- [ ] Tapping a card opens a full-screen detail destination.
- [ ] Detail hero is full bleed with floating back/share actions.
- [ ] The achievement seal overlaps the hero/content boundary.
- [ ] Progress milestones use connected shield states.
- [ ] New unlocks can trigger the celebration screen after data persistence.
- [ ] Persian and Arabic text, digits, TalkBack labels, and large font scale are verified.
- [ ] Light and dark app themes keep readable contrast; the award artwork itself is not recolored in dark mode.
