package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import qouteall.imm_ptl.core.portal.LoadingIndicatorEntity;

public class LoadingIndicatorRenderer extends EntityRenderer<LoadingIndicatorEntity> {
    public LoadingIndicatorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
    
    @Override
    public ResourceLocation getTextureLocation(LoadingIndicatorEntity var1) {
        return null;
    }
    
    @Override
    public void render(
        LoadingIndicatorEntity entity_1,
        float float_1,
        float float_2,
        PoseStack matrixStack_1,
        MultiBufferSource vertexConsumerProvider_1,
        int int_1
    ) {
//        String[] splited = entity_1.getText().getString().split("\n");
//        for (int i = 0; i < splited.length; i++) {
//            matrixStack_1.push();
//            matrixStack_1.translate(0, -i * 0.25 - 0.5, 0);
//            this.renderLabelIfPresent(
//                entity_1,
//                new LiteralText(splited[i]),
//                matrixStack_1,
//                vertexConsumerProvider_1,
//                int_1
//            );
//            matrixStack_1.pop();
//        }
    }
}
