package com.vnap.mixin.client;

import com.vnap.client.StableVillagerData;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import org.spongepowered.asm.mixin.Mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerProfessionLayer.class)
abstract class VillagerProfessionLayerMixin {
	// adult clothing textures do NOT fit the baby model
	@Inject(
		method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void vnap$skipBabyClothing(PoseStack poseStack, MultiBufferSource buffers, int light, LivingEntity entity,
			float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch,
			CallbackInfo ci) {
		if (entity instanceof Villager villager && villager.isBaby()) ci.cancel();
	}

	@Redirect(
		method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/npc/VillagerDataHolder;getVillagerData()Lnet/minecraft/world/entity/npc/VillagerData;"
		)
	)
	private VillagerData vnap$stableVillagerData(VillagerDataHolder holder) {
		return holder instanceof Villager villager ? StableVillagerData.resolve(villager) : holder.getVillagerData();
	}
}
