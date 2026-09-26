package com.vnap.entity;

import net.minecraft.world.entity.MobSpawnType;

public interface VillagerNewsData {
	String STRUCTURE_SPAWN_TAG = "vnap_structure_spawn";

	MobSpawnType vnap$consumeSpawnType();

	boolean vnap$hasNose();

	void vnap$setHasNose(boolean value);

	int vnap$cosmetic();

	void vnap$setCosmetic(int value);

	int vnap$signMessage();

	void vnap$setSignMessage(int value);

	int vnap$signType();

	void vnap$setSignType(int value);

	boolean vnap$hasOriginalVillagerState();

	void vnap$captureOriginalVillagerState();

	void vnap$restoreOriginalVillagerState();
}
