package com.github.minecraftschurlimods.codeclib;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Abstract base class for a Packet using a codec to encode and decode the data.
 *
 * @param <T> the datatype of the data being sent.
 */
public abstract class CodecPacket<T> implements CustomPacketPayload {
    protected final T data;

    /**
     * Constructor accepting the data to send.
     *
     * @param data the data to send.
     */
    public CodecPacket(T data) {
        this.data = data;
    }

    /**
     * Constructor for deserialization.<br>
     * Subclasses must have this constructor present.
     */
    public CodecPacket(FriendlyByteBuf buf) {
        this.data = buf.readWithCodecTrusted(this.ops(), this.codec());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeWithCodec(this.ops(), this.codec(), this.data);
    }

    /**
     * Implement this method and return the codec to encode and decode the data.
     *
     * @return the codec to encode and decode the data.
     */
    protected abstract Codec<T> codec();

    protected DynamicOps<Tag> ops() {
        return NbtOps.INSTANCE;
    }
}
