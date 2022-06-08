package com.github.minecraftschurlimods.codeclib;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public final class CodecHelper {
    public static final Codec<Ingredient> INGREDIENT = Codec.PASSTHROUGH.xmap(
            dynamic -> Ingredient.fromJson(dynamic.convert(JsonOps.INSTANCE).getValue()),
            ingredient -> new Dynamic<>(JsonOps.INSTANCE, ingredient.toJson()));
    public static final Codec<Ingredient> NETWORK_INGREDIENT = ItemStack.CODEC.listOf().xmap(
            itemStacks -> Ingredient.fromValues(itemStacks.stream().map(Ingredient.ItemValue::new)),
            ingredient -> Arrays.asList(ingredient.getItems()));
    public static final Codec<Component> COMPONENT = Codec.PASSTHROUGH.xmap(
            dynamic -> Component.Serializer.fromJson(dynamic.convert(JsonOps.INSTANCE).getValue()),
            component -> new Dynamic<>(JsonOps.INSTANCE, Component.Serializer.toJsonTree(component)));
    public static final Codec<EntityPredicate> ENTITY_PREDICATE = Codec.PASSTHROUGH.xmap(
            dynamic -> EntityPredicate.fromJson(dynamic.convert(JsonOps.INSTANCE).getValue()),
            entityPredicate -> new Dynamic<>(JsonOps.INSTANCE, sanitizeJson(entityPredicate.serializeToJson())));
    public static final Codec<MinMaxBounds.Ints> INT_MIN_MAX_BOUNDS = Codec.PASSTHROUGH.xmap(
            dynamic -> MinMaxBounds.Ints.fromJson(dynamic.convert(JsonOps.INSTANCE).getValue()),
            minMaxBounds -> new Dynamic<>(JsonOps.INSTANCE, minMaxBounds.serializeToJson()));
    public static final Codec<MinMaxBounds.Doubles> DOUBLE_MIN_MAX_BOUNDS = Codec.PASSTHROUGH.xmap(
            dynamic -> MinMaxBounds.Doubles.fromJson(dynamic.convert(JsonOps.INSTANCE).getValue()),
            minMaxBounds -> new Dynamic<>(JsonOps.INSTANCE, minMaxBounds.serializeToJson()));

    private static JsonElement sanitizeJson(final JsonElement json) {
        if (json instanceof JsonObject object) {
            object.entrySet().removeIf(entry -> entry.getValue() instanceof JsonNull);
        }
        return json;
    }

    public static <T> Codec<Set<T>> setOf(Codec<T> codec) {
        return codec.listOf().xmap(HashSet::new, ArrayList::new);
    }

    public static <T extends Enum<T>> Codec<T> forStringEnum(Class<T> clazz) {
        return Codec.STRING.xmap(s -> Enum.valueOf(clazz, s), Enum::name);
    }

    public static <T extends Enum<T>> Codec<T> forIntEnum(Class<T> clazz) {
        return Codec.INT.xmap(i -> clazz.getEnumConstants()[i], Enum::ordinal);
    }

    public static <T> Codec<T> forRegistry(Supplier<IForgeRegistry<T>> registrySupplier) {
        return ExtraCodecs.lazyInitializedCodec(() -> registrySupplier.get().getCodec());
    }
}
