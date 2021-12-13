package qouteall.imm_ptl.core.render;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import qouteall.imm_ptl.core.portal.LoadingIndicatorEntity;

public class LoadingIndicatorRenderer extends EntityRenderer<LoadingIndicatorEntity> {
    public LoadingIndicatorRenderer(EntityRendererFactory.Context context) {
        super(context);
    }
    
    @Override
    public Identifier getTexture(LoadingIndicatorEntity var1) {
        return null;
    }
    
    @Override
    public void render(
        LoadingIndicatorEntity entity_1,
        float float_1,
        float float_2,
        MatrixStack matrixStack_1,
        VertexConsumerProvider vertexConsumerProvider_1,
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
