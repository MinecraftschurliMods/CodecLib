package com.github.minecraftschurlimods.codeclib;

import net.minecraft.core.RegistryAccess;
import net.minecraftforge.fml.DistExecutor;

public class RegistryAccessGetter {
    public static RegistryAccess getRegistryAccess() {
        return DistExecutor.safeRunForDist(() -> ClientRegistryAccess::getRegistryAccess, () -> ServerRegistryAccess::getRegistryAccess);
    }
}
