package com.vnap.gametest;

import com.vnap.VillagerNewsAddonPort;
import com.vnap.dialogue.ContextualDialogueController;
import com.vnap.dialogue.DialogueCatalog;
import com.vnap.entity.VillagerNewsData;
import com.vnap.item.VillagerNewsItems;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

import java.util.Set;
import java.util.UUID;

public class VillagerNewsGameTests implements FabricGameTest {
	private static final Set<String> SPECIAL_NAMES = Set.of(
		"The Mayor", "Testificate Man", "Villager #5", "Villager #9", "Can't Catch Me!"
	);

	@GameTest(template = EMPTY_STRUCTURE)
	public void catalogsRecipeAndSpawnEggLookupLoad(GameTestHelper helper) {
		helper.assertTrue(DialogueCatalog.groups().size() == 523, "Expected 523 dialogue groups");
		helper.assertTrue(helper.getLevel().getRecipeManager().byKey(VillagerNewsAddonPort.id("handbook")).isPresent(),
			"The handbook recipe did not load");
		helper.assertTrue(SpawnEggItem.byId(EntityType.VILLAGER) == Items.VILLAGER_SPAWN_EGG,
			"A character spawn egg replaced the vanilla villager spawn egg lookup");
		helper.assertTrue(SpawnEggItem.byId(EntityType.SHEEP) == Items.SHEEP_SPAWN_EGG,
			"Wooly's spawn egg replaced the vanilla sheep spawn egg lookup");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void villagerNewsDataSurvivesSaving(GameTestHelper helper) {
		Villager villager = helper.spawn(EntityType.VILLAGER, 1, 2, 1);
		VillagerNewsData data = (VillagerNewsData) villager;
		data.vnap$setHasNose(false);
		data.vnap$setCosmetic(3);
		data.vnap$setSignType(2);
		data.vnap$setSignMessage(40);
		CompoundTag tag = villager.saveWithoutId(new CompoundTag());

		Villager copy = EntityType.VILLAGER.create(helper.getLevel());
		copy.load(tag);
		VillagerNewsData loaded = (VillagerNewsData) copy;
		helper.assertTrue(!loaded.vnap$hasNose(), "The missing nose was not saved");
		helper.assertTrue(loaded.vnap$cosmetic() == 3, "The cosmetic was not saved");
		helper.assertTrue(loaded.vnap$signType() == 2 && loaded.vnap$signMessage() == 40, "The held sign was not saved");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void specialTraderRestoresOriginalTrades(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Villager villager = EntityType.VILLAGER.create(level);
		villager.setVillagerData(villager.getVillagerData().setProfession(VillagerProfession.FARMER).setLevel(2));
		int originalOffers = villager.getOffers().size();
		helper.assertTrue(originalOffers > 0, "The farmer has no trades to preserve");
		villager.setCustomName(Component.literal("Mayor"));
		villager.moveTo(helper.absoluteVec(new Vec3(1.5, 2.0, 1.5)));
		level.addFreshEntity(villager);

		helper.assertTrue(ContextualDialogueController.isSpecialTrader(villager), "The Mayor is not a special trader");
		helper.assertTrue(villager.getVillagerData().getProfession() == VillagerProfession.NONE, "The Mayor kept a profession");
		helper.assertTrue(villager.getOffers().size() == 1, "The Mayor should only sell the Mayor Hat");
		MerchantOffer offer = villager.getOffers().getFirst();
		helper.assertTrue(offer.getResult().is(VillagerNewsItems.MAYOR_HAT) && offer.getCostA().is(Items.EMERALD)
			&& offer.getCostA().getCount() == 24, "The Mayor Hat trade is wrong");

		villager.setCustomName(null);
		CompoundTag tag = villager.saveWithoutId(new CompoundTag());
		villager.discard();
		Villager reloaded = EntityType.VILLAGER.create(level);
		reloaded.load(tag);
		reloaded.setUUID(UUID.randomUUID());
		reloaded.moveTo(helper.absoluteVec(new Vec3(1.5, 2.0, 1.5)));
		level.addFreshEntity(reloaded);
		helper.assertTrue(reloaded.getVillagerData().getProfession() == VillagerProfession.FARMER
			&& reloaded.getVillagerData().getLevel() == 2, "The original profession was not restored");
		helper.assertTrue(reloaded.getOffers().size() == originalOffers, "The original trades were not restored");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void spawnEggsCreateNamedCharacters(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
		Villager mayor = EntityType.VILLAGER.spawn(level, new ItemStack(VillagerNewsItems.MAYOR_VILLAGER_SPAWN_EGG),
			null, pos, MobSpawnType.SPAWN_EGG, true, false);
		helper.assertTrue(mayor != null && mayor.getCustomName() != null
			&& mayor.getCustomName().getString().equals("Mayor Villager"), "The Mayor spawn egg did not name the villager");
		helper.assertTrue(mayor.isPersistenceRequired(), "The Mayor spawn egg villager can despawn");
		helper.assertTrue(ContextualDialogueController.isSpecialTrader(mayor), "The Mayor spawn egg did not create the Mayor");

		Sheep wooly = EntityType.SHEEP.spawn(level, new ItemStack(VillagerNewsItems.WOOLY_SPAWN_EGG),
			null, pos.east(2), MobSpawnType.SPAWN_EGG, true, false);
		helper.assertTrue(wooly != null && wooly.getCustomName() != null
			&& wooly.getCustomName().getString().equals("Wooly The Sheep"), "Wooly's spawn egg did not name the sheep");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void structureSpawnReasonSurvivesChunkSaving(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
		Villager villager = EntityType.VILLAGER.create(level);
		villager.moveTo(Vec3.atBottomCenterOf(pos));
		villager.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
		helper.assertTrue(villager.getTags().contains(VillagerNewsData.STRUCTURE_SPAWN_TAG), "The structure spawn was not marked");

		Villager loaded = EntityType.VILLAGER.create(level);
		loaded.load(villager.saveWithoutId(new CompoundTag()));
		helper.assertTrue(loaded.getTags().contains(VillagerNewsData.STRUCTURE_SPAWN_TAG), "The structure spawn mark was not saved");
		level.addFreshEntity(loaded);
		helper.assertTrue(!loaded.getTags().contains(VillagerNewsData.STRUCTURE_SPAWN_TAG), "The structure spawn mark was not consumed");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "natural_special")
	public void distantStructureVillagerBecomesSpecialCharacter(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos originalSpawn = level.getSharedSpawnPos();
		float originalAngle = level.getSharedSpawnAngle();
		BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
		level.setDefaultSpawnPos(pos.offset(5000, 0, 5000), 0.0F);
		// the test world is kept between runs so earlier special spawns are forgotten first
		Scoreboard scoreboard = level.getScoreboard();
		for (String name : new String[] {"vnap_special", "vnap_special_x", "vnap_special_z"}) {
			Objective objective = scoreboard.getObjective(name);
			if (objective != null) scoreboard.removeObjective(objective);
		}
		try {
			Villager villager = EntityType.VILLAGER.create(level);
			villager.moveTo(Vec3.atBottomCenterOf(pos));
			villager.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
			level.addFreshEntity(villager);
			boolean namedCharacter = !villager.isRemoved() && villager.getCustomName() != null
				&& SPECIAL_NAMES.contains(villager.getCustomName().getString());
			boolean wooly = villager.isRemoved() && !level.getEntitiesOfClass(Sheep.class, villager.getBoundingBox().inflate(2.0),
				sheep -> sheep.getCustomName() != null && sheep.getCustomName().getString().equals("Wooly The Sheep")).isEmpty();
			helper.assertTrue(namedCharacter || wooly, "A distant structure villager did not become a special character");
		} finally {
			level.setDefaultSpawnPos(originalSpawn, originalAngle);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 600)
	public void villagerFinishesBedtimeLineBeforeSleeping(GameTestHelper helper) {
		helper.setBlock(1, 1, 1, Blocks.RED_BED);
		BlockPos bed = helper.absolutePos(new BlockPos(1, 1, 1));
		Villager villager = helper.spawn(EntityType.VILLAGER, 1, 2, 2);
		villager.setNoAi(true);
		villager.startSleeping(bed);
		helper.assertTrue(!villager.isSleeping(), "The villager slept before its bedtime line");
		helper.succeedWhen(() -> helper.assertTrue(villager.isSleeping(), "The villager never went to sleep"));
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void noseCosmeticAndSignInteractions(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Villager villager = helper.spawn(EntityType.VILLAGER, 1, 2, 1);
		villager.setNoAi(true);
		VillagerNewsData data = (VillagerNewsData) villager;
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);

		helper.assertTrue(interact(player, villager, new ItemStack(Items.SHEARS)) == InteractionResult.SUCCESS
			&& !data.vnap$hasNose(), "Shears did not remove the nose");
		helper.assertTrue(dropped(level, villager, VillagerNewsItems.VILLAGER_NOSE), "The removed nose was not dropped");

		ItemStack nose = new ItemStack(VillagerNewsItems.VILLAGER_NOSE);
		interact(player, villager, nose);
		helper.assertTrue(data.vnap$hasNose() && nose.isEmpty(), "Giving the nose back did not restore it");

		ItemStack microphone = new ItemStack(VillagerNewsItems.MICROPHONE);
		interact(player, villager, microphone);
		helper.assertTrue(data.vnap$cosmetic() == 3 && microphone.isEmpty(), "The microphone cosmetic was not given");

		interact(player, villager, new ItemStack(Items.OAK_SIGN));
		helper.assertTrue(data.vnap$signType() == 0 && data.vnap$signMessage() >= 0 && data.vnap$signMessage() < 87,
			"The oak sign was not given");

		interact(player, villager, new ItemStack(Items.SHEARS));
		helper.assertTrue(data.vnap$signType() == -1 && dropped(level, villager, Items.OAK_SIGN), "Shears did not remove the sign first");
		interact(player, villager, new ItemStack(Items.SHEARS));
		helper.assertTrue(data.vnap$cosmetic() == 0 && dropped(level, villager, VillagerNewsItems.MICROPHONE),
			"Shears did not remove the cosmetic");
		helper.assertTrue(data.vnap$hasNose(), "Shears removed the nose before the cosmetic");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void vanillaTradeSoundsAreReplaced(GameTestHelper helper) {
		Villager villager = helper.spawn(EntityType.VILLAGER, 1, 2, 1);
		helper.assertTrue(villager.getNotifyTradeSound() == SoundEvents.EMPTY, "The vanilla trade sound still plays");
		helper.succeed();
	}

	private static InteractionResult interact(Player player, Villager villager, ItemStack stack) {
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		return UseEntityCallback.EVENT.invoker().interact(player, villager.level(), InteractionHand.MAIN_HAND, villager, null);
	}

	private static boolean dropped(ServerLevel level, Villager villager, Item item) {
		return !level.getEntitiesOfClass(ItemEntity.class, villager.getBoundingBox().inflate(3.0),
			entity -> entity.getItem().is(item)).isEmpty();
	}
}
