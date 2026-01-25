package nl.trifox.foxprison.commands.admin.mine.requirements;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import nl.trifox.foxprison.config.mines.MineRequirementsDefinition;
import nl.trifox.foxprison.service.MineService;
import nl.trifox.foxprison.service.RankService;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class MineSetRankRequirement extends AbstractAsyncCommand {

    private final MineService mineService;

    private final RequiredArg<String> mineId;
    private final RequiredArg<String> requirementValue;

    public MineSetRankRequirement(MineService mineService, RankService rankService) {
        super("setRequiredRank", "Sets the allowed ranks for a mine");

        this.mineService = mineService;

        this.mineId = this.withRequiredArg("mineId", "the mine you want to edit", ArgTypes.STRING);
        this.requirementValue = this.withRequiredArg("ranks", "comma separated ranks: a,b,c", ArgTypes.STRING);
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext ctx) {

        var mineOpt = mineService.getMine(mineId.get(ctx));
        if (mineOpt.isEmpty()) {
            ctx.sender().sendMessage(Message.raw("Mine does not exist."));
            return CompletableFuture.completedFuture(null);
        }

        String raw = requirementValue.get(ctx);

        if (raw.equalsIgnoreCase("none")) {
            MineRequirementsDefinition req = mineOpt.get().getRequirements();
            if (req == null) req = new MineRequirementsDefinition();
            req.allowedRanks = new String[0];
            mineOpt.get().setRequirements(req);

            ctx.sender().sendMessage(Message.raw("Cleared required ranks for mine '" + mineOpt.get().getId() + "'."));
            return CompletableFuture.completedFuture(null);
        }

        String[] ranks = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toLowerCase)
                .toArray(String[]::new);

        if (ranks.length == 0) {
            ctx.sender().sendMessage(Message.raw("Usage: setrequiredrank <mineId> a,b,c,d (or 'none' to clear)"));
            return CompletableFuture.completedFuture(null);
        }

        MineRequirementsDefinition req = mineOpt.get().getRequirements();
        if (req == null) req = new MineRequirementsDefinition();
        req.allowedRanks = ranks;

        mineOpt.get().setRequirements(req);

        ctx.sender().sendMessage(Message.raw(
                "Updated required ranks for mine '" + mineOpt.get().getId() + "': " + String.join(", ", ranks)
        ));

        return CompletableFuture.completedFuture(null);
    }
}
