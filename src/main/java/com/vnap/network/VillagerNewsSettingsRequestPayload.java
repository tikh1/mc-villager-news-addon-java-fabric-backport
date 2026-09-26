package com.vnap.network;

import com.vnap.VillagerNewsAddonPort;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// asks the server to send the settings again when the settings screen opens
public record VillagerNewsSettingsRequestPayload() implements CustomPacketPayload {
	public static final VillagerNewsSettingsRequestPayload INSTANCE = new VillagerNewsSettingsRequestPayload();
	public static final Type<VillagerNewsSettingsRequestPayload> TYPE = new Type<>(VillagerNewsAddonPort.id("settings_request"));
	public static final StreamCodec<RegistryFriendlyByteBuf, VillagerNewsSettingsRequestPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
