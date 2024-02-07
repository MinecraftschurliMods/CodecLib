package com.github.minecraftschurlimods.codeclib;

import net.minecraft.core.RegistryAccess;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

public class RegistryAccessGetter {
    @Nullable
    public static RegistryAccess getRegistryAccess() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return ClientRegistryAccess.getRegistryAccess();
        } else {
            return ServerLifecycleHooks.getCurrentServer().registryAccess();
        }
    }
}
