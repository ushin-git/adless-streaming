# Adless Streaming design system

Version 0.1 · Author: Tanishq

## Visual identity

- **Name:** Adless Streaming
- **Mark:** A custom “A” formed from two rising sound bars around a small negative-space play triangle. Drawn in the app as original vector geometry; no Echo Music assets or marks.
- **Palette:** Ink and warm paper foundations with a bright mint accent and coral secondary accent. Album art may tint the player surface only; text and controls keep accessible contrast.
- **Typography:** Android system sans (Roboto) for reliable system scaling; expressive display face reserved for large section titles. No downloaded fonts in the first milestone.

## Color tokens

| Token | Light | Dark | AMOLED |
|---|---|---|---|
| `background` | `#F7F6F2` | `#111412` | `#000000` |
| `surface` | `#FFFFFF` | `#191E1B` | `#080A09` |
| `surfaceElevated` | `#EFEEE8` | `#222824` | `#101411` |
| `primary` | `#176B54` | `#8CE3BD` | `#8CE3BD` |
| `onPrimary` | `#FFFFFF` | `#08271C` | `#00140C` |
| `secondary` | `#B34D3D` | `#FF9B86` | `#FF9B86` |
| `textPrimary` | `#171B18` | `#F2F4F0` | `#FFFFFF` |
| `textSecondary` | `#555E58` | `#B7C0B9` | `#C7CEC9` |
| `outline` | `#737D76` | `#89948D` | `#78847C` |
| `error` | `#B3261E` | `#FFB4AB` | `#FFB4AB` |

## Layout and type

- Base spacing unit: 4 dp. Tokens: `xs=4`, `sm=8`, `md=16`, `lg=24`, `xl=32`, `xxl=48`.
- Touch target minimum: 48 dp. Standard content gutter: 20 dp phone, 32 dp tablet.
- Type scale: `display=32/38`, `headline=24/30`, `title=20/26`, `body=16/24`, `label=12/16` (sp / line-height sp). Support Android font scaling without clipping.
- Corners: 12 dp controls, 20 dp cards, 28 dp sheets and player panels. Use restrained elevation; separate surfaces by color before shadow.

## Components

- **Primary button:** filled mint/green, 52 dp height, 16 dp horizontal padding, visible pressed state.
- **Icon button:** 48 dp touch target with a 24 dp icon and descriptive screen-reader label.
- **Track row:** 56–64 dp minimum, artwork 48 dp, title + artist, overflow action; selected/playing state indicated by icon and text, not color alone.
- **Mini-player:** 68 dp panel above bottom navigation, thumbnail, two-line metadata, play/pause, progress indicator; tap opens player.
- **Search field:** 52 dp rounded field, clear action, semantic hint and keyboard search action.
- **Empty/error states:** concise explanation, source context, and one retry or browse action; never use color alone to communicate status.

## Motion and adaptive layout

- Motion is brief (150–280 ms), interruptible, and respects reduced-motion/system animation settings.
- Use shared artwork and metadata transitions only when navigation remains understandable with motion reduced.
- Phones use bottom navigation and list-first browsing. Tablets use a navigation rail and two-pane layouts. Desktop-sized foldables increase list/card density without shrinking controls.
- Dynamic album-art colors are optional decoration, validated against fixed contrast-safe text tokens. Full light, dark, and AMOLED black theme modes are required.

