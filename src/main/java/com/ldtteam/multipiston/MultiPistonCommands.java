package com.ldtteam.multipiston;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MultiPistonCommands
{
    private MultiPistonCommands()
    {
    }

    public static void onRegisterCommands(final RegisterCommandsEvent event)
    {
        event.getDispatcher().register(
          Commands.literal(MultiPiston.MOD_ID)
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("moveable")
              .then(Commands.literal("add")
                .then(Commands.argument("block", ResourceLocationArgument.id())
                  .executes(context -> addBlock(context.getSource(), ResourceLocationArgument.getId(context, "block")))))
              .then(Commands.literal("remove")
                .then(Commands.argument("block", ResourceLocationArgument.id())
                  .executes(context -> removeBlock(context.getSource(), ResourceLocationArgument.getId(context, "block")))))
              .then(Commands.literal("list")
                .executes(context -> listBlocks(context.getSource()))))
        );
    }

    private static int addBlock(final CommandSourceStack source, final ResourceLocation blockId)
    {
        if (!BuiltInRegistries.BLOCK.containsKey(blockId))
        {
            source.sendFailure(Component.literal("Unknown block id: " + blockId));
            return 0;
        }

        if (MultiPistonConfig.addMoveableEntityBlock(blockId))
        {
            source.sendSuccess(() -> Component.literal("Added multipiston moveable block: " + blockId), true);
            return 1;
        }

        source.sendFailure(Component.literal("Block already in multipiston allowlist: " + blockId));
        return 0;
    }

    private static int removeBlock(final CommandSourceStack source, final ResourceLocation blockId)
    {
        if (MultiPistonConfig.removeMoveableEntityBlock(blockId))
        {
            source.sendSuccess(() -> Component.literal("Removed multipiston moveable block: " + blockId), true);
            return 1;
        }

        source.sendFailure(Component.literal("Block not found in multipiston allowlist: " + blockId));
        return 0;
    }

    private static int listBlocks(final CommandSourceStack source)
    {
        final List<String> blocks = new ArrayList<>(MultiPistonConfig.getMoveableEntityBlockIds());
        if (blocks.isEmpty())
        {
            source.sendSuccess(() -> Component.literal("No manually added multipiston blocks."), false);
            return 1;
        }

        Collections.sort(blocks);
        source.sendSuccess(() -> Component.literal("Manually added multipiston blocks (" + blocks.size() + "): "
          + String.join(", ", blocks)), false);
        return blocks.size();
    }
}
