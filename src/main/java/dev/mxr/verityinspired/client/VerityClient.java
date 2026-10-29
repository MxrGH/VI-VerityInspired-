package dev.mxr.verityinspired.client;

import dev.mxr.verityinspired.VerityEntities;
import dev.mxr.verityinspired.VerityEntity;
import dev.mxr.verityinspired.VerityInspired;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = VerityInspired.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class VerityClient {
    private VerityClient() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(VerityEntities.VERITY.get(), VerityRenderer::new);
    }

    private static final class VerityRenderer extends MobRenderer<VerityEntity, HumanoidModel<VerityEntity>> {
        private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("minecraft", "textures/entity/zombie/zombie.png");

        VerityRenderer(EntityRendererProvider.Context context) {
            super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.55F);
        }

        @Override
        public Identifier getTextureLocation(VerityEntity entity) {
            return TEXTURE;
        }
    }
}