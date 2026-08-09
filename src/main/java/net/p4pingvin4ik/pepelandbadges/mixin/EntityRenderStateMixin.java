package net.p4pingvin4ik.pepelandbadges.mixin;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.p4pingvin4ik.pepelandbadges.util.NameplateStateAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements NameplateStateAccess {

    @Unique
    private boolean pepeland$vanillaExtracted;

    @Override
    public boolean pepeland$isVanillaExtracted() {
        return pepeland$vanillaExtracted;
    }

    @Override
    public void pepeland$setVanillaExtracted(boolean vanillaExtracted) {
        this.pepeland$vanillaExtracted = vanillaExtracted;
    }
}
