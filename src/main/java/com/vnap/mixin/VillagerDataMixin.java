package com.vnap.mixin;

import com.vnap.dialogue.ContextualDialogueController;
import com.vnap.entity.VillagerNewsData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Villager.class)
public abstract class VillagerDataMixin implements VillagerNewsData {
	@Unique
	private static final EntityDataAccessor<Boolean> VNAP_HAS_NOSE = SynchedEntityData.defineId(Villager.class, EntityDataSerializers.BOOLEAN);
	@Unique
	private static final EntityDataAccessor<Integer> VNAP_COSMETIC = SynchedEntityData.defineId(Villager.class, EntityDataSerializers.INT);
	@Unique
	private static final EntityDataAccessor<Integer> VNAP_SIGN_MESSAGE = SynchedEntityData.defineId(Villager.class, EntityDataSerializers.INT);
	@Unique
	private static final EntityDataAccessor<Integer> VNAP_SIGN_TYPE = SynchedEntityData.defineId(Villager.class, EntityDataSerializers.INT);
	@Unique
	private VillagerData vnap$originalVillagerData;
	@Unique
	private MerchantOffers vnap$originalVillagerOffers;
	@Unique
	private MobSpawnType vnap$spawnType;

	@Inject(method = "defineSynchedData", at = @At("TAIL"))
	private void vnap$defineData(SynchedEntityData.Builder builder, CallbackInfo ci) {
		builder.define(VNAP_HAS_NOSE, true);
		builder.define(VNAP_COSMETIC, 0);
		builder.define(VNAP_SIGN_MESSAGE, -1);
		builder.define(VNAP_SIGN_TYPE, -1);
	}

