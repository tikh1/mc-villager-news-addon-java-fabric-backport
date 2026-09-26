package com.vnap.client;

import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;

import java.util.Map;
import java.util.WeakHashMap;

public final class StableVillagerData {
	private static final Map<Villager, StableVillagerData> STABLE_DATA = new WeakHashMap<>();
	private VillagerData displayed;
	private VillagerData pending;
	private int pendingSince;

	private StableVillagerData(VillagerData displayed) {
		this.displayed = displayed;
	}

	public static VillagerData resolve(Villager villager) {
		VillagerData current = villager.getVillagerData();
		return STABLE_DATA.computeIfAbsent(villager, ignored -> new StableVillagerData(current))
			.resolve(current, villager.tickCount);
	}

	private VillagerData resolve(VillagerData current, int tick) {
		if (same(current, displayed)) {
			pending = null;
			return displayed;
		}
		if (!same(current, pending)) {
			pending = current;
			pendingSince = tick;
			return displayed;
		}
		if (tick - pendingSince >= 2) {
			displayed = current;
			pending = null;
		}
		return displayed;
	}

	// villagerdata has NO equals in 1.21.1
	private static boolean same(VillagerData first, VillagerData second) {
		return second != null && first.getType() == second.getType()
			&& first.getProfession() == second.getProfession() && first.getLevel() == second.getLevel();
	}
}
