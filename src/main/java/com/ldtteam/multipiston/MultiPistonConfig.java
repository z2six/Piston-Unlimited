package com.ldtteam.multipiston;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class MultiPistonConfig
{
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> MOVEABLE_ENTITY_BLOCKS;

    private static volatile Set<ResourceLocation> allowedEntityBlocks = Collections.emptySet();

    static
    {
        final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("multipiston");
        MOVEABLE_ENTITY_BLOCKS = builder
            .comment("Additional block ids (e.g. \"minecraft:barrel\") the multipiston may move even if normally blocked.")
            .defineList("moveableEntityBlocks", List.of(), MultiPistonConfig::isValidBlockId);
        builder.pop();
        SPEC = builder.build();
    }

    private MultiPistonConfig()
    {
    }

    public static void register(final ModContainer container, final IEventBus modBus)
    {
        container.registerConfig(ModConfig.Type.SERVER, SPEC);
        modBus.addListener(MultiPistonConfig::onLoad);
        modBus.addListener(MultiPistonConfig::onReload);
    }

    public static boolean isAllowedBlock(final ResourceLocation blockId)
    {
        return allowedEntityBlocks.contains(blockId);
    }

    public static List<String> getMoveableEntityBlockIds()
    {
        final List<String> sanitized = sanitizeMoveableEntityBlocks(MOVEABLE_ENTITY_BLOCKS.get());
        return Collections.unmodifiableList(sanitized);
    }

    public static boolean addMoveableEntityBlock(final ResourceLocation blockId)
    {
        final String entry = blockId.toString();
        final List<String> updated = sanitizeMoveableEntityBlocks(MOVEABLE_ENTITY_BLOCKS.get());
        if (updated.contains(entry))
        {
            return false;
        }
        updated.add(entry);
        setMoveableEntityBlocks(updated);
        return true;
    }

    public static boolean removeMoveableEntityBlock(final ResourceLocation blockId)
    {
        final String entry = blockId.toString();
        final List<String> updated = sanitizeMoveableEntityBlocks(MOVEABLE_ENTITY_BLOCKS.get());
        final boolean removed = updated.removeIf(value -> value.equals(entry));
        if (removed)
        {
            setMoveableEntityBlocks(updated);
        }
        return removed;
    }

    private static void onLoad(final ModConfigEvent.Loading event)
    {
        if (isOurServerConfig(event.getConfig()))
        {
            bake();
        }
    }

    private static void onReload(final ModConfigEvent.Reloading event)
    {
        if (isOurServerConfig(event.getConfig()))
        {
            bake();
        }
    }

    private static void bake()
    {
        final Set<ResourceLocation> updated = new HashSet<>();
        for (final String blockId : MOVEABLE_ENTITY_BLOCKS.get())
        {
            if (blockId == null || blockId.isBlank())
            {
                continue;
            }
            try
            {
                updated.add(ResourceLocation.parse(blockId.trim()));
            }
            catch (final RuntimeException ex)
            {
                MultiPiston.LOGGER.warn("Invalid multipiston moveableEntityBlocks entry: {}", blockId, ex);
            }
        }
        allowedEntityBlocks = Collections.unmodifiableSet(updated);
    }

    private static boolean isValidBlockId(final Object value)
    {
        if (!(value instanceof String))
        {
            return false;
        }
        try
        {
            ResourceLocation.parse(((String) value).trim());
            return true;
        }
        catch (final RuntimeException ex)
        {
            return false;
        }
    }

    private static List<String> sanitizeMoveableEntityBlocks(final List<? extends String> values)
    {
        final List<String> sanitized = new ArrayList<>();
        for (final String value : values)
        {
            if (value == null || value.isBlank())
            {
                continue;
            }
            sanitized.add(value.trim());
        }
        return sanitized;
    }

    private static void setMoveableEntityBlocks(final List<String> values)
    {
        final List<String> unique = new ArrayList<>(new LinkedHashSet<>(values));
        MOVEABLE_ENTITY_BLOCKS.set(new ArrayList<>(unique));
        bake();
        save();
    }

    private static void save()
    {
        if (SPEC.isLoaded())
        {
            SPEC.save();
        }
    }

    private static boolean isOurServerConfig(final ModConfig config)
    {
        return config.getType() == ModConfig.Type.SERVER && MultiPiston.MOD_ID.equals(config.getModId());
    }
}
