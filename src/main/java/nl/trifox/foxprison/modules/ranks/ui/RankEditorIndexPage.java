package nl.trifox.foxprison.modules.ranks.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import nl.trifox.foxprison.FoxPrisonPlugin;
import nl.trifox.foxprison.modules.ranks.RankService;
import nl.trifox.foxprison.modules.ranks.config.RankDefinition;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

/**
 * Index page for the rank editor — lists all ranks with edit/delete buttons
 * and a create button.
 */
public class RankEditorIndexPage extends InteractiveCustomUIPage<RankEditorIndexPage.IndexEventData> {

    private final FoxPrisonPlugin plugin;
    private final RankService rankService;
    private final Player player;

    public RankEditorIndexPage(PlayerRef playerRef, Player player, FoxPrisonPlugin plugin, RankService rankService) {
        super(playerRef, CustomPageLifetime.CanDismiss, IndexEventData.CODEC);
        this.player = player;
        this.plugin = plugin;
        this.rankService = rankService;
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, UICommandBuilder cmd, UIEventBuilder events,
                      @NonNullDecl Store<EntityStore> store) {

        cmd.append("Pages/FoxPrison_RankEditorIndex.ui");

        // Close button
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#CloseButton",
                EventData.of(IndexEventData.KEY_ACTION, "close"),
                false);

        // Create button
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#CreateButton",
                EventData.of(IndexEventData.KEY_ACTION, "create"),
                false);

        RankDefinition[] allRanks = rankService.getAllRanks();

        cmd.set("#TotalRanksLabel.Text", "Total: " + allRanks.length + " Ranks");
        cmd.clear("#IndexCards");

        for (int i = 0; i < allRanks.length; i++) {
            RankDefinition rank = allRanks[i];

            cmd.append("#IndexCards", "Pages/FoxPrison_RankEditorEntry.ui");

            String prefix = "#IndexCards[" + i + "] ";
            cmd.set(prefix + "#RankName.Text", rank.getDisplayName());

            // Build info line
            double moneyCost = rank.getCosts().getCurrencyCost("money");
            String info = "ID: " + rank.getId()
                    + "  |  #" + (i + 1)
                    + "  |  Cost: " + moneyCost;
            cmd.set(prefix + "#RankInfo.Text", info);

            events.addEventBinding(CustomUIEventBindingType.Activating,
                    prefix + "#EditButton",
                    EventData.of(IndexEventData.KEY_ACTION, "edit")
                            .append(IndexEventData.KEY_RANK, rank.getId()),
                    false);

            events.addEventBinding(CustomUIEventBindingType.Activating,
                    prefix + "#DeleteButton",
                    EventData.of(IndexEventData.KEY_ACTION, "delete")
                            .append(IndexEventData.KEY_RANK, rank.getId()),
                    false);
        }
    }

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store,
                                IndexEventData eventData) {
        String action = eventData.getAction();
        if (action == null) return;

        String rankId = eventData.getRank();

        switch (action) {
            case "close" -> player.getPageManager().setPage(ref, store, Page.None);
            case "edit" -> handleEdit(ref, store, rankId);
            case "delete" -> handleDelete(ref, store, rankId);
            case "create" -> handleCreate(ref, store);
        }
    }

    private void handleEdit(Ref<EntityStore> ref, Store<EntityStore> store, String rankId) {
        var rankOpt = rankService.getRank(rankId);
        if (rankOpt.isEmpty()) {
            player.sendMessage(Message.raw("Rank not found: " + rankId).color("#FF0000"));
            return;
        }

        var editPage = new RankEditPage(playerRef, player, plugin, rankService, rankOpt.get());
        player.getPageManager().openCustomPage(ref, store, editPage);
    }

    private void handleDelete(Ref<EntityStore> ref, Store<EntityStore> store, String rankId) {
        try {
            boolean ok = rankService.deleteRank(rankId).join();
            if (ok) {
                player.sendMessage(Message.raw("Rank deleted: " + rankId).color("#00FF00"));
                // Rebuild the index page to reflect the change
                var indexPage = new RankEditorIndexPage(playerRef, player, plugin, rankService);
                player.getPageManager().openCustomPage(ref, store, indexPage);
            } else {
                player.sendMessage(Message.raw("Failed to delete rank.").color("#FF0000"));
            }
        } catch (Exception e) {
            player.sendMessage(Message.raw("Failed to delete rank.").color("#FF0000"));
        }
    }

    private void handleCreate(Ref<EntityStore> ref, Store<EntityStore> store) {
        // Create a new rank with a generated ID
        String newId = "new_rank_" + System.currentTimeMillis();
        try {
            boolean ok = rankService.createRank(newId, "New Rank", 0.0).join();
            if (ok) {
                player.sendMessage(Message.raw("Rank created! Opening editor...").color("#00FF00"));
                var rankOpt = rankService.getRank(newId);
                if (rankOpt.isPresent()) {
                    var editPage = new RankEditPage(playerRef, player, plugin, rankService, rankOpt.get());
                    player.getPageManager().openCustomPage(ref, store, editPage);
                }
            } else {
                player.sendMessage(Message.raw("Failed to create rank.").color("#FF0000"));
            }
        } catch (Exception e) {
            player.sendMessage(Message.raw("Failed to create rank.").color("#FF0000"));
        }
    }

    @Override
    public void onDismiss(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store) {
    }

    // ─── Event Data ────────────────────────────────────────────────
    public static class IndexEventData {
        public static final String KEY_ACTION = "Action";
        public static final String KEY_RANK = "Rank";

        public static final BuilderCodec<IndexEventData> CODEC = BuilderCodec
                .builder(IndexEventData.class, IndexEventData::new)
                .append(new KeyedCodec<>(KEY_ACTION, Codec.STRING),
                        IndexEventData::setAction, IndexEventData::getAction)
                .add()
                .append(new KeyedCodec<>(KEY_RANK, Codec.STRING),
                        IndexEventData::setRank, IndexEventData::getRank)
                .add()
                .build();

        private String action;
        private String rank;

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getRank() { return rank; }
        public void setRank(String rank) { this.rank = rank; }
    }
}



