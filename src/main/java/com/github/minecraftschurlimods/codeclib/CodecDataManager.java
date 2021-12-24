package com.github.minecraftschurlimods.codeclib;

import com.github.minecraftschurlimods.simplenetlib.IPacket;
import com.github.minecraftschurlimods.simplenetlib.NetworkHandler;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class CodecDataManager<T> extends SimpleJsonResourceReloadListener implements Map<ResourceLocation, T> {
    private static final BiMap<Integer, CodecDataManager<?>> DATA_MANAGER = HashBiMap.create();
    private static final Gson GSON = new Gson();
    private final String folderName;
    private final Codec<T> codec;
    private final Codec<T> networkCodec;
    private final Validator<Map<ResourceLocation, T>> validator;
    private Map<ResourceLocation, T> data = new HashMap<>();
    protected final Logger logger;

    public CodecDataManager(String folderName, Codec<T> codec, Logger logger) {
        this(folderName, codec, codec, (m, l) -> {}, logger);
    }

    public CodecDataManager(String folderName, Codec<T> codec, Validator<Map<ResourceLocation, T>> validator, Logger logger) {
        this(folderName, codec, codec, validator, logger);
    }

    public CodecDataManager(String folderName, Codec<T> codec, Codec<T> networkCodec, Logger logger) {
        this(folderName, codec, networkCodec, (m, l) -> {}, logger);
    }

    public CodecDataManager(String folderName, Codec<T> codec, Codec<T> networkCodec, Validator<Map<ResourceLocation, T>> validator, Logger logger) {
        super(GSON, folderName);
        this.folderName = folderName;
        this.codec = codec;
        this.networkCodec = networkCodec;
        this.validator = validator;
        this.logger = logger;
        synchronized (DATA_MANAGER) {
            DATA_MANAGER.put(DATA_MANAGER.size(), this);
        }
    }

    @Override
    public int size() {
        return this.data != null ? this.data.size() : 0;
    }

    @Override
    public boolean isEmpty() {
        return this.data != null && this.data.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.data != null && this.data.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.data != null && this.data.containsValue(value);
    }

    @Nullable
    @Override
    public T get(@Nullable Object id) {
        if (id == null) return null;
        if (id instanceof String keyS) {
            id = new ResourceLocation(keyS);
        }
        if (!(id instanceof ResourceLocation)) throw this.logger.throwing(new ClassCastException());
        if (this.data == null) return null;
        return this.data.get(id);
    }

    @SuppressWarnings("ConstantConditions")
    public T getOrThrow(@Nullable Object id) {
        if (this.data == null) throw this.logger.throwing(new IllegalStateException(
                "CodecDataManager(%s) not loaded yet!".formatted(this.folderName)));
        if (!this.containsKey(id) || id == null) throw this.logger.throwing(new NoSuchElementException());
        return this.get(id);
    }

    @SuppressWarnings("ConstantConditions")
    public T getOrDefault(@Nullable Object id, Supplier<T> defaultSupplier) {
        if (this.data == null || !this.containsKey(id) || id == null) return defaultSupplier.get();
        return this.get(id);
    }

    public Optional<T> getOptional(@Nullable Object id) {
        return Optional.ofNullable(this.get(id));
    }

    public Map<ResourceLocation, T> getData() {
        return Collections.unmodifiableMap(this.data);
    }

    @NotNull
    @Override
    public Set<ResourceLocation> keySet() {
        return Collections.unmodifiableSet(this.data.keySet());
    }

    @NotNull
    @Override
    public Collection<T> values() {
        return Collections.unmodifiableCollection(this.data.values());
    }

    @NotNull
    @Override
    public Set<Entry<ResourceLocation, T>> entrySet() {
        return Collections.unmodifiableSet(this.data.entrySet());
    }

    @Override
    protected final void apply(Map<ResourceLocation, JsonElement> dataIn, ResourceManager resourceManager, ProfilerFiller profiler) {
        this.logger.info("Beginning loading of data for data loader: {}", this.folderName);
        profiler.push("data_manager_%s_deserialize".formatted(this.folderName));
        this.data = mapData(dataIn);
        profiler.pop();
        this.logger.info("Data loader for {} loaded {} jsons", this.folderName, this.data.size());
        this.logger.info("Beginning validation of data for data loader: {}", this.folderName);
        profiler.push("data_manager_%s_validate".formatted(this.folderName));
        try {
            this.validator.validate(this.data, logger); // yes it is intentional to pass the mutable data
            this.logger.info("Data loader for {} finished validation of {} entries", this.folderName, this.data.size());
        } catch (ValidationError e) {
            this.logger.error("Data loader for {} failed validation", this.folderName, e);
        }
        profiler.pop();
    }

    protected void receiveSyncedData(Map<ResourceLocation, T> data) {
        this.data = data;
    }

    private Map<ResourceLocation, T> mapData(Map<ResourceLocation, JsonElement> dataIn) {
        Map<ResourceLocation, T> data = new HashMap<>();
        dataIn.forEach((key, jsonElement) -> this.codec.decode(JsonOps.INSTANCE, jsonElement)
                .get()
                .ifLeft(result -> data.put(key, result.getFirst()))
                .ifRight(partial -> this.logger.error("Failed to parse data json for {} due to: {}", key.toString(), partial.message()))
        );
        return data;
    }

    public final CodecDataManager<T> subscribeAsSyncable(NetworkHandler networkHandler) {
        networkHandler.register(SyncPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        MinecraftForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) -> networkHandler.sendToPlayerOrAll(new SyncPacket<>(DATA_MANAGER.inverse().get(this), this.data), event.getPlayer()));
        return this;
    }

    public static final class SyncPacket<T> implements IPacket {
        private final Map<ResourceLocation, T> data;
        private final int index;

        private SyncPacket(int index, Map<ResourceLocation, T> data) {
            this.index = index;
            this.data = data;
        }

        public SyncPacket(FriendlyByteBuf buffer) {
            this.index = buffer.readInt();
            Codec<T> codec = getDataManager().networkCodec;
            this.data = buffer.readMap(FriendlyByteBuf::readResourceLocation, buf -> buf.readWithCodec(codec));
        }

        public void serialize(FriendlyByteBuf buffer) {
            Codec<T> codec = getDataManager().networkCodec;
            buffer.writeInt(this.index);
            buffer.writeMap(this.data, FriendlyByteBuf::writeResourceLocation, (buf, t) -> buf.writeWithCodec(codec, t));
        }

        @Override
        public void handle(NetworkEvent.Context ctx) {
            ctx.enqueueWork(() -> getDataManager().receiveSyncedData(this.data));
        }

        @SuppressWarnings("unchecked")
        private CodecDataManager<T> getDataManager() {
            return (CodecDataManager<T>) CodecDataManager.DATA_MANAGER.get(this.index);
        }
    }

    public static class ValidationError extends Exception {}

    @FunctionalInterface
    public interface Validator<T> {
        void validate(T data, Logger logger) throws ValidationError;
    }

    // region unsupported operations
    @Override
    public T put(ResourceLocation key, T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(@NotNull final Map<? extends ResourceLocation, ? extends T> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    // endregion
}
