package io.github.apace100.calio.codec;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.BaseMapCodec;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 *  Calio's implementation of {@link com.mojang.serialization.codecs.UnboundedMapCodec} with strict key/value handling.
 */
public record StrictUnboundedMapCodec<K, V>(StrictCodec<K> keyCodec, StrictCodec<V> elementCodec) implements BaseMapCodec<K, V>, StrictCodec<Map<K, V>> {

    @Override
    public <T> Pair<Map<K, V>, T> strictDecode(DynamicOps<T> ops, T input) {

        Consumer<BiConsumer<T, T>> inputEntries = ops.getMapEntries(input)
            .setLifecycle(Lifecycle.stable())
            .getOrThrow();

        Object2ObjectMap<K, V> resultMap = new Object2ObjectArrayMap<>();
        inputEntries.accept((key, element) -> {

            K decodedKey = keyCodec().strictParse(ops, key);
            V decodedElement = elementCodec().strictParse(ops, element);

            if (resultMap.putIfAbsent(decodedKey, decodedElement) != null) {
                throw new IllegalArgumentException("Duplicate field \"" + key + "\"");
            }

        });

        return Pair.of(ImmutableMap.copyOf(resultMap), input);

    }

    @Override
    public <T> T strictEncode(Map<K, V> input, DynamicOps<T> ops, T prefix) {

        RecordBuilder<T> builder = ops.mapBuilder();
        input.forEach((key, element) -> {

            String encodedKey = ops.getStringValue(keyCodec().strictEncodeStart(ops, key)).getOrThrow();
            T encodedElement = elementCodec().strictEncodeStart(ops, element);

            builder.add(encodedKey, encodedElement);

        });

        return builder.build(prefix).getOrThrow();

    }

}
