package com.vnap.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.FittingMultiLineTextWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class HandbookScreen extends Screen {
	private static HandbookData data;
	private static String dataLanguage;
	private static final int ROWS = 8;
	private static final String PREFIX = "gui.villager-news-addon-port.";
	private static final String TEXT = "handbook.villager-news-addon-port.";
	private final Screen parent;
	private final boolean settingsOnly;
	private Page page;
	private Page returnPage = Page.TRIGGERS;
	private int pageIndex;
	private int entryIndex;
	private int categoryIndex;
	private int sectionIndex;
	private String search = "";
	private Entry detail;

	public HandbookScreen() {
		this(null, Page.HOME, false);
	}

	private HandbookScreen(Screen parent, Page page, boolean settingsOnly) {
		super(Component.literal(t("title")));
		this.parent = parent;
		this.page = page;
		this.settingsOnly = settingsOnly;
	}

	public static HandbookScreen settingsScreen(Screen parent) {
		VillagerNewsSettingsState.prepareConfigScreen();
		return new HandbookScreen(parent, Page.SETTINGS, true);
	}

	@Override
	protected void init() {
		int contentWidth = Math.min(380, width - 32);
		int left = (width - contentWidth) / 2;
		addText(left, 16, contentWidth, Component.literal(titleForPage()).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), true);
		switch (page) {
			case HOME -> buildHome(left, contentWidth);
			case GUIDE -> buildGuide(left, contentWidth);
			case OVERVIEW -> buildEntryPage(left, contentWidth, data().overview, Page.GUIDE);
			case SPECIALS -> buildEntryPage(left, contentWidth, data().specialVillagers, Page.GUIDE);
			case COSMETICS -> buildEntryPage(left, contentWidth, data().cosmetics, Page.GUIDE);
			case GENERAL -> buildEntryPage(left, contentWidth, data().generalInformation, Page.TRIGGERS);
			case SETTINGS -> buildSettings(left, contentWidth);
			case SOCIALS -> buildEntryPage(left, contentWidth, data().socials, Page.HOME);
			case SUPPORT -> buildSupport(left, contentWidth);
			case TRANSLATING -> buildTranslating(left, contentWidth);
			case TRIGGERS -> buildTriggers(left, contentWidth);
			case CATEGORY -> buildCategory(left, contentWidth);
			case SECTION -> buildSection(left, contentWidth);
			case DETAIL -> buildDetail(left, contentWidth);
		}
	}

	private void buildHome(int left, int contentWidth) {
		addText(left, 48, contentWidth, Component.literal(data().headline), true);
		int y = 126;
		addMenuButton(left, y, contentWidth, t("guide"), Page.GUIDE);
		addMenuButton(left, y + 24, contentWidth, t("settings"), Page.SETTINGS);
		addMenuButton(left, y + 48, contentWidth, t("socials"), Page.SOCIALS);
		addMenuButton(left, y + 72, contentWidth, t("support"), Page.SUPPORT);
		addCredit(left, contentWidth, y + 102);
		addRenderableWidget(Button.builder(Component.literal(t("close")), button -> onClose())
			.bounds(left, height - 30, contentWidth, 20).build());
	}

	private void buildGuide(int left, int contentWidth) {
		addText(left, 44, contentWidth, Component.literal(data().guideIntro), true);
		int y = 112;
		addMenuButton(left, y, contentWidth, t("overview"), Page.OVERVIEW);
		addMenuButton(left, y + 24, contentWidth, t("special_villagers"), Page.SPECIALS);
		addMenuButton(left, y + 48, contentWidth, t("cosmetics"), Page.COSMETICS);
		addMenuButton(left, y + 72, contentWidth, t("triggers"), Page.TRIGGERS);
		addBackButton(left, contentWidth, Page.HOME);
	}

	private void buildTriggers(int left, int contentWidth) {
		EditBox field = new EditBox(font, left, 44, contentWidth - 62, 20, Component.literal(t("search")));
		field.setValue(search);
		field.setMaxLength(80);
		field.setHint(Component.literal(t("search")));
		addRenderableWidget(field);
		addRenderableWidget(Button.builder(Component.literal(t("go")), button -> {
			search = field.getValue().trim();
			pageIndex = 0;
			rebuildWidgets();
		}).bounds(left + contentWidth - 58, 44, 58, 20).build());
		if (!search.isBlank()) {
			buildSearchResults(left, contentWidth);
			return;
		}
		addText(left, 70, contentWidth, Component.literal(t("browse")), true);
		addRenderableWidget(Button.builder(Component.literal(t("general_information")), button -> navigate(Page.GENERAL))
			.bounds(left, 94, contentWidth, 20).build());
		List<Category> categories = data().categories;
		int start = pageIndex * ROWS;
		for (int index = start; index < Math.min(categories.size(), start + ROWS); index++) {
			int selected = index;
			addRenderableWidget(Button.builder(Component.literal(categories.get(index).title), button -> {
				categoryIndex = selected;
				pageIndex = 0;
				page = Page.CATEGORY;
				rebuildWidgets();
			}).bounds(left, 118 + (index - start) * 22, contentWidth, 20).build());
		}
		addPager(left, contentWidth, categories.size(), Page.GUIDE);
	}

	private void buildSearchResults(int left, int contentWidth) {
		String query = search.toLowerCase(Locale.ROOT);
		List<Entry> results = data().searchable.stream()
			.filter(entry -> clean(entry.title).toLowerCase(Locale.ROOT).contains(query)
				|| clean(entry.body).toLowerCase(Locale.ROOT).contains(query))
			.sorted(Comparator.comparing(Entry::title, String.CASE_INSENSITIVE_ORDER))
			.toList();
		addText(left, 70, contentWidth, Component.literal(t("matching", results.size())), true);
		int start = pageIndex * ROWS;
		for (int index = start; index < Math.min(results.size(), start + ROWS); index++) {
			Entry entry = results.get(index);
			addRenderableWidget(Button.builder(Component.literal(clean(entry.title)), button -> openDetail(entry, Page.TRIGGERS))
				.bounds(left, 94 + (index - start) * 22, contentWidth, 20).build());
		}
		if (results.isEmpty()) addText(left, 110, contentWidth, Component.literal(t("no_results")).withStyle(ChatFormatting.RED), true);
		addPager(left, contentWidth, results.size(), Page.GUIDE);
	}

	private void buildCategory(int left, int contentWidth) {
		Category category = data().categories.get(categoryIndex);
		addText(left, 44, contentWidth, Component.literal(t("choose_section")), true);
		int start = pageIndex * ROWS;
		for (int index = start; index < Math.min(category.sections.size(), start + ROWS); index++) {
			int selected = index;
			addRenderableWidget(Button.builder(Component.literal(category.sections.get(index).title), button -> {
				sectionIndex = selected;
				pageIndex = 0;
				page = Page.SECTION;
				rebuildWidgets();
			}).bounds(left, 72 + (index - start) * 22, contentWidth, 20).build());
		}
		addPager(left, contentWidth, category.sections.size(), Page.TRIGGERS);
	}

	private void buildSection(int left, int contentWidth) {
		Section section = data().categories.get(categoryIndex).sections.get(sectionIndex);
		List<Entry> groups = new ArrayList<>();
		for (String id : section.groups) {
			Entry entry = data().contexts.get(id);
			if (entry != null && !entry.title.isBlank()) groups.add(entry);
		}
		groups.addAll(section.entries);
		addText(left, 44, contentWidth, Component.literal(t("choose_trigger")), true);
		int start = pageIndex * ROWS;
		for (int index = start; index < Math.min(groups.size(), start + ROWS); index++) {
			Entry entry = groups.get(index);
			addRenderableWidget(Button.builder(Component.literal(clean(entry.title)), button -> openDetail(entry, Page.SECTION))
				.bounds(left, 76 + (index - start) * 22, contentWidth, 20).build());
		}
		if (groups.isEmpty()) addText(left, 100, contentWidth, Component.literal(t("section_covered")), true);
		addPager(left, contentWidth, groups.size(), Page.CATEGORY);
	}

	private void buildDetail(int left, int contentWidth) {
		if (detail != null) {
			addScrollingText(left, 52, contentWidth, height - 36, Component.literal(clean(detail.body)));
		}
		addBackButton(left, contentWidth, returnPage);
	}

	private void buildEntryPage(int left, int contentWidth, List<Entry> entries, Page back) {
		Entry entry = entries.get(Math.max(0, Math.min(entryIndex, entries.size() - 1)));
		addText(left, 48, contentWidth, Component.literal(clean(entry.title)).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), true);
		addScrollingText(left, 74, contentWidth, height - 60, Component.literal(clean(entry.body)));
		int half = (contentWidth - 6) / 2;
		Button previous = Button.builder(Component.literal(t("previous")), button -> {
			entryIndex--;
			rebuildWidgets();
		}).bounds(left, height - 54, half, 20).build();
		previous.active = entryIndex > 0;
		addRenderableWidget(previous);
		Button next = Button.builder(Component.literal(t("next")), button -> {
			entryIndex++;
			rebuildWidgets();
		}).bounds(left + half + 6, height - 54, half, 20).build();
		next.active = entryIndex + 1 < entries.size();
		addRenderableWidget(next);
		addBackButton(left, contentWidth, back);
	}

	private void buildSupport(int left, int contentWidth) {
		addScrollingText(left, 52, contentWidth, height - 36, Component.literal(data().support));
		addBackButton(left, contentWidth, Page.HOME);
	}

	private void buildTranslating(int left, int contentWidth) {
		addScrollingText(left, 40, contentWidth, height - 60, Component.literal(t("translation_guide")));
		addRenderableWidget(Button.builder(Component.literal(t("open_subtitles_folder")),
			button -> Util.getPlatform().openFile(SubtitleLanguages.folder().toFile()))
			.bounds(left, height - 54, contentWidth, 20).build());
		addBackButton(left, contentWidth, Page.SETTINGS);
	}

	private void buildSettings(int left, int contentWidth) {
		SubtitleLanguages.reloadFolder();
		boolean canEdit = VillagerNewsSettingsState.canEdit();
		addText(left, 42, contentWidth, Component.literal(canEdit
			? VillagerNewsSettingsState.localSettings()
				? t("settings_local")
				: t("settings_server")
			: t("settings_operator")), true);
		int labelWidth = Math.min(166, contentWidth / 2);
		int buttonLeft = left + labelWidth;
		int buttonWidth = contentWidth - labelWidth;
		int y = 66;
		int rowSpacing = Math.max(21, Math.min(26, (height - 36 - y - 20) / 5));
		addText(left, y + 6, labelWidth - 6, Component.literal(t("subtitles")), false);
		addRenderableWidget(Button.builder(Component.literal(toggleLabel(VillagerNewsClientSettings.showSubtitles())), button -> {
			boolean enabled = !VillagerNewsClientSettings.showSubtitles();
			VillagerNewsClientSettings.setShowSubtitles(enabled);
			button.setMessage(Component.literal(toggleLabel(enabled)));
		}).bounds(buttonLeft, y, buttonWidth, 20).build());
		y += rowSpacing;
		addText(left, y + 6, labelWidth - 6, Component.literal(t("subtitle_language")), false);
		addRenderableWidget(Button.builder(Component.literal(SubtitleLanguages.name(VillagerNewsClientSettings.subtitleLanguage())), button -> {
			String language = SubtitleLanguages.next(VillagerNewsClientSettings.subtitleLanguage());
			VillagerNewsClientSettings.setSubtitleLanguage(language);
			button.setMessage(Component.literal(SubtitleLanguages.name(language)));
		}).bounds(buttonLeft, y, buttonWidth - 24, 20).build());
		addRenderableWidget(Button.builder(Component.literal("?"), button -> navigate(Page.TRANSLATING))
			.bounds(buttonLeft + buttonWidth - 20, y, 20, 20)
			.tooltip(Tooltip.create(Component.literal(t("translate_tooltip")))).build());
		y += rowSpacing;
		addText(left, y + 6, labelWidth - 6, Component.literal(t("chattiness")), false);
		Button chattiness = Button.builder(Component.literal(chattinessLabel(VillagerNewsSettingsState.chattiness())), button -> {
			VillagerNewsSettingsState.setChattiness(VillagerNewsSettingsState.chattiness() + 1);
			button.setMessage(Component.literal(chattinessLabel(VillagerNewsSettingsState.chattiness())));
		}).bounds(buttonLeft, y, buttonWidth, 20).build();
		chattiness.active = canEdit;
		addRenderableWidget(chattiness);
		y += rowSpacing;
		addText(left, y + 6, labelWidth - 6, Component.literal(t("rare_voicelines")), false);
		Button rareVoicelines = Button.builder(Component.literal(rareLabel(VillagerNewsSettingsState.rareVoicelines())), button -> {
			VillagerNewsSettingsState.setRareVoicelines(VillagerNewsSettingsState.rareVoicelines() + 1);
			button.setMessage(Component.literal(rareLabel(VillagerNewsSettingsState.rareVoicelines())));
		}).bounds(buttonLeft, y, buttonWidth, 20).build();
		rareVoicelines.active = canEdit;
		addRenderableWidget(rareVoicelines);
		y += rowSpacing;
		addText(left, y + 6, labelWidth - 6, Component.literal(t("spawn_special_villagers")), false);
		Button spawnSpecialVillagers = Button.builder(Component.literal(toggleLabel(VillagerNewsSettingsState.spawnSpecialVillagers())), button -> {
			VillagerNewsSettingsState.setSpawnSpecialVillagers(!VillagerNewsSettingsState.spawnSpecialVillagers());
			button.setMessage(Component.literal(toggleLabel(VillagerNewsSettingsState.spawnSpecialVillagers())));
		}).bounds(buttonLeft, y, buttonWidth, 20).build();
		spawnSpecialVillagers.active = canEdit;
		addRenderableWidget(spawnSpecialVillagers);
		y += rowSpacing;
		addText(left, y + 6, labelWidth - 6, Component.literal(t("villager_style")), false);
		Button style = Button.builder(Component.literal("Villager News"), button -> {
		}).bounds(buttonLeft, y, buttonWidth, 20).build();
		style.active = false;
		addRenderableWidget(style);
		addCredit(left, contentWidth, y + rowSpacing + 4);
		if (settingsOnly) {
			addRenderableWidget(Button.builder(Component.literal(t("done")), button -> onClose())
				.bounds(left, height - 30, contentWidth, 20).build());
		} else addBackButton(left, contentWidth, Page.HOME);
	}

	private void addCredit(int left, int contentWidth, int y) {
		if (y + font.lineHeight <= height - 36) {
			addText(left, y, contentWidth, Component.literal(t("credit")).withStyle(ChatFormatting.GRAY), true);
		}
	}

	private static String toggleLabel(boolean enabled) {
		return enabled ? t("on") : t("off");
	}

	private static String chattinessLabel(int value) {
		return switch (value) {
			case 0 -> t("muted");
			case 1 -> t("shy");
			case 3 -> t("super_chatty");
			default -> t("chatty");
		};
	}

	private static String rareLabel(int value) {
		return switch (value) {
			case 0 -> t("never");
			case 2 -> t("often");
			default -> t("default");
		};
	}

	private void addPager(int left, int contentWidth, int count, Page back) {
		int pages = Math.max(1, (count + ROWS - 1) / ROWS);
		int third = (contentWidth - 12) / 3;
		Button previous = Button.builder(Component.literal(t("previous")), button -> {
			pageIndex--;
			rebuildWidgets();
		}).bounds(left, height - 30, third, 20).build();
		previous.active = pageIndex > 0;
		addRenderableWidget(previous);
		addRenderableWidget(Button.builder(Component.literal(t("back")), button -> navigate(back))
			.bounds(left + third + 6, height - 30, third, 20).build());
		Button next = Button.builder(Component.literal(t("next")), button -> {
			pageIndex++;
			rebuildWidgets();
		}).bounds(left + (third + 6) * 2, height - 30, third, 20).build();
		next.active = pageIndex + 1 < pages;
		addRenderableWidget(next);
	}

	private void addMenuButton(int left, int y, int contentWidth, String label, Page destination) {
		addRenderableWidget(Button.builder(Component.literal(label), button -> navigate(destination))
			.bounds(left, y, contentWidth, 20).build());
	}

	private void addBackButton(int left, int contentWidth, Page destination) {
		addRenderableWidget(Button.builder(Component.literal(t("back")), button -> navigate(destination))
			.bounds(left, height - 30, contentWidth, 20).build());
	}

	private void navigate(Page destination) {
		page = destination;
		pageIndex = 0;
		entryIndex = 0;
		if (destination != Page.TRIGGERS) search = "";
		rebuildWidgets();
	}

	private void openDetail(Entry entry, Page back) {
		detail = entry;
		returnPage = back;
		page = Page.DETAIL;
		rebuildWidgets();
	}

	// long text scrolls instead of running into the buttons
	private void addScrollingText(int x, int y, int textWidth, int bottom, Component text) {
		addRenderableWidget(new FittingMultiLineTextWidget(x, y, textWidth, Math.max(font.lineHeight, bottom - y), text, font));
	}

	private MultiLineTextWidget addText(int x, int y, int textWidth, Component text, boolean centered) {
		MultiLineTextWidget widget = new MultiLineTextWidget(x, y, text, font).setMaxWidth(textWidth).setCentered(centered);
		addRenderableWidget(widget);
		return widget;
	}

	private String titleForPage() {
		return switch (page) {
			case HOME -> t("title");
			case GUIDE -> t("guide");
			case OVERVIEW -> t("overview");
			case SPECIALS -> t("special_villagers");
			case COSMETICS -> t("cosmetics");
			case GENERAL -> t("general_information");
			case SETTINGS -> t("settings");
			case SOCIALS -> t("socials");
			case SUPPORT -> t("support");
			case TRANSLATING -> t("translating");
			case TRIGGERS -> t("triggers");
			case CATEGORY -> data().categories.get(categoryIndex).title;
			case SECTION -> data().categories.get(categoryIndex).sections.get(sectionIndex).title;
			case DETAIL -> {
				yield detail == null ? t("trigger") : clean(detail.title);
			}
		};
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	private static String t(String key, Object... args) {
		return I18n.get(PREFIX + key, args);
	}

	private static String clean(String value) {
		return value.replace("Â", "").replaceAll("§[0-9a-fk-or]", "");
	}

	private static HandbookData data() {
		String language = Minecraft.getInstance().getLanguageManager().getSelected();
		if (data == null || !language.equals(dataLanguage)) {
			data = load();
			dataLanguage = language;
		}
		return data;
	}

	// handbook json is the fallback and every language including english lives in the lang files
	private static String text(String key, String english) {
		return Language.getInstance().getOrDefault(TEXT + key, english);
	}

	private static HandbookData load() {
		String path = "/assets/villager-news-addon-port/handbook.json";
		try (InputStream stream = HandbookScreen.class.getResourceAsStream(path)) {
			if (stream == null) throw new IOException("Missing " + path);
			JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			List<Category> categories = new ArrayList<>();
			Map<String, Entry> contexts = new LinkedHashMap<>();
			for (Map.Entry<String, JsonElement> context : root.getAsJsonObject("contexts").entrySet()) {
				JsonObject value = context.getValue().getAsJsonObject();
				String title = value.get("browseTitle").getAsString();
				if (title.isBlank()) title = value.get("title").getAsString();
				String key = "context." + context.getKey();
				contexts.put(context.getKey(), new Entry(text(key + ".title", title), text(key + ".body", value.get("body").getAsString())));
			}
			List<Entry> searchable = new ArrayList<>();
			for (JsonElement categoryElement : root.getAsJsonArray("categories")) {
				JsonObject category = categoryElement.getAsJsonObject();
				List<Section> sections = new ArrayList<>();
				for (JsonElement sectionElement : category.getAsJsonArray("sections")) {
					JsonObject section = sectionElement.getAsJsonObject();
					List<String> groups = new ArrayList<>();
					for (JsonElement group : section.getAsJsonArray("groups")) groups.add(group.getAsString());
					String sectionKey = "section." + section.get("id").getAsString();
					List<Entry> sectionEntries = entries(section.getAsJsonArray("entries"), sectionKey);
					for (String group : groups) {
						Entry entry = contexts.get(group);
						if (entry != null && !entry.title.isBlank()) searchable.add(entry);
					}
					searchable.addAll(sectionEntries);
					sections.add(new Section(text(sectionKey + ".title", section.get("title").getAsString()), List.copyOf(groups),
						sectionEntries));
				}
				categories.add(new Category(text("category." + categories.size() + ".title", category.get("title").getAsString()),
					List.copyOf(sections)));
			}
			return new HandbookData(
				text("headline", root.get("headline").getAsString()),
				text("guide_intro", root.get("guideIntro").getAsString()),
				entries(root.getAsJsonArray("overview"), "overview"),
				entries(root.getAsJsonArray("specialVillagers"), "special_villagers"),
				entries(root.getAsJsonArray("cosmetics"), "cosmetics"),
				entries(root.getAsJsonArray("generalInformation"), "general_information"),
				entries(root.getAsJsonArray("socials"), "socials"),
				entries(root.getAsJsonArray("settings"), "settings"),
				text("support", root.get("support").getAsString()),
				List.copyOf(categories),
				Map.copyOf(contexts),
				List.copyOf(searchable)
			);
		} catch (IOException | RuntimeException exception) {
			throw new IllegalStateException("Could not load the Villager News handbook", exception);
		}
	}

	private static List<Entry> entries(JsonArray array, String key) {
		List<Entry> result = new ArrayList<>();
		for (JsonElement element : array) {
			JsonObject entry = element.getAsJsonObject();
			String entryKey = key + "." + result.size();
			result.add(new Entry(text(entryKey + ".title", entry.get("title").getAsString()),
				text(entryKey + ".body", entry.get("body").getAsString())));
		}
		return List.copyOf(result);
	}

	private enum Page {
		HOME, GUIDE, OVERVIEW, SPECIALS, COSMETICS, GENERAL, SETTINGS, SOCIALS, SUPPORT, TRANSLATING, TRIGGERS, CATEGORY, SECTION,
		DETAIL
	}

	private record Entry(String title, String body) {
	}

	private record Section(String title, List<String> groups, List<Entry> entries) {
	}

	private record Category(String title, List<Section> sections) {
	}

	private record HandbookData(String headline, String guideIntro, List<Entry> overview,
		List<Entry> specialVillagers, List<Entry> cosmetics, List<Entry> generalInformation,
		List<Entry> socials, List<Entry> settings, String support, List<Category> categories,
		Map<String, Entry> contexts, List<Entry> searchable) {
	}
}
