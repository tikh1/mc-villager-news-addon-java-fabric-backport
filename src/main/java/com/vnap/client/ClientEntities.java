package com.vnap.client;

import com.vnap.mixin.client.LevelEntitiesAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

final class ClientEntities {
	private ClientEntities() {
	}

	static Entity get(ClientLevel level, UUID id) {
		return level == null || id == null ? null : ((LevelEntitiesAccessor) level).vnap$getEntities().get(id);
	}
}