	@Inject(method = "finalizeSpawn", at = @At("HEAD"))
	private void vnap$rememberSpawnType(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
			SpawnGroupData spawnData, CallbackInfoReturnable<SpawnGroupData> cir) {
		vnap$spawnType = spawnType;
		// worldgen villagers are saved to nbt BEFORE they load so the reason is kept as a tag
		if (spawnType == MobSpawnType.STRUCTURE) ((Villager) (Object) this).addTag(STRUCTURE_SPAWN_TAG);
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void vnap$saveData(CompoundTag output, CallbackInfo ci) {
		Villager villager = (Villager) (Object) this;
		output.putBoolean("VillagerNewsHasNose", vnap$hasNose());
		output.putInt("VillagerNewsCosmetic", vnap$cosmetic());
		output.putInt("VillagerNewsSignMessage", vnap$signMessage());
		output.putInt("VillagerNewsSignType", vnap$signType());
		if (vnap$originalVillagerData != null && vnap$originalVillagerOffers != null) {
			VillagerData.CODEC.encodeStart(NbtOps.INSTANCE, vnap$originalVillagerData)
				.ifSuccess(tag -> output.put("VillagerNewsOriginalData", tag));
			MerchantOffers.CODEC.encodeStart(villager.registryAccess().createSerializationContext(NbtOps.INSTANCE), vnap$originalVillagerOffers)
				.ifSuccess(tag -> output.put("VillagerNewsOriginalOffers", tag));
		}
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void vnap$loadData(CompoundTag input, CallbackInfo ci) {
		Villager villager = (Villager) (Object) this;
		vnap$setHasNose(!input.contains("VillagerNewsHasNose", Tag.TAG_BYTE) || input.getBoolean("VillagerNewsHasNose"));
		vnap$setCosmetic(input.getInt("VillagerNewsCosmetic"));
		int signMessage = input.contains("VillagerNewsSignMessage", Tag.TAG_INT) ? input.getInt("VillagerNewsSignMessage") : -1;
		vnap$setSignMessage(signMessage);
		int equippedSign = ContextualDialogueController.signType(villager.getMainHandItem());
		vnap$setSignType(input.contains("VillagerNewsSignType", Tag.TAG_INT) ? input.getInt("VillagerNewsSignType")
			: equippedSign >= 0 ? equippedSign : signMessage >= 0 ? 0 : -1);
		if (equippedSign >= 0) villager.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		vnap$originalVillagerData = input.contains("VillagerNewsOriginalData")
			? VillagerData.CODEC.parse(NbtOps.INSTANCE, input.get("VillagerNewsOriginalData")).result().orElse(null)
			: null;
		vnap$originalVillagerOffers = input.contains("VillagerNewsOriginalOffers")
			? MerchantOffers.CODEC.parse(villager.registryAccess().createSerializationContext(NbtOps.INSTANCE),
				input.get("VillagerNewsOriginalOffers")).result().orElse(null)
			: null;
		if (vnap$originalVillagerData == null || vnap$originalVillagerOffers == null) {
			vnap$originalVillagerData = null;
			vnap$originalVillagerOffers = null;
		}
	}

	@Redirect(
		method = "customServerAiStep",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/Villager;stopTrading()V")
	)
	private void vnap$keepSpecialTradeOpen(Villager villager) {
		if (!ContextualDialogueController.isSpecialTrader(villager)) villager.setTradingPlayer(null);
	}

	@ModifyVariable(method = "setVillagerData", at = @At("HEAD"), argsOnly = true)
	private VillagerData vnap$preventSpecialProfession(VillagerData value) {
		Villager villager = (Villager) (Object) this;
		return ContextualDialogueController.isSpecialTrader(villager)
			? value.setProfession(VillagerProfession.NONE).setLevel(1)
			: value;
	}

	@Override
	public MobSpawnType vnap$consumeSpawnType() {
		Villager villager = (Villager) (Object) this;
		MobSpawnType spawnType = vnap$spawnType;
		vnap$spawnType = null;
		if (villager.removeTag(STRUCTURE_SPAWN_TAG) && spawnType == null) spawnType = MobSpawnType.STRUCTURE;
		return spawnType;
	}

	@Override
	public boolean vnap$hasOriginalVillagerState() {
		return vnap$originalVillagerData != null && vnap$originalVillagerOffers != null;
	}

	@Override
	public void vnap$captureOriginalVillagerState() {
		if (vnap$hasOriginalVillagerState()) return;
		Villager villager = (Villager) (Object) this;
		vnap$originalVillagerData = villager.getVillagerData();
		vnap$originalVillagerOffers = villager.getOffers().copy();
	}

	@Override
	public void vnap$restoreOriginalVillagerState() {
		if (!vnap$hasOriginalVillagerState()) return;
		Villager villager = (Villager) (Object) this;
		VillagerData originalData = vnap$originalVillagerData;
		MerchantOffers originalOffers = vnap$originalVillagerOffers.copy();
		vnap$originalVillagerData = null;
		vnap$originalVillagerOffers = null;
		villager.setVillagerData(originalData);
		villager.getOffers().clear();
		villager.getOffers().addAll(originalOffers);
	}

	@Override
	public boolean vnap$hasNose() {
		return ((Villager) (Object) this).getEntityData().get(VNAP_HAS_NOSE);
	}

	@Override
	public void vnap$setHasNose(boolean value) {
		((Villager) (Object) this).getEntityData().set(VNAP_HAS_NOSE, value);
	}

	@Override
	public int vnap$cosmetic() {
		return ((Villager) (Object) this).getEntityData().get(VNAP_COSMETIC);
	}

	@Override
	public void vnap$setCosmetic(int value) {
		((Villager) (Object) this).getEntityData().set(VNAP_COSMETIC, Math.max(0, Math.min(4, value)));
	}

	@Override
	public int vnap$signMessage() {
		return ((Villager) (Object) this).getEntityData().get(VNAP_SIGN_MESSAGE);
	}

	@Override
	public void vnap$setSignMessage(int value) {
		((Villager) (Object) this).getEntityData().set(VNAP_SIGN_MESSAGE, Math.max(-1, Math.min(86, value)));
	}

	@Override
	public int vnap$signType() {
		return ((Villager) (Object) this).getEntityData().get(VNAP_SIGN_TYPE);
	}

	@Override
	public void vnap$setSignType(int value) {
		((Villager) (Object) this).getEntityData().set(VNAP_SIGN_TYPE, Math.max(-1, Math.min(11, value)));
	}
}
