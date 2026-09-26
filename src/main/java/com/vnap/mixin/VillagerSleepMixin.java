package com.vnap.mixin;

import com.vnap.dialogue.ContextualDialogueController;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerSleepMixin {
	@Inject(method = "startSleeping", at = @At("HEAD"), cancellable = true)
	private void vnap$delaySleepUntilBedtimeLineFinishes(BlockPos bedPos, CallbackInfo ci) {
		if (ContextualDialogueController.delayVillagerSleep((Villager) (Object) this, bedPos)) ci.cancel();
	}
}
