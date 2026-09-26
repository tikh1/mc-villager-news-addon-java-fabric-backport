package com.vnap.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vnap.VillagerNewsAddonPort;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SubtitleLanguages {
	public static final String DEFAULT_LANGUAGE = "en_us";
	public static final String AUTO = "auto";
	private static final String KEY_PREFIX = "subtitles." + VillagerNewsAddonPort.MOD_ID + ".dialogue.";
	private static final Pattern FILE = Pattern.compile("subtitles/([a-z]{2,3}_[a-z0-9]{2,4})\\.json");
	private static final Pattern FOLDER_FILE = Pattern.compile("([a-z]{2,3}_[a-z0-9]{2,4})\\.json");
	private static final Path FOLDER = FabricLoader.getInstance().getConfigDir()
		.resolve(VillagerNewsAddonPort.MOD_ID).resolve("subtitles");
	private static final Path STATE = FOLDER.resolveSibling("subtitles_state.json");
	// copied from the jar and kept up to date so players never edit a stale copy
	private static final List<String> FOLDER_EXTRAS = List.of("example_language.json", "ai_translation_prompt.md");
	private static Map<String, Builder> packLanguages = Map.of();
	private static Map<String, Language> languages = Map.of();
	private static Map<String, byte[]> builtIn = Map.of();
	private static Language english = new Language("English", Map.of(), Map.of(), Map.of());

	private SubtitleLanguages() {
	}

	static void register() {
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public ResourceLocation getFabricId() {
				return VillagerNewsAddonPort.id("subtitle_languages");
			}

			@Override
			public void onResourceManagerReload(ResourceManager manager) {
				load(manager);
			}
		});
	}

	private static void load(ResourceManager manager) {
		Map<String, Builder> loaded = new TreeMap<>();
		for (Map.Entry<ResourceLocation, List<Resource>> file : manager.listResourceStacks("subtitles",
				id -> id.getNamespace().equals(VillagerNewsAddonPort.MOD_ID)).entrySet()) {
			Matcher matcher = FILE.matcher(file.getKey().getPath());
			if (!matcher.matches()) continue;
			Builder builder = new Builder(matcher.group(1));
			// lowest priority pack first so later packs override single lines
			for (Resource resource : file.getValue()) {
				try (InputStream in = resource.open()) {
					byte[] bytes = in.readAllBytes();
					// the mods own copy lives in the config folder so a deleted language stays deleted
					if (Arrays.equals(bytes, builtIn().get(matcher.group(1)))) continue;
					builder.add(JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject());
				} catch (IOException | RuntimeException exception) {
					VillagerNewsAddonPort.LOGGER.warn("Could not load Villager News subtitles {} from {}",
						file.getKey(), resource.sourcePackId(), exception);
				}
			}
			if (!builder.isEmpty()) loaded.put(matcher.group(1), builder);
		}
		packLanguages = loaded;
		reloadFolder();
	}

	// files in the config folder go on top of the mod and resource packs
	public static void reloadFolder() {
		Map<String, Builder> merged = new TreeMap<>();
		packLanguages.forEach((code, builder) -> merged.put(code, builder.copy()));
		createFolder();
		try (Stream<Path> files = Files.list(FOLDER)) {
			for (Path file : files.sorted().toList()) {
				Matcher matcher = FOLDER_FILE.matcher(file.getFileName().toString());
				if (!matcher.matches()) continue;
				try {
					merged.computeIfAbsent(matcher.group(1), Builder::new)
						.add(JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject());
				} catch (IOException | RuntimeException exception) {
					VillagerNewsAddonPort.LOGGER.warn("Could not load Villager News subtitles from {}", file, exception);
				}
			}
		} catch (IOException exception) {
			VillagerNewsAddonPort.LOGGER.warn("Could not read the Villager News subtitles folder {}", FOLDER, exception);
		}
		Map<String, Language> built = new TreeMap<>();
		merged.forEach((code, builder) -> built.put(code, builder.build()));
		languages = built;
		Builder base = new Builder(DEFAULT_LANGUAGE);
		byte[] jarEnglish = builtIn().get(DEFAULT_LANGUAGE);
		if (jarEnglish != null) base.add(JsonParser.parseString(new String(jarEnglish, StandardCharsets.UTF_8)).getAsJsonObject());
		if (merged.containsKey(DEFAULT_LANGUAGE)) base.addAll(merged.get(DEFAULT_LANGUAGE));
		english = base.build();
		VillagerNewsAddonPort.LOGGER.info("Loaded Villager News subtitle languages: {}", built.keySet());
	}

	public static Path folder() {
		createFolder();
		return FOLDER;
	}

	private static void createFolder() {
		try {
			Files.createDirectories(FOLDER);
			for (String extra : FOLDER_EXTRAS) {
				byte[] bytes = jarFile(extra);
				Path target = FOLDER.resolve(extra);
				if (bytes != null && (!Files.exists(target) || !Arrays.equals(bytes, Files.readAllBytes(target)))) Files.write(target, bytes);
			}
			syncBuiltIn();
		} catch (IOException | RuntimeException exception) {
			VillagerNewsAddonPort.LOGGER.warn("Could not create the Villager News subtitles folder {}", FOLDER, exception);
		}
	}

	// the state remembers what the mod last wrote so edited files are kept and deleted ones stay gone
	private static void syncBuiltIn() throws IOException {
		JsonObject state = Files.exists(STATE)
			? JsonParser.parseString(Files.readString(STATE, StandardCharsets.UTF_8)).getAsJsonObject()
			: new JsonObject();
		boolean changed = false;
		for (Map.Entry<String, byte[]> file : builtIn().entrySet()) {
			Path target = FOLDER.resolve(file.getKey() + ".json");
			String shipped = hash(file.getValue());
			String written = state.has(file.getKey()) ? state.get(file.getKey()).getAsString() : null;
			if (!Files.exists(target)) {
				if (written != null) continue;
				Files.write(target, file.getValue());
			} else {
				String current = hash(Files.readAllBytes(target));
				if (current.equals(shipped)) {
					if (shipped.equals(written)) continue;
				} else if (current.equals(written)) {
					Files.write(target, file.getValue());
				} else {
					continue;
				}
			}
			state.addProperty(file.getKey(), shipped);
			changed = true;
		}
		if (changed) Files.writeString(STATE, state + System.lineSeparator(), StandardCharsets.UTF_8);
	}

	private static Map<String, byte[]> builtIn() {
		if (!builtIn.isEmpty()) return builtIn;
		Map<String, byte[]> found = new TreeMap<>();
		FabricLoader.getInstance().getModContainer(VillagerNewsAddonPort.MOD_ID)
			.flatMap(mod -> mod.findPath("assets/" + VillagerNewsAddonPort.MOD_ID + "/subtitles"))
			.ifPresent(dir -> {
				try (Stream<Path> files = Files.list(dir)) {
					for (Path file : files.toList()) {
						Matcher matcher = FOLDER_FILE.matcher(file.getFileName().toString());
						if (matcher.matches()) found.put(matcher.group(1), Files.readAllBytes(file));
					}
				} catch (IOException exception) {
					VillagerNewsAddonPort.LOGGER.warn("Could not list the built in Villager News subtitles", exception);
				}
			});
		builtIn = found;
		return found;
	}

	private static byte[] jarFile(String name) throws IOException {
		try (InputStream in = SubtitleLanguages.class.getResourceAsStream(
				"/assets/" + VillagerNewsAddonPort.MOD_ID + "/subtitles/" + name)) {
			return in == null ? null : in.readAllBytes();
		}
	}

	private static String hash(byte[] bytes) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
	}

	public static String subtitle(String key) {
		String id = key.startsWith(KEY_PREFIX) ? key.substring(KEY_PREFIX.length()) : key;
		Language selected = languages.get(resolve(VillagerNewsClientSettings.subtitleLanguage()));
		String line = selected == null ? null : selected.lines().get(id);
		if (line == null) line = english.lines().get(id);
		return line == null ? id : line;
	}

	public static String sign(int index) {
		Language selected = languages.get(resolve(VillagerNewsClientSettings.subtitleLanguage()));
		String text = selected == null ? null : selected.signs().get(String.valueOf(index));
		return text != null ? text : english.signs().get(String.valueOf(index));
	}

	// villagers without a name tag use the vanilla name so it is swapped for the subtitle language
	public static Component speakerName(Entity entity) {
		Component name = entity.getName();
		if (entity.hasCustomName() || !(name.getContents() instanceof TranslatableContents translatable)) return name;
		Language language = languages.get(resolve(VillagerNewsClientSettings.subtitleLanguage()));
		String translated = language == null ? null : language.names().get(translatable.getKey());
		return translated == null ? name : Component.literal(translated);
	}

	public static String resolve(String code) {
		if (AUTO.equals(code)) {
			code = Minecraft.getInstance().getLanguageManager().getSelected();
			if (!languages.containsKey(code)) {
				// other regional variants of the same language are closer than english
				String prefix = code.substring(0, code.indexOf('_') + 1);
				for (String candidate : languages.keySet()) {
					if (!prefix.isEmpty() && candidate.startsWith(prefix)) return candidate;
				}
			}
		}
		return languages.containsKey(code) ? code : DEFAULT_LANGUAGE;
	}

	public static List<String> codes() {
		List<String> codes = new ArrayList<>();
		codes.add(AUTO);
		codes.add(DEFAULT_LANGUAGE);
		for (String code : languages.keySet()) if (!code.equals(DEFAULT_LANGUAGE)) codes.add(code);
		return codes;
	}

	public static String name(String code) {
		if (AUTO.equals(code)) return I18n.get("options.villager-news-addon-port.subtitle_language.auto", name(resolve(AUTO)));
		Language language = DEFAULT_LANGUAGE.equals(code) ? languages.getOrDefault(code, english) : languages.get(code);
		return language == null ? code : language.name();
	}

	public static String next(String current) {
		List<String> codes = codes();
		return codes.get((Math.max(0, codes.indexOf(current)) + 1) % codes.size());
	}

	private record Language(String name, Map<String, String> lines, Map<String, String> names, Map<String, String> signs) {
	}

	private static final class Builder {
		private String name;
		private final Map<String, String> lines = new HashMap<>();
		private final Map<String, String> names = new HashMap<>();
		private final Map<String, String> signs = new HashMap<>();

		private Builder(String code) {
			name = code;
		}

		private void add(JsonObject root) {
			if (root.has("language")) name = root.get("language").getAsString();
			if (root.has("subtitles")) {
				for (Map.Entry<String, JsonElement> line : root.getAsJsonObject("subtitles").entrySet()) {
					lines.put(line.getKey(), line.getValue().getAsString());
				}
			}
			if (root.has("names")) {
				for (Map.Entry<String, JsonElement> line : root.getAsJsonObject("names").entrySet()) {
					names.put(line.getKey(), line.getValue().getAsString());
				}
			}
			if (root.has("signs")) {
				for (Map.Entry<String, JsonElement> line : root.getAsJsonObject("signs").entrySet()) {
					signs.put(line.getKey(), line.getValue().getAsString());
				}
			}
		}

		private void addAll(Builder other) {
			name = other.name;
			lines.putAll(other.lines);
			names.putAll(other.names);
			signs.putAll(other.signs);
		}

		private boolean isEmpty() {
			return lines.isEmpty() && names.isEmpty() && signs.isEmpty();
		}

		private Builder copy() {
			Builder copy = new Builder(name);
			copy.lines.putAll(lines);
			copy.names.putAll(names);
			copy.signs.putAll(signs);
			return copy;
		}

		private Language build() {
			return new Language(name, Map.copyOf(lines), Map.copyOf(names), Map.copyOf(signs));
		}
	}
}
