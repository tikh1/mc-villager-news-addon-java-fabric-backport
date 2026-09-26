package com.vnap.client;

import com.vnap.VillagerNewsAddonPort;
import com.vnap.item.VillagerNewsItems;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

// 1.21.1 has no item model definitions so held and worn models are picked by display context
public final class ItemDisplayModels {
	private static final Map<Item, ResourceLocation> HELD = Map.of(
		VillagerNewsItems.HANDBOOK, VillagerNewsAddonPort.id("item/handbook_held"),
		VillagerNewsItems.MICROPHONE, VillagerNewsAddonPort.id("item/microphone_held")
	);
	private static final Map<Item, ResourceLocation> WORN = Map.of(
		VillagerNewsItems.MAYOR_HAT, VillagerNewsAddonPort.id("item/mayor_hat_worn"),
		VillagerNewsItems.MOUSTACHE, VillagerNewsAddonPort.id("item/moustache_worn"),
		VillagerNewsItems.TESTIFICATE_MAN_HELMET, VillagerNewsAddonPort.id("item/testificate_man_helmet_worn"),
		VillagerNewsItems.VILLAGER_NOSE, VillagerNewsAddonPort.id("item/villager_nose_worn")
	);

	private ItemDisplayModels() {
	}

	static void register() {
		ModelLoadingPlugin.register(context -> {
			context.addModels(HELD.values());
			context.addModels(WORN.values());
		});
	}

	public static BakedModel select(ItemStack stack, ItemDisplayContext context, BakedModel model) {
		ResourceLocation id = switch (context) {
			case HEAD -> WORN.get(stack.getItem());
			case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> HELD.get(stack.getItem());
			default -> null;
		};
		if (id == null) return model;
		BakedModel replacement = Minecraft.getInstance().getModelManager().getModel(id);
		return replacement == null ? model : replacement;
	}
}
