package com.vnap;

import com.vnap.command.DialogueTestCommand;
import com.vnap.config.VillagerNewsBuildSettings;
import com.vnap.config.VillagerNewsSettings;
import com.vnap.dialogue.ContextualDialogueController;
import com.vnap.dialogue.DialogueCatalog;
import com.vnap.item.VillagerNewsItems;
import com.vnap.network.DialogueAnimationPayload;
import com.vnap.network.HurtEffectPayload;
import com.vnap.network.VillagerNewsSettingsNetwork;
import com.vnap.network.VillagerNewsSettingsPayload;
import com.vnap.network.VillagerNewsSettingsRequestPayload;
import com.vnap.sound.SupplementalSoundCatalog;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillagerNewsAddonPort implements ModInitializer {
	public static final String MOD_ID = "villager-news-addon-port";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		VillagerNewsItems.register();
		PayloadTypeRegistry.playS2C().register(DialogueAnimationPayload.TYPE, DialogueAnimationPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(HurtEffectPayload.TYPE, HurtEffectPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(VillagerNewsSettingsPayload.TYPE, VillagerNewsSettingsPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(VillagerNewsSettingsPayload.TYPE, VillagerNewsSettingsPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(VillagerNewsSettingsRequestPayload.TYPE, VillagerNewsSettingsRequestPayload.CODEC);
		VillagerNewsSettings.load();
		VillagerNewsSettingsNetwork.register();
		SupplementalSoundCatalog.register();
		DialogueCatalog.register();
		ContextualDialogueController.register();
		if (VillagerNewsBuildSettings.dialogueTestCommand()) DialogueTestCommand.register();
		LOGGER.info("Villager News models, textures, and contextual dialogue are ready.");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
