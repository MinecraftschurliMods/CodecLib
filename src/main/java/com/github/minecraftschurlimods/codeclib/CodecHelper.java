package com.github.minecraftschurlimods.codeclib;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.ListBuilder;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

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

    public static <T extends Enum<T>> Codec<T> forStringEnum(Class<T> clazz) {
        return Codec.STRING.xmap(s -> Enum.valueOf(clazz, s), Enum::name);
    }

    public static <T extends Enum<T>> Codec<T> forIntEnum(Class<T> clazz) {
        return Codec.INT.xmap(i -> clazz.getEnumConstants()[i], Enum::ordinal);
    }

    public static <T> Codec<T> forRegistry(Supplier<IForgeRegistry<T>> registrySupplier) {
        return ExtraCodecs.lazyInitializedCodec(() -> registrySupplier.get().getCodec());
    }

    public static <T> Codec<Set<T>> setOf(Codec<T> codec) {
        return new SetCodec<>(codec);
    }

    private record SetCodec<T>(Codec<T> elementCodec) implements Codec<Set<T>> {

        @Override
        public <T1> DataResult<Pair<Set<T>, T1>> decode(DynamicOps<T1> ops, T1 input) {
            return ops.getList(input).setLifecycle(Lifecycle.stable()).flatMap(stream -> {
                final ImmutableSet.Builder<T> read = ImmutableSet.builder();
                final Stream.Builder<T1> failed = Stream.builder();
                final AtomicReference<DataResult<Unit>> result = new AtomicReference<>(DataResult.success(Unit.INSTANCE, Lifecycle.stable()));

                stream.accept(t -> {
                    final DataResult<Pair<T, T1>> element = elementCodec.decode(ops, t);
                    element.error().ifPresent(e -> failed.add(t));
                    result.set(result.get().apply2stable((r, v) -> {
                        read.add(v.getFirst());
                        return r;
                    }, element));
                });

                final Pair<Set<T>, T1> pair = Pair.of(read.build(), ops.createList(failed.build()));
                return result.get().map(unit -> pair).setPartial(pair);
            });
        }

        @Override
        public <T1> DataResult<T1> encode(Set<T> input, DynamicOps<T1> ops, T1 prefix) {
            ListBuilder<T1> builder = ops.listBuilder();
            for (T element : input) {
                builder.add(elementCodec.encodeStart(ops, element));
            }
            return builder.build(prefix);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            return Objects.equals(elementCodec, ((SetCodec<?>) o).elementCodec);
        }

        @Override
        public int hashCode() {
            return Objects.hash(elementCodec);
        }

        @Override
        public String toString() {
            return "SetCodec[" + elementCodec + ']';
        }
    }
}
