package net.p4pingvin4ik.pepelandbadges.util;

/**
 * Marks render states that went through the vanilla {@code EntityRenderer.extractRenderState} pass.
 * States built by culling mods (EntityCulling) skip that pass and stay unmarked.
 */
public interface NameplateStateAccess {

    boolean pepeland$isVanillaExtracted();

    void pepeland$setVanillaExtracted(boolean vanillaExtracted);
}
