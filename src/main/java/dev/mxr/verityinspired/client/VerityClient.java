package dev.mxr.verityinspired.client;

import dev.mxr.verityinspired.VerityEntities;
import dev.mxr.verityinspired.VerityEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class VerityClient {
    private VerityClient() {}

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(VerityEntities.VERITY.get(), VerityRenderer::new);
    }

    private static final class VerityRenderer extends MobRenderer<VerityEntity, ZombieRenderState, HumanoidModel<HumanoidRenderState>> {
        private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("minecraft", "textures/entity/zombie/zombie.png");

        VerityRenderer(EntityRendererProvider.Context context) {
            super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.55F);
        }

        @Override
        public ZombieRenderState createRenderState() {
            return new ZombieRenderState();
        }

        @Override
        public void extractRenderState(VerityEntity entity, ZombieRenderState state, float partialTick) {
            super.extractRenderState(entity, state, partialTick);
        }

        @Override
        public Identifier getTextureLocation(ZombieRenderState state) {
            return TEXTURE;
        }
    }
}