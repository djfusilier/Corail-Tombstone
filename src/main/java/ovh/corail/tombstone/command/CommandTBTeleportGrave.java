package ovh.corail.tombstone.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.dimension.DimensionType;
import ovh.corail.tombstone.helper.DeathHandler;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.Location;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.registry.ModItems;
import ovh.corail.tombstone.tileentity.TileEntityGrave;

public class CommandTBTeleportGrave extends TombstoneCommand {

    public CommandTBTeleportGrave(CommandDispatcher<CommandSource> commandDispatcher) {
        super(commandDispatcher);
    }

    @Override
    public String getName() {
        return "tbteleportgrave";
    }

    @Override
    LiteralArgumentBuilder<CommandSource> getBuilder(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(c -> teleportGrave(c.getSource(), c.getSource().asPlayer(), c.getSource().asPlayer()));
        builder.then(Commands.argument(PLAYER_PARAM, EntityArgument.player())
                .executes(c -> teleportGrave(c.getSource(), c.getSource().asPlayer(), EntityArgument.getPlayer(c, PLAYER_PARAM)))
                .then(Commands.argument(TARGET_PARAM, EntityArgument.player())
                        .executes(c -> teleportGrave(c.getSource(), EntityArgument.getPlayer(c, PLAYER_PARAM), EntityArgument.getPlayer(c, TARGET_PARAM)))
                )
        );
        return builder;
    }

    private int teleportGrave(CommandSource sender, ServerPlayerEntity player, ServerPlayerEntity target) {
        checkAlive(player);
        checkNotSpectator(player);

        DeathHandler deathHandler = DeathHandler.INSTANCE;
        Location lastGrave = deathHandler.getLastGrave(target.getGameProfile().getName());
        if (!lastGrave.isOrigin()) {
            DimensionType dimensionType = getOrThrowDimensionType(lastGrave.dim);
            if (!(sender.getServer().getWorld(dimensionType).getTileEntity(lastGrave.getPos()) instanceof TileEntityGrave)) {
                deathHandler.removeGrave(lastGrave);
                lastGrave = Location.ORIGIN;
            }
        }

        // check for grave's key on the target if no grave is found
        if (lastGrave.isOrigin()) {
            lastGrave = ModItems.grave_key.getTombPos(ModItems.grave_key.findFirstKeyInInventory(target));
            if (lastGrave.isOrigin()) {
                throw LangKey.MESSAGE_NO_GRAVE.asCommandException();
            }
        }

        DimensionType dimensionType = getOrThrowDimensionType(lastGrave.dim);
        checkValidPos(sender.getServer().getWorld(dimensionType), lastGrave.getPos());
        Entity newEntity = Helper.teleportToGrave(player, lastGrave);
        if (EntityHelper.isValidPlayer(newEntity)) {
            newEntity.sendMessage(LangKey.MESSAGE_TELEPORT_SUCCESS.getTranslationWithStyle(StyleType.MESSAGE_SPELL));
        }
        sendMessage(sender, LangKey.MESSAGE_TELEPORT_TARGET_TO_LOCATION.getTranslation(newEntity.getName(), LangKey.MESSAGE_HERE.getTranslation(), lastGrave.x, lastGrave.y, lastGrave.z, lastGrave.dim), false);
        return 1;
    }
}
