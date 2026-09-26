# AI translation prompt for Villager News subtitles

This file is an optional helper. Copy everything below the line into an AI
coding assistant that can read and write files in this project (for example
Claude Code, Cursor, GitHub Copilot, or Windsurf). Replace `<LANGUAGE>` and
`<CODE>` first, for example `German` and `de_de`.

AI output can contain mistakes. Please have a native speaker read the result
before you share it.

---

You are translating the in-game subtitles of the **Villager News Addon Java
Backport**, a Minecraft Java Edition mod, into **<LANGUAGE>**. The new file
will be `<CODE>.json`, where `<CODE>` is the Minecraft language code for
<LANGUAGE>.

Work carefully and take your time. Quality matters more than speed. The goal
is a translation that sounds like the characters are really speaking
<LANGUAGE>, with the same jokes, moods, and timing as the English original.

## 1. Study the project first

Before you translate anything, read and understand these files. Do not skip
this step.

- `README.md` in the project root: what the mod is, the characters, and how
  languages work.
- `src/main/resources/assets/villager-news-addon-port/subtitles/en_us.json`:
  the English source. This is what you translate.
- `src/main/resources/assets/villager-news-addon-port/subtitles/example_language.json`:
  a short example of the file format.
- The finished translations in the same folder (`ru_ru.json`, `tr_tr.json`,
  `zh_cn.json`): study how they keep tone, shouting, stretched words, and
  stage directions. Use them as a quality reference, not as a source.
- `src/main/resources/assets/villager-news-addon-port/dialogues.json`: the
  context for every line. For each dialogue id it has
  - `title` and `body`: what the player did to trigger the line and what kind
    of reaction it is
  - `speaker`: who talks (`villager`, `mayor`, `number_5`, `number_9`,
    `testificate_man`, `wooly`, `wandering_trader`, `unreachable`)
  - `variants[].subtitles[].time`: when each part appears, in seconds
  - `variants[].duration`: how long the whole voice line lasts
- `src/main/resources/assets/villager-news-addon-port/handbook.json`: the same
  triggers explained for players, grouped into categories.
- The game's own language file for <LANGUAGE> (`minecraft/lang/<CODE>.json`
  from the Minecraft assets or the Minecraft Wiki). Use it for the official
  names of mobs, blocks, items, and professions.

Look up the dialogue id of a line in `dialogues.json` before you translate it.
A line like "Oh no!" means something different when a Creeper is near than
when a villager drops bread.

## 2. The characters

Keep each voice consistent through the whole file.

- **Villagers**: silly, dramatic, easily offended, often confused, love bread
  and emeralds, gossip a lot. Short, punchy lines.
- **Baby villagers**: childish, excited, sometimes cheeky.
- **Nitwit**: dreamy and odd, believes strange theories.
- **The Mayor** (`mayor`): smooth, pompous, clearly corrupt, talks around his
  crimes with fake politeness.
- **Villager #5** (`number_5`): vain news anchor, "BREAKING NEWS!" energy,
  proud of his moustache.
- **Villager #9** (`number_9`): eager field reporter, always "live", polite
  and a bit desperate for a story.
- **Testificate Man** (`testificate_man`): self-declared superhero, dramatic,
  nobody takes him seriously.
- **Wooly** (`wooly`): a deadpan, unimpressed sheep. Very few words, dry.
- **Wandering Trader** (`wandering_trader`): pushy salesman, a little shady,
  talks to his llamas.
- **Villager Unreachable** (`unreachable`): smug, teasing, loves that you
  cannot catch him.

## 3. File format

- Copy `en_us.json` to `<CODE>.json` in the same folder.
- Set `"language"` to the name of <LANGUAGE> written in <LANGUAGE>, for example
  `"Deutsch"` or `"Español"`. Players see this name in the settings.
- `"names"`: translate the speaker names. Use the exact official names from
  the game's `<CODE>` language file for the same keys (for example
  `entity.minecraft.villager.farmer`).
