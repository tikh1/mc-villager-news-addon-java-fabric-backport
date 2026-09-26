package com.vnap.gametest;

import com.vnap.VillagerNewsAddonPort;
import com.vnap.client.HandbookScreen;
import com.vnap.client.SubtitleLanguages;
import com.vnap.client.VillagerNewsClientSettings;
import com.vnap.client.VillagerNewsModMenu;
import com.vnap.dialogue.ContextualDialogueController;
import com.vnap.dialogue.DialogueCatalog;
import net.minecraft.client.gui.components.Button;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

// dev only visual check that only runs with the vnap visualtest flag
public final class VisualSnapshot implements ClientModInitializer {
	private static final List<Entity> SPAWNED = new ArrayList<>();
	private static BlockPos origin;
	private static Villager unreachable;
	private int ticks;

	@Override
	public void onInitializeClient() {
		if (!Boolean.getBoolean("vnap.visualTest")) return;
		ClientTickEvents.START_CLIENT_TICK.register(client -> client.options.pauseOnLostFocus = false);
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
	}

	private static final String[] CHARACTERS = {
		"villager", "Mayor", "Testificate Man", "Villager #5", "Villager #9", "Villager Unreachable",
		"wandering_trader", "baby", "jeb_", "baby jeb_", "Wooly"
	};
	private static final int PER_CHARACTER = 100;

	private void tick(Minecraft client) {
		MinecraftServer server = client.getSingleplayerServer();
		if (client.level == null || client.player == null || server == null) return;
		ticks++;
		if (ticks == 40) server.execute(() -> setUp(server));
		int characterTick = ticks - 60;
		if (characterTick >= 0 && characterTick < CHARACTERS.length * PER_CHARACTER) {
			String character = CHARACTERS[characterTick / PER_CHARACTER];
			String file = "vnap_" + character.toLowerCase().replaceAll("[^a-z0-9]+", "_");
			switch (characterTick % PER_CHARACTER) {
				case 0 -> server.execute(() -> spawnCharacter(server, character));
				case 45 -> screenshot(client, file + "_front.png");
				case 50 -> server.execute(() -> turn(90.0F));
				case 85 -> screenshot(client, file + "_side.png");
				case 95 -> server.execute(VisualSnapshot::clearCharacters);
				default -> {
				}
			}
			return;
		}
		int after = ticks - 60 - CHARACTERS.length * PER_CHARACTER;
		switch (after) {
			case 0 -> server.execute(() -> spawnUnreachable(server));
			case 1, 20, 40, 80 -> server.execute(() -> logUnreachable(server));
			case 90 -> {
				VillagerNewsClientSettings.setSubtitleLanguage("tr_tr");
				client.setScreen(new VillagerNewsModMenu().getModConfigScreenFactory().create(null));
			}
			case 120 -> screenshot(client, "vnap_modmenu.png");
			case 125 -> pressButton(client, "?");
			case 150 -> screenshot(client, "vnap_translation_guide.png");
			case 160 -> {
				client.setScreen(null);
				server.execute(() -> speak(server));
			}
			case 185 -> screenshot(client, "vnap_subtitle_tr_1.png", false);
			case 195 -> VillagerNewsClientSettings.setSubtitleLanguage("zh_cn");
			case 200 -> screenshot(client, "vnap_subtitle_zh.png", false);
			case 205 -> VillagerNewsClientSettings.setSubtitleLanguage("ru_ru");
			case 210 -> screenshot(client, "vnap_subtitle_ru.png", false);
			case 215 -> VillagerNewsClientSettings.setSubtitleLanguage("es_es");
			case 220 -> screenshot(client, "vnap_subtitle_es.png", false);
			case 225 -> {
				// the first line is over by now so say it again
				VillagerNewsClientSettings.setSubtitleLanguage("pt_br");
				server.execute(() -> {
					clearCharacters();
					speak(server);
				});
			}
			case 250 -> screenshot(client, "vnap_subtitle_pt.png", false);
			case 255 -> VillagerNewsClientSettings.setSubtitleLanguage("tr_tr");
			case 260 -> screenshot(client, "vnap_subtitle_tr_2.png", false);
			case 270 -> {
				VillagerNewsClientSettings.setSubtitleLanguage(SubtitleLanguages.AUTO);
				client.getWindow().setWindowed(1280, 720);
			}
			case 290 -> client.setScreen(new VillagerNewsModMenu().getModConfigScreenFactory().create(null));
			case 320 -> screenshot(client, "vnap_settings_large.png");
			case 330 -> client.setScreen(new HandbookScreen());
			case 360 -> screenshot(client, "vnap_handbook.png");
			case 370 -> client.stop();
			default -> {
			}
		}
	}

