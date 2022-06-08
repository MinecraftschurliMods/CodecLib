package com.github.minecraftschurlimods.codeclib;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;

public interface IDataManager<T> extends Map<ResourceLocation, T>, PreparableReloadListener {
    /**
     * @return Whether this data manager is already loaded.
     */
    boolean isLoaded();

    /**
     * @return The name of the folder that this data manager is stored in.
     */
    String getFolderName();

    /**
     * @return The id of this data manager.
     */
    ResourceLocation id();

    /**
     * Override to wrap, modify or replace the exception to throw.
     */
    <E extends Throwable> E throwing(E throwable);

    /**
     * Retrieve the value with the given id from this data manager.
     *
     * @param id the id of the data to get.
     * @return The data with the given id.
     */
    @Nullable T get(@Nullable ResourceLocation id);

    /**
     * Retrieve the value with the given id from this data manager.
     *
     * @param id the id of the data to get.
     * @return The data with the given id.
     */
    @Nullable
    @Override
    default T get(@Nullable Object id) {
        if (id == null) return null;
        if (id instanceof CharSequence keyS) {
            id = new ResourceLocation(keyS.toString());
        }
        if (!(id instanceof ResourceLocation rl)) throw this.throwing(new ClassCastException());
        return this.get(rl);
    }

    /**
     * Retrieve the value with the given id from this data manager or throw an exception.
     *
     * @param id the id of the data to get.
     * @return The data with the given id.
     */
    default T getOrThrow(@Nullable Object id) {
        if (!isLoaded()) throw throwing(new IllegalStateException("CodecDataManager(%s) not loaded yet!".formatted(this.getFolderName())));
        T t = this.get(id);
        if (t == null) throw throwing(new NoSuchElementException());
        return t;
    }

    /**
     * Retrieve the value with the given id from this data manager or return a default value.
     *
     * @param id the id of the data to get.
     * @param defaultSupplier the default value to return if the data is not found.
     * @return The data with the given id or the default value.
     */
    default T getOrDefault(@Nullable Object id, Supplier<T> defaultSupplier) {
        T t = this.get(id);
        return t == null ? defaultSupplier.get() : t;
    }

    /**
     * Retrieve an {@link Optional} of the value with the given id from this data manager or {@link Optional#empty()}.
     *
     * @param id the id of the data to get.
     * @return The data with the given id or empty.
     */
    default Optional<T> getOptional(@Nullable Object id) {
        return Optional.ofNullable(this.get(id));
    }

    /**
     * Retrieve the value with the given id from this data manager or throw an exception.
     *
     * @param id the id of the data to get.
     * @return The data with the given id.
     */
    default T getOrThrow(@Nullable ResourceLocation id) {
        if (!this.isLoaded()) throw this.throwing(new IllegalStateException("CodecDataManager(%s) not loaded yet!".formatted(this.getFolderName())));
        T t = this.get(id);
        if (t == null) throw this.throwing(new NoSuchElementException());
        return t;
    }

    /**
     * Retrieve the value with the given id from this data manager or return a default value.
     *
     * @param id the id of the data to get.
     * @param defaultSupplier the default value to return if the data is not found.
     * @return The data with the given id or the default value.
     */
    default T getOrDefault(@Nullable ResourceLocation id, Supplier<T> defaultSupplier) {
        T t = this.get(id);
        return t == null ? defaultSupplier.get() : t;
    }

    /**
     * Retrieve an {@link Optional} of the value with the given id from this data manager or {@link Optional#empty()}.
     *
     * @param id the id of the data to get.
     * @return The data with the given id or empty.
     */
    default Optional<T> getOptional(@Nullable ResourceLocation id) {
        return Optional.ofNullable(this.get(id));
    }

    // region unsupported operations

    /**
     * @throws UnsupportedOperationException always.
     */
    @Override
    default T put(ResourceLocation key, T value) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    @Override
    default T remove(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    @Override
    default void putAll(@NotNull final Map<? extends ResourceLocation, ? extends T> m) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    @Override
    default void clear() {
        throw new UnsupportedOperationException();
    }
    // endregion
}