- `"subtitles"`: translate every value. **Never change a key.**
- Each key is `<dialogue>.<variant>.<part>`. Parts with the same dialogue and
  variant are one voice line split over time. Keep **the same number of
  parts**, keep each part in its own key, and keep the order of ideas so each
  part still matches the moment it is shown.
- Do not add or remove keys. Missing keys fall back to English, extra keys are
  ignored.
- Save the file as UTF-8 and keep it valid JSON.

## 4. How to translate

Translate the meaning, the feeling, and the joke, not the words.

- **Stretched words**: if the English stretches a word, stretch it in
  <LANGUAGE> too, the way people there would. "Nooo" becomes a stretched "no",
  "Aaagh" a stretched scream, "sloowww" a stretched "slow".
- **Shouting**: keep words in CAPITALS where the English has them. They mark
  shouting or emphasis. If <LANGUAGE> has no capitals, use the usual way to
  show shouting in that language.
- **Stutters and hesitation**: keep them. "W-what", "Th.. that's", "Uhh...".
- **Punctuation**: keep "...", "?!", "!!" and similar. They set the rhythm.
- **Stage directions**: text in `*asterisks*`, `(brackets)` or `[brackets]`
  such as `*Gasp*`, `*Snore*`, `(laughing)` describes a sound. Translate it and
  keep the same markers.
- **Sounds and noises**: "Hmm", "Ooh", "Pfft", "Baa", "Moo" should become the
  natural sound in <LANGUAGE>.
- **Puns and wordplay**: many lines are jokes built on English wordplay. Make a
  joke that works in <LANGUAGE> about the same thing. Do not translate a pun
  word by word.
- **References**: pop-culture references (films, songs, memes) can be kept if
  <LANGUAGE> speakers know them, or swapped for a close local one.
- **Official terms**: use the official Minecraft names for mobs, blocks,
  items, effects, and professions from the game's `<CODE>` file. Keep them
  consistent with the `"names"` section.
- **Character names**: keep "Testificate Man", "Wooly", "Villager News", and
  the number names consistent everywhere. Translate "Villager #5" the same way
  every time.
- **Length**: subtitles are read while the line is spoken. Keep each part about
  as short as the English part. Check `time` and `duration` in
  `dialogues.json` when a part must be short.
- **Casual speech**: villagers talk casually. Use natural spoken <LANGUAGE>,
  not formal written style, unless the character is being fake-polite (the
  Mayor) or dramatic (Testificate Man).
- **Consistency**: the same English line appears in several dialogues. Keep
  the translation the same unless the context needs it to change.

## 5. Work in batches

The file has about 3,700 lines. Do not translate it in one go.

1. Group the keys by dialogue id in the order of `en_us.json`.
2. Translate about 150 to 250 lines at a time. For each dialogue id read its
   `title`, `body`, and `speaker` in `dialogues.json` first.
3. After each batch, check that every key exists and every part is filled in.
4. Continue until every key is done. Do not stop early and do not leave
   English text behind unless the English really is the right word (names,
   "Villager News").

## 6. Check your work

When the file is complete, check it with a small script or by hand:

- The file is valid JSON.
- `subtitles` has exactly the same keys as `en_us.json`, and no value is empty.
- `names` has the same keys as in `en_us.json`.
- No line is still in English by mistake.
- CAPITALS, stretched words, `*actions*`, and `...` from the English are still
  there in some form.
- Read ten random dialogues from start to end, with their context, and make
  sure they sound natural and funny in <LANGUAGE>.

## 7. Optional: the handbook and item names

The handbook, settings screen, and item names use a separate file. If you have
time, also translate
`src/main/resources/assets/villager-news-addon-port/lang/en_us.json` into
`lang/<CODE>.json`. Keep every key, keep `%s` placeholders, and keep the line
breaks inside the handbook text. Use the same official Minecraft terms as in
the subtitles.

## 8. Finish

- Tell the user which files you created and anything you were unsure about.
- To test in game, copy the file to `config/villager-news-addon-port/subtitles/`
  in the Minecraft folder. The Open subtitles folder button in the in-game
  translation guide opens it. Then choose the language in the Villager News
  settings, or leave it on Auto. To ship it with the mod, add it to this folder
  instead and build the mod.
