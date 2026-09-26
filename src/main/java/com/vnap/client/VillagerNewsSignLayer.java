package com.vnap.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.vnap.VillagerNewsAddonPort;
import com.vnap.entity.VillagerNewsData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import traben.entity_model_features.models.IEMFModel;
import traben.entity_model_features.models.animation.EMFAttachment;

import java.util.function.Consumer;

public final class VillagerNewsSignLayer extends RenderLayer<Villager, VillagerModel<Villager>> {
	private static final ResourceLocation[] BOARD_TEXTURES = {
		minecraft("oak"), minecraft("spruce"), minecraft("birch"), minecraft("jungle"),
		minecraft("acacia"), minecraft("dark_oak"), minecraft("mangrove"), minecraft("cherry"),
		// pale oak signs do NOT exist in 1.21.1 so this slot only keeps the indices stable
		minecraft("oak"), minecraft("bamboo"), minecraft("crimson"), minecraft("warped")
	};
	private static final ResourceLocation TEXT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
		VillagerNewsAddonPort.MOD_ID, "textures/entity/sign_text.png"
	);
	public VillagerNewsSignLayer(RenderLayerParent<Villager, VillagerModel<Villager>> renderer) {
		super(renderer);
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, Villager villager,
			float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
		VillagerNewsData sign = (VillagerNewsData) villager;
		int type = sign.vnap$signType();
		int message = sign.vnap$signMessage();
		if (villager.isInvisible() || villager.isBaby() || type < 0 || type >= BOARD_TEXTURES.length || message < 0 || message >= 87) return;
		poseStack.pushPose();
		Consumer<PoseStack> positioner = getParentModel() instanceof IEMFModel emfModel && emfModel.emf$isEMFModel()
			? emfModel.emf$getEMFRootModel().getPositionerForAttachment(EMFAttachment.Type.VILLAGER)
			: null;
		if (positioner == null) {
			translateToArms(getParentModel(), poseStack);
		} else {
			positioner.accept(poseStack);
		}
		poseStack.translate(0.0F, 5.75F / 16.0F, -1.75F / 16.0F);
		poseStack.mulPose(Axis.XP.rotationDegrees(42.97F));
		PoseStack.Pose pose = poseStack.last();
		drawBoard(pose, buffers.getBuffer(RenderType.entityCutout(BOARD_TEXTURES[type])), packedLight);
		if (!drawTranslatedText(poseStack, buffers, packedLight, message)) {
			drawText(pose, buffers.getBuffer(RenderType.entityCutout(TEXT_TEXTURE)), packedLight, message);
		}
		poseStack.popPose();
	}

	private static void translateToArms(VillagerModel<Villager> model, PoseStack poseStack) {
		ModelPart root = model.root();
		root.translateAndRotate(poseStack);
		root.getChild("arms").translateAndRotate(poseStack);
	}

	private static ResourceLocation minecraft(String wood) {
		return ResourceLocation.withDefaultNamespace("textures/entity/signs/" + wood + ".png");
	}

	// uvs follow the 64x32 vanilla sign entity texture
	private static void drawBoard(PoseStack.Pose pose, VertexConsumer vertices, int light) {
		float left = -0.50625F;
		float right = 0.50625F;
		float top = -0.25625F;
		float bottom = 0.25625F;
		float front = -0.04792F;
		float back = 0.04792F;
		quad(pose, vertices, light, left, bottom, front, right, bottom, front, right, top, front, left, top, front,
			2.0F / 64.0F, 14.0F / 32.0F, 26.0F / 64.0F, 2.0F / 32.0F, 0.0F, 0.0F, -1.0F);
		quad(pose, vertices, light, right, bottom, back, left, bottom, back, left, top, back, right, top, back,
			28.0F / 64.0F, 14.0F / 32.0F, 52.0F / 64.0F, 2.0F / 32.0F, 0.0F, 0.0F, 1.0F);
		quad(pose, vertices, light, left, top, back, right, top, back, right, top, front, left, top, front,
			2.0F / 64.0F, 0.0F, 26.0F / 64.0F, 2.0F / 32.0F, 0.0F, -1.0F, 0.0F);
		quad(pose, vertices, light, left, bottom, front, right, bottom, front, right, bottom, back, left, bottom, back,
			26.0F / 64.0F, 0.0F, 50.0F / 64.0F, 2.0F / 32.0F, 0.0F, 1.0F, 0.0F);
		quad(pose, vertices, light, left, bottom, back, left, bottom, front, left, top, front, left, top, back,
			0.0F, 14.0F / 32.0F, 2.0F / 64.0F, 2.0F / 32.0F, -1.0F, 0.0F, 0.0F);
		quad(pose, vertices, light, right, bottom, front, right, bottom, back, right, top, back, right, top, front,
			26.0F / 64.0F, 14.0F / 32.0F, 28.0F / 64.0F, 2.0F / 32.0F, 1.0F, 0.0F, 0.0F);
	}

	// the text follows the subtitle language and the old atlas is only a fallback
	private static boolean drawTranslatedText(PoseStack poseStack, MultiBufferSource buffers, int light, int message) {
		String text = SubtitleLanguages.sign(message);
		if (text == null || text.isBlank()) return false;
		Font font = Minecraft.getInstance().font;
		String[] lines = text.split("\n");
		int width = 1;
		for (String line : lines) width = Math.max(width, font.width(line));
		int height = lines.length * font.lineHeight - 1;
		float scale = Math.min(1.0F / 96.0F, Math.min(0.94F / width, 0.36F / height));
		poseStack.pushPose();
		poseStack.translate(0.0F, 0.0F, -0.0605F);
		poseStack.scale(scale, scale, scale);
		float y = -height / 2.0F;
		for (String line : lines) {
			font.drawInBatch(line, -font.width(line) / 2.0F, y, 0xFF000000, false, poseStack.last().pose(), buffers,
				Font.DisplayMode.POLYGON_OFFSET, 0, light);
			y += font.lineHeight;
		}
		poseStack.popPose();
		return true;
	}

	private static void drawText(PoseStack.Pose pose, VertexConsumer vertices, int light, int message) {
		float topV = message / 87.0F;
		float bottomV = (message + 1) / 87.0F;
		quad(pose, vertices, light, -0.5F, 0.1875F, -0.06042F, 0.5F, 0.1875F, -0.06042F,
			0.5F, -0.1875F, -0.06042F, -0.5F, -0.1875F, -0.06042F,
			0.0F, bottomV, 1.0F, topV, 0.0F, 0.0F, -1.0F);
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer vertices, int light,
			float x1, float y1, float z1, float x2, float y2, float z2,
			float x3, float y3, float z3, float x4, float y4, float z4,
			float u1, float v1, float u2, float v2, float nx, float ny, float nz) {
		vertex(pose, vertices, light, x1, y1, z1, u1, v1, nx, ny, nz);
		vertex(pose, vertices, light, x2, y2, z2, u2, v1, nx, ny, nz);
		vertex(pose, vertices, light, x3, y3, z3, u2, v2, nx, ny, nz);
		vertex(pose, vertices, light, x4, y4, z4, u1, v2, nx, ny, nz);
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer vertices, int light,
			float x, float y, float z, float u, float v, float nx, float ny, float nz) {
		vertices.addVertex(pose, x, y, z)
			.setColor(-1)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(light)
			.setNormal(pose, nx, ny, nz);
	}
}