	private static void setUp(MinecraftServer server) {
		ServerLevel level = server.overworld();
		ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
		for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(128.0))) mob.discard();
		level.setDayTime(6000L);
		level.setWeatherParameters(12000, 0, false, false);
		origin = new BlockPos(player.getBlockX(), 200, player.getBlockZ());
		for (int x = -24; x <= 24; x++) for (int z = -24; z <= 24; z++) {
			level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
			for (int y = 0; y <= 6; y++) level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
		}
		player.teleportTo(level, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0.0F, 15.0F);
		player.getAbilities().flying = false;
		player.getAbilities().mayfly = true;
		player.onUpdateAbilities();
	}

	private static void spawnCharacter(MinecraftServer server, String character) {
		ServerLevel level = server.overworld();
		BlockPos pos = origin.offset(0, 0, 6);
		switch (character) {
			case "villager" -> spawnVillager(level, pos, null, false);
			case "baby" -> spawnVillager(level, pos, null, true);
			case "baby jeb_" -> spawnVillager(level, pos, "jeb_", true);
			case "wandering_trader" -> place(level, EntityType.WANDERING_TRADER.create(level), pos);
			case "Wooly" -> {
				Sheep wooly = EntityType.SHEEP.create(level);
				wooly.setCustomName(Component.literal("Wooly"));
				place(level, wooly, pos);
			}
			default -> spawnVillager(level, pos, character, false);
		}
	}

	private static void clearCharacters() {
		for (Entity entity : SPAWNED) entity.discard();
		SPAWNED.clear();
	}

	private static void spawnUnreachable(MinecraftServer server) {
		ServerLevel level = server.overworld();
		unreachable = EntityType.VILLAGER.create(level);
		unreachable.setCustomName(Component.literal("Can't Catch Me!"));
		unreachable.moveTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 3.5, 180.0F, 0.0F);
		unreachable.setPersistenceRequired();
		level.addFreshEntity(unreachable);
	}

	private static void logUnreachable(MinecraftServer server) {
		ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
		VillagerNewsAddonPort.LOGGER.info("VNAP-VISUAL unreachable distance={} villager={} player={} removed={}",
			String.format("%.2f", unreachable.distanceTo(player)), unreachable.position(), player.position(),
			unreachable.isRemoved());
	}

	private static void spawnVillager(ServerLevel level, BlockPos pos, String name, boolean baby) {
		Villager villager = EntityType.VILLAGER.create(level);
		villager.setVillagerData(villager.getVillagerData().setProfession(VillagerProfession.FARMER));
		if (name != null) villager.setCustomName(Component.literal(name));
		if (baby) villager.setAge(-24000);
		place(level, villager, pos);
	}

	private static void place(ServerLevel level, Mob mob, BlockPos pos) {
		mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 180.0F, 0.0F);
		mob.setYHeadRot(180.0F);
		mob.setYBodyRot(180.0F);
		mob.setNoAi(true);
		mob.setPersistenceRequired();
		level.addFreshEntity(mob);
		SPAWNED.add(mob);
	}

	private static void turn(float offset) {
		for (Entity entity : SPAWNED) {
			float yaw = 180.0F + offset;
			entity.setYRot(yaw);
			entity.setYHeadRot(yaw);
			entity.setYBodyRot(yaw);
		}
	}

	private static void speak(MinecraftServer server) {
		ServerLevel level = server.overworld();
		Villager villager = EntityType.VILLAGER.create(level);
		place(level, villager, origin.offset(0, 0, 3));
		ContextualDialogueController.playTestDialogue(level, villager, DialogueCatalog.byId("kxrhxt"), 1,
			server.getPlayerList().getPlayers().getFirst());
	}

	private static void pressButton(Minecraft client, String label) {
		if (client.screen == null) return;
		// pressing can rebuild the screen so find the button first
		client.screen.children().stream()
			.filter(child -> child instanceof Button button && button.getMessage().getString().equals(label))
			.map(Button.class::cast).findFirst().ifPresent(Button::onPress);
	}

	private static void screenshot(Minecraft client, String name) {
		screenshot(client, name, client.screen == null);
	}

	private static void screenshot(Minecraft client, String name, boolean hideGui) {
		client.options.hideGui = hideGui;
		Screenshot.grab(client.gameDirectory, name, client.getMainRenderTarget(), message -> { });
	}
}
