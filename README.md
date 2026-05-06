# Auto Trade Filtering

[![build](https://github.com/Aye10032/auto-trade-filtering/actions/workflows/build.yml/badge.svg)](https://github.com/Aye10032/auto-trade-filtering/actions/workflows/build.yml)
![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-62B47A?style=flat-square)
![Fabric Loader](https://img.shields.io/badge/Fabric%20Loader-0.19.2%2B-DBD0B4?style=flat-square)
![Java](https://img.shields.io/badge/Java-25%2B-E76F00?style=flat-square)
![License](https://img.shields.io/badge/License-CC0--1.0-lightgrey?style=flat-square)

A Fabric Minecraft mod that automatically rerolls villager trades until a player-selected target trade appears.

## Features

- Adds an `F` button to the villager trading screen to open the trade filter.
- Supports selecting multiple target trades and setting a max attempt count, up to 10,000 attempts.
- Repeatedly rerolls trades for unlocked villagers until the current-level offers contain the selected target.
- Adds a librarian enchanted book overlay that can be toggled with a client command.
- Includes English and Simplified Chinese translations.

## Supported Targets

| Villager Profession | Filterable Targets |
| --- | --- |
| Librarian | Enchanted books |
| Armorer | Diamond armor and compatible enchantments |
| Toolsmith | Diamond tools and compatible enchantments |
| Weaponsmith | Diamond weapons and compatible enchantments |
| Fletcher | Bows, crossbows and compatible enchantments; tipped arrows |
| Mason | Terracotta and glazed terracotta color variants |
| Shepherd | Wool color variants |

## Usage

1. Find a villager that already has a profession but has not been traded with.
2. Open the villager trading screen and click the `F` button.
3. Select the target trade in the filter screen, and adjust the max attempt count if needed.
4. Click `Start`.
5. A toast and sound will play on success. If rerolling fails, the toast will show the reason, such as being too far away, a locked profession, or reaching the max attempt count.

Notes:

- The villager must not be trade-locked. Once a player completes a trade with that villager, this mod will not reroll its profession or existing offers.
- The player must be within 8 blocks of the target villager.
- On multiplayer servers, both the client and server need this mod installed.
- Higher attempt counts require more server-side work. Start with a lower count and raise it only for rare target combinations.

## Librarian Trade Overlay

The mod can show enchanted book information from a librarian's visible offers. Use these client commands to toggle the overlay:

```mcfunction
/atf overlay
/atf overlay true
/atf overlay false
```

## License

This project is licensed under `CC0-1.0`. See [LICENSE](LICENSE).
