package com.github.minecraftschurli.codeclib;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.SerializationTags;
import net.minecraft.tags.Tag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public final class CodecHelper {
    public static final Codec<Ingredient> INGREDIENT = Codec.PASSTHROUGH.xmap(
            dynamic -> Ingredient.fromJson(dynamic.convert(JsonOps.INSTANCE).getValue()),
            ingredient -> new Dynamic<>(JsonOps.INSTANCE, ingredient.toJson()));

    public static <K,V> Codec<Map<K,V>> mapOf(Codec<K> keyCodec, Codec<V> valueCodec) {
        return Codec.compoundList(keyCodec, valueCodec).xmap(CodecHelper::pairListToMap, CodecHelper::mapToPairList);
    }

    public static <T> Codec<Set<T>> setOf(Codec<T> codec) {
        return codec.listOf().xmap((Function<List<T>, Set<T>>) HashSet::new, (Function<Set<T>, List<T>>) ArrayList::new);
    }

    public static <T extends Enum<T>> Codec<T> forStringEnum(Class<T> clazz) {
        return Codec.STRING.xmap(s -> Enum.valueOf(clazz, s), Enum::name);
    }

    public static <T extends Enum<T>> Codec<T> forIntEnum(Class<T> clazz) {
        return Codec.INT.xmap(i -> clazz.getEnumConstants()[i], Enum::ordinal);
    }

    public static <T extends IForgeRegistryEntry<T>> Codec<T> forRegistry(Supplier<IForgeRegistry<T>> registrySupplier) {
        return ResourceLocation.CODEC.xmap(resourceLocation -> registrySupplier.get().getValue(resourceLocation), t -> registrySupplier.get().getKey(t));
    }

    public static <T> Codec<Either<T, Tag<T>>> instanceOrTag(Registry<T> registry) {
        return Codec.STRING.xmap(
                s -> s.startsWith("#")
                        ? Either.right(SerializationTags.getInstance()
                                                        .getOrEmpty(registry.key())
                                                        .getTagOrEmpty(new ResourceLocation(s.substring(1))))
                        : Either.left(registry.get(new ResourceLocation(s))),
                e -> e.map((T block) -> registry.getKey(block).toString(),
                           (Tag<T> tag) -> "#"+SerializationTags.getInstance()
                                                                .getOrEmpty(registry.key())
                                                                .getId(tag)
                                                                .toString()));
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
}
