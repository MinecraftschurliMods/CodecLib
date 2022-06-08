package com.github.minecraftschurlimods.codeclib;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.Map;

@FunctionalInterface
public interface Validator<T> {
    void validate(T data, Logger logger) throws ValidationError;

    static <T> Validator<Map<ResourceLocation, T>> noop() {
        return (m, l) -> {};
    }

    class ValidationError extends Exception {}
}
