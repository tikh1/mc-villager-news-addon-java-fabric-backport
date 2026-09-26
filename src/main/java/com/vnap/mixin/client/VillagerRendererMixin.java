package com.vnap.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerRenderer.class)
public abstract class VillagerRendererMixin {
	// the baby model is already full size so the vanilla half scale is skipped
	@Inject(method = "scale(Lnet/minecraft/world/entity/npc/Villager;Lcom/mojang/blaze3d/vertex/PoseStack;F)V",
		at = @At("HEAD"), cancellable = true)
	private void vnap$keepBabyModelScale(Villager villager, PoseStack poseStack, float partialTick, CallbackInfo ci) {
		if (villager.isBaby()) ci.cancel();
	}
}
