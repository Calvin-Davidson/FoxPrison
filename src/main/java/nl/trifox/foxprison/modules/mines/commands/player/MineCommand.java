package nl.trifox.foxprison.modules.mines.commands.player;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.modules.mines.MineService;
import nl.trifox.foxprison.modules.mines.commands.admin.*;
import nl.trifox.foxprison.modules.mines.commands.admin.autoreset.MineAutoResetCommands;
import nl.trifox.foxprison.modules.mines.commands.admin.region.MineRegionCommand;
import nl.trifox.foxprison.modules.mines.commands.admin.requirements.MineRequirementCommands;
import nl.trifox.foxprison.modules.ranks.RankService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.concurrent.CompletableFuture;

public class MineCommand extends AbstractAsyncPlayerCommand {

    private final MineService service;
    private final OptionalArg<String> mineArg;

    public MineCommand(MineService mineService, RankService rankService
    ) {
        super("mine", "Teleport to your mine (or a mine by id)");
        this.service = mineService;
        this.mineArg = withOptionalArg("mine", "Mine id", ArgTypes.STRING);
        requirePermission("foxprison.mines.command.mine");

        addSubCommand(new MineRegionCommand(mineService));
        addSubCommand(new MineCreateCommand(mineService));
        addSubCommand(new MineDeleteCommand(mineService));
        addSubCommand(new MineResetCommand(mineService));
        addSubCommand(new MineSetBlockPatternCommand(mineService));
        addSubCommand(new MineTpCommand(mineService));
        addSubCommand(new MineListCommand(mineService));
        addSubCommand(new MineRequirementCommands(mineService, rankService));
        addSubCommand(new MineAutoResetCommands(mineService));
        addSubCommand(new MineSetSpawn(mineService));
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
        String mineId = mineArg.provided(commandContext) ? mineArg.get(commandContext) : null;
        service.mine(playerRef, mineId);
        return CompletableFuture.completedFuture(null);
    }
}
