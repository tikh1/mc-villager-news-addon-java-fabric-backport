package com.vnap.mixin.client;

import com.vnap.client.ItemDisplayModels;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
	@ModifyVariable(
		method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
		at = @At("HEAD"),
		argsOnly = true
	)
	private BakedModel vnap$selectDisplayModel(BakedModel model, ItemStack stack, ItemDisplayContext context) {
		return stack.isEmpty() ? model : ItemDisplayModels.select(stack, context, model);
	}
}
