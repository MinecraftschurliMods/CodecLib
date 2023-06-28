package com.github.minecraftschurlimods.codeclib;

import com.github.minecraftschurlimods.simplenetlib.CodecPacket;
import com.github.minecraftschurlimods.simplenetlib.NetworkHandler;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class CodecDataManager<T> extends SimpleJsonResourceReloadListener implements IDataManager<T> {
    private static final BiMap<ResourceLocation, CodecDataManager<?>> DATA_MANAGER = HashBiMap.create();
    private static final Gson GSON = new Gson();
    private final Codec<T> elementCodec;
    private final Codec<Map<ResourceLocation, T>> networkCodec;
    private final Validator<Map<ResourceLocation, T>> validator;
    private final ResourceLocation id;
    @Nullable
    private Map<ResourceLocation, T> data = new HashMap<>();
    private boolean useRegistryOps = false;
    private boolean isSyncable = false;
    protected final Logger logger;

    public CodecDataManager(ResourceLocation id, Codec<T> elementCodec) {
        this(id, elementCodec, Validator.noop());
    }

    public CodecDataManager(String modId, String folderName, Codec<T> elementCodec) {
        this(new ResourceLocation(modId, folderName), elementCodec);
    }

    public CodecDataManager(ResourceLocation id, Codec<T> elementCodec, Validator<Map<ResourceLocation, T>> validator) {
        this(id, elementCodec, elementCodec, validator);
    }

    public CodecDataManager(String modId, String folderName, Codec<T> elementCodec, Validator<Map<ResourceLocation, T>> validator) {
        this(new ResourceLocation(modId, folderName), elementCodec, elementCodec, validator);
    }

    public CodecDataManager(ResourceLocation id, Codec<T> elementCodec, Codec<T> elementNetworkCodec) {
        this(id, elementCodec, elementNetworkCodec, Validator.noop());
    }

    public CodecDataManager(String modId, String folderName, Codec<T> elementCodec, Codec<T> elementNetworkCodec) {
        this(modId, folderName, elementCodec, elementNetworkCodec, Validator.noop());
    }

    public CodecDataManager(ResourceLocation id, Codec<T> elementCodec, Logger logger) {
        this(id, elementCodec, Validator.noop(), logger);
    }

    public CodecDataManager(String modId, String folderName, Codec<T> elementCodec, Logger logger) {
        this(new ResourceLocation(modId, folderName), elementCodec, logger);
    }

    public CodecDataManager(ResourceLocation id, Codec<T> elementCodec, Codec<T> elementNetworkCodec, Validator<Map<ResourceLocation, T>> validator) {
        this(id, elementCodec, elementNetworkCodec, validator, null);
    }

    public CodecDataManager(String modId, String folderName, Codec<T> elementCodec, Codec<T> elementNetworkCodec, Validator<Map<ResourceLocation, T>> validator) {
        this(new ResourceLocation(modId, folderName), elementCodec, elementNetworkCodec, validator);
    }

    public CodecDataManager(ResourceLocation id, Codec<T> elementCodec, Validator<Map<ResourceLocation, T>> validator, Logger logger) {
        this(id, elementCodec, elementCodec, validator, logger);
    }

    public CodecDataManager(String modId, String folderName, Codec<T> elementCodec, Validator<Map<ResourceLocation, T>> validator, Logger logger) {
        this(new ResourceLocation(modId, folderName), elementCodec, elementCodec, validator, logger);
    }

    public CodecDataManager(ResourceLocation id, Codec<T> elementCodec, Codec<T> elementNetworkCodec, Logger logger) {
        this(id, elementCodec, elementNetworkCodec, Validator.noop(), logger);
    }

    public CodecDataManager(String modId, String folderName, Codec<T> elementCodec, Codec<T> elementNetworkCodec, Logger logger) {
        this(new ResourceLocation(modId, folderName), elementCodec, elementNetworkCodec, logger);
    }

    public CodecDataManager(String modId, String folderName, Codec<T> elementCodec, Codec<T> elementNetworkCodec, Validator<Map<ResourceLocation, T>> validator, @Nullable Logger logger) {
        this(new ResourceLocation(modId, folderName), elementCodec, elementNetworkCodec, validator, logger);
    }

    public CodecDataManager(ResourceLocation id, Codec<T> elementCodec, Codec<T> elementNetworkCodec, Validator<Map<ResourceLocation, T>> validator, @Nullable Logger logger) {
        super(GSON, id.getPath());
        this.id = id;
        this.elementCodec = elementCodec;
        this.networkCodec = Codec.unboundedMap(ResourceLocation.CODEC, elementNetworkCodec);
        this.validator = validator;
        if (logger == null) {
            if (getClass() == CodecDataManager.class) {
                logger = LoggerFactory.getLogger("CodecDataManager(%s)".formatted(id()));
            } else {
                logger = LoggerFactory.getLogger(getClass());
            }
        }
        this.logger = logger;
        synchronized (DATA_MANAGER) {
            DATA_MANAGER.put(this.id, this);
        }
    }

    public final CodecDataManager<T> subscribeAsSyncable(NetworkHandler networkHandler) {
        if (this.isSyncable) return this;
        networkHandler.register(this.id, SyncPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        MinecraftForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) -> {
            if (this.data == null) return;
            networkHandler.sendToPlayerOrAll(new SyncPacket<>(DATA_MANAGER.inverse().get(this), this.data), event.getPlayer());
        });
        this.isSyncable = true;
        return this;
    }

    public CodecDataManager<T> useRegistryOps() {
        this.useRegistryOps = true;
        return this;
    }

    @Override
    public int size() {
        return isLoaded() ? this.data.size() : 0;
    }

    @Override
    public boolean isEmpty() {
        return isLoaded() && this.data.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return isLoaded() && this.data.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return isLoaded() && this.data.containsValue(value);
    }

    @Override
    public final boolean isLoaded() {
        return this.data != null;
    }

    @Override
    public String getFolderName() {
        return this.id().getPath();
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public <E extends Throwable> E throwing(final E e) {
        this.logger.error("", e);
        return e;
    }

    @Nullable
    @Override
    public T get(@Nullable ResourceLocation id) {
        if (id == null) return null;
        if (this.data == null) return null;
        return this.data.get(id);
    }

    @NotNull
    @Override
    public Set<ResourceLocation> keySet() {
        return isLoaded() ? Collections.unmodifiableSet(this.data.keySet()) : Collections.emptySet();
    }

    @NotNull
    @Override
    public Collection<T> values() {
        return isLoaded() ? Collections.unmodifiableCollection(this.data.values()) : Collections.emptyList();
    }

    @NotNull
    @Override
    public Set<Entry<ResourceLocation, T>> entrySet() {
        return isLoaded() ? Collections.unmodifiableSet(this.data.entrySet()) : Collections.emptySet();
    }

    @Override
    protected final void apply(Map<ResourceLocation, JsonElement> dataIn, ResourceManager resourceManager, ProfilerFiller profiler) {
        this.logger.info("Beginning loading of data for data manager: {}", id());
        profiler.push("data_manager_%s_deserialize".formatted(id().toString().replace(':', '_').replace('/', '_')));
        this.data = mapData(dataIn, this.data != null ? this.data : new HashMap<>(), this.elementCodec, this.logger);
        profiler.pop();
        this.logger.info("Data manager for {} loaded {} jsons", id(), this.data.size());
        this.logger.info("Beginning validation of data for data manager: {}", id());
        profiler.push("data_manager_%s_validate".formatted(id().toString().replace(':', '_').replace('/', '_')));
        try {
            this.validator.validate(this.data, logger); // yes it is intentional to pass the mutable data
            this.logger.info("Data manager for {} finished validation of {} entries", id(), this.data.size());
        } catch (Validator.ValidationError e) {
            this.logger.error("Data manager for {} failed validation", id(), e);
        }
        profiler.pop();
    }

    protected void receiveSyncedData(Map<ResourceLocation, T> data) {
        if (this.data == null) {
            this.data = data;
        } else {
            this.data.clear();
            this.data.putAll(data);
        }
    }

    private Map<ResourceLocation, T> mapData(Map<ResourceLocation, JsonElement> dataIn, Map<ResourceLocation, T> data, Codec<T> codec, Logger logger) {
        data.clear();
        dataIn.forEach((key, jsonElement) -> codec.decode(getOps(), jsonElement)
                .get()
                .ifLeft(result -> data.put(key, result.getFirst()))
                .ifRight(partial -> logger.error("Failed to parse data json for {} due to: {}", key.toString(), partial.message()))
        );
        return data;
    }

    private DynamicOps<JsonElement> getOps() {
        return this.useRegistryOps ? RegistryOps.create(JsonOps.INSTANCE, RegistryAccessGetter.getRegistryAccess()) : JsonOps.INSTANCE;
    }

    public static final class SyncPacket<T> extends CodecPacket<Map<ResourceLocation, T>> {
        private SyncPacket(ResourceLocation id, Map<ResourceLocation, T> data) {
            super(id, data);
        }

        public SyncPacket(ResourceLocation id, FriendlyByteBuf buffer) {
            super(id, buffer);
        }

        @Override
        public ResourceLocation id() {
            return this.id;
        }

        @Override
        protected Codec<Map<ResourceLocation, T>> codec() {
            return getDataManager(id()).networkCodec;
        }

        @Override
        public void handle(NetworkEvent.Context ctx) {
            ctx.enqueueWork(() -> getDataManager(id()).receiveSyncedData(this.data));
        }

        @SuppressWarnings("unchecked")
        private CodecDataManager<T> getDataManager(ResourceLocation id) {
            return (CodecDataManager<T>) CodecDataManager.DATA_MANAGER.get(id);
        }
    }

}
