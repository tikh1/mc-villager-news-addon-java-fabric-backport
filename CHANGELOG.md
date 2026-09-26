# Changelog

## 1.3.6.1+1.21.1

A bug fix update. Replace the old jar in your `mods` folder with this one.

### Fixed

- Dialogue settings could stay locked in singleplayer, and operators could not
  always change them on servers
- A whole village, such as a trading hall, could start talking at once. Only
  one villager starts a background line at a time now
- The text on the signs villagers carry was always English. It now follows the
  subtitle language in all six languages

### Added

- Language files can translate sign text in a new `"signs"` section

## 1.3.6+1.21.1

The first Minecraft 1.21.1 backport of
[Villager News Addon Port](https://github.com/MarcYohannTheScripter/villager-news-bedrock-addon-java-port)
1.3.6.

### Added

- Minecraft 1.21.1 support with Fabric Loader 0.19.5
- Subtitle language setting with an **Auto** option that follows the game
  language, then another region of the same language, then English
- Subtitle translations for Brazilian Portuguese, Russian, Simplified Chinese,
  Spanish, and Turkish
- Speaker names in front of subtitles follow the subtitle language
- Translated handbook, settings screen, and item names for Brazilian
  Portuguese, Russian, Simplified Chinese, Spanish, and Turkish
- Subtitle folder at `config/villager-news-addon-port/subtitles` with every
  shipped language in it. Add, edit, or delete language files there, no
  resource pack or restart needed
- In-game guide for translating subtitles with a button that opens the folder
- Automated in-game server tests (`runGametest`)
- GitHub Actions build that uploads the jar and publishes a release for
  version tags

### Fixed

- Wooly the Sheep turning invisible
- Baby villagers using the wrong model and size
- `jeb_` rainbow nose missing on babies and special characters
- Special characters never spawning in newly generated villages
- Morning, afternoon, evening, and night greetings playing at the wrong time
- Wrong wood texture on signs held by villagers
- Villager News Handbook held in the wrong position

### Notes

- Voice acting is not translated. All characters still speak English.
