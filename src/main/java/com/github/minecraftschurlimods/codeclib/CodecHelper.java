package com.github.minecraftschurlimods.codeclib;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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


    public static <K,V> Codec<Map<K,V>> mapOf(Codec<K> keyCodec, Codec<V> valueCodec) {
        return Codec.compoundList(keyCodec, valueCodec).xmap(CodecHelper::pairListToMap, CodecHelper::mapToPairList);
    }

    public static <T> Codec<Set<T>> setOf(Codec<T> codec) {
        return codec.listOf().xmap(setFromList(), listFromSet());
    }

    public static <T extends Enum<T>> Codec<T> forStringEnum(Class<T> clazz) {
        return Codec.STRING.xmap(s -> Enum.valueOf(clazz, s), Enum::name);
    }

    public static <T extends Enum<T>> Codec<T> forIntEnum(Class<T> clazz) {
        return Codec.INT.xmap(i -> clazz.getEnumConstants()[i], Enum::ordinal);
    }

    public static <T extends IForgeRegistryEntry<T>> Codec<T> forRegistry(Supplier<IForgeRegistry<T>> registrySupplier) {
        return ResourceLocation.CODEC.xmap(resourceLocation -> registrySupplier.get().getValue(resourceLocation),
                                           t -> registrySupplier.get().getKey(t));
    }

    private static <K, V> Map<K, V> pairListToMap(List<Pair<K, V>> pairs) {
        return pairs.stream().collect(Pair.toMap());
    }

    private static <K, V> List<Pair<K, V>> mapToPairList(Map<K, V> map) {
        return map.entrySet().stream().map(CodecHelper::entryToPair).toList();
    }

    private static <K, V> Pair<K, V> entryToPair(Map.Entry<K, V> e) {
        return Pair.of(e.getKey(), e.getValue());
    }

    public static <T> Function<List<T>, Set<T>> setFromList() {
        return HashSet::new;
    }

    public static <T> Function<Set<T>, List<T>> listFromSet() {
        return ArrayList::new;
    }
}
