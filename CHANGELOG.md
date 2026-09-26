# Changelog

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
