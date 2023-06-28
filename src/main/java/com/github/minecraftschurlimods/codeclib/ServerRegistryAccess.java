package com.github.minecraftschurlimods.codeclib;

import net.minecraft.core.RegistryAccess;
import net.minecraftforge.server.ServerLifecycleHooks;

public class ServerRegistryAccess {
    public static RegistryAccess getRegistryAccess() {
        return ServerLifecycleHooks.getCurrentServer().registryAccess();
    }
}
