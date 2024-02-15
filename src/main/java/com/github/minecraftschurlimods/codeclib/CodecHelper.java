package com.github.minecraftschurlimods.codeclib;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.ListBuilder;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Contract;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class CodecHelper {
    @Deprecated(forRemoval = true)
    public static final Codec<Ingredient> INGREDIENT = Ingredient.CODEC_NONEMPTY;
    @Deprecated(forRemoval = true)
    public static final Codec<Ingredient> NETWORK_INGREDIENT = Ingredient.CODEC_NONEMPTY;
    @Deprecated(forRemoval = true)
    public static final Codec<Component> COMPONENT = ComponentSerialization.FLAT_CODEC;
    @Deprecated(forRemoval = true)
    public static final Codec<EntityPredicate> ENTITY_PREDICATE = EntityPredicate.CODEC;
    @Deprecated(forRemoval = true)
    public static final Codec<MinMaxBounds.Ints> INT_MIN_MAX_BOUNDS = MinMaxBounds.Ints.CODEC;
    @Deprecated(forRemoval = true)
    public static final Codec<MinMaxBounds.Doubles> DOUBLE_MIN_MAX_BOUNDS = MinMaxBounds.Doubles.CODEC;

    @Deprecated(forRemoval = true)
    public static <T> Codec<T> forRegistry(Supplier<Registry<T>> registrySupplier) {
        return registrySupplier.get().byNameCodec();
    }

    @Contract("_ -> new")
    public static <E extends Enum<E>> Codec<E> forStringEnum(Class<E> clazz) {
        return Codec.STRING.xmap(s -> Enum.valueOf(clazz, s), Enum::name);
    }

    @Contract("_ -> new")
    public static <E extends Enum<E>> Codec<E> forIntEnum(Class<E> clazz) {
        E[] enumConstants = clazz.getEnumConstants();
        int size = enumConstants.length;
        if (size < Byte.MAX_VALUE) {
            return Codec.BYTE.xmap(i -> enumConstants[i], e -> (byte) e.ordinal());
        }
        if (size < Short.MAX_VALUE) {
            return Codec.SHORT.xmap(i -> enumConstants[i], e -> (short) e.ordinal());
        }
        return Codec.INT.xmap(i -> enumConstants[i], Enum::ordinal);
    }

    @Contract("_ -> new")
    public static <E extends Enum<E>> Codec<E> forEnum(Class<E> clazz) {
        return ExtraCodecs.orCompressed(forStringEnum(clazz), forIntEnum(clazz));
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
        public String toString() {
            return "SetCodec[" + elementCodec + ']';
        }
    }
}
