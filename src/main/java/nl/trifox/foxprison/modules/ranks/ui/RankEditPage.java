package nl.trifox.foxprison.modules.ranks.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
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
 * Detailed rank editing page — exposes all configurable fields for a single
 * rank in a scrollable form with a Save button.
 */
public class RankEditPage extends InteractiveCustomUIPage<RankEditPage.DetailEventData> {

    private final FoxPrisonPlugin plugin;
    private final Player player;
    private final RankService rankService;
    private final RankDefinition rank;

    public RankEditPage(PlayerRef playerRef, Player player, FoxPrisonPlugin plugin,
                        RankService rankService, RankDefinition rank) {
        super(playerRef, CustomPageLifetime.CanDismiss, DetailEventData.CODEC);
        this.player = player;
        this.plugin = plugin;
        this.rankService = rankService;
        this.rank = rank;
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, UICommandBuilder cmd, UIEventBuilder events,
                      @NonNullDecl Store<EntityStore> store) {

        cmd.append("Pages/FoxPrison_RankEditPage.ui");

        // Back → index
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BackButton",
                EventData.of(DetailEventData.KEY_ACTION, "back"),
                false);

        // ── Populate current values ──────────────────────────────
        cmd.set("#RankNameLabel.Text", rank.getDisplayName());
        cmd.set("#RankIdLabel.Text", "ID: " + rank.getId());

        int position = rankService.indexOfRank(rank.getId()) + 1;
        cmd.set("#RankPositionLabel.Text", "Position: #" + position + " of " + rankService.getAllRanks().length);

        cmd.set("#DisplayNameField.Value", rank.getDisplayName());

        double moneyCost = rank.getCosts().getCurrencyCost("money");
        cmd.set("#MoneyCostField.Value", String.valueOf(moneyCost));

        // ── Event bindings ───────────────────────────────────────

        // Save — reads all text fields at once
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#SaveButton",
                EventData.of(DetailEventData.KEY_ACTION, "save")
                        .append(DetailEventData.KEY_DISPLAY_NAME, "#DisplayNameField.Value")
                        .append(DetailEventData.KEY_MONEY_COST, "#MoneyCostField.Value"),
                false);

        // Delete
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#DeleteButton",
                EventData.of(DetailEventData.KEY_ACTION, "delete"),
                false);
    }

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store,
                                DetailEventData eventData) {
        String action = eventData.getAction();
        if (action == null) return;

        switch (action) {
            case "back" -> {
                var indexPage = new RankEditorIndexPage(playerRef, player, plugin, rankService);
                player.getPageManager().openCustomPage(ref, store, indexPage);
            }
            case "save" -> handleSave(eventData, ref, store);
            case "delete" -> handleDelete(ref, store);
        }
    }

    private void handleSave(DetailEventData data, Ref<EntityStore> ref, Store<EntityStore> store) {
        // ── Display name ─────────────────────────────────────────
        String newName = data.getDisplayName();
        if (newName == null || newName.trim().isEmpty()) {
            player.sendMessage(Message.raw("Display name cannot be empty!").color("#FF6600"));
            return;
        }

        // ── Money cost ───────────────────────────────────────────
        double moneyCost;
        try {
            moneyCost = Double.parseDouble(data.getMoneyCost().trim());
        } catch (NumberFormatException | NullPointerException e) {
            player.sendMessage(Message.raw("Money cost must be a valid number!").color("#FF0000"));
            return;
        }
        if (moneyCost < 0) {
            player.sendMessage(Message.raw("Money cost cannot be negative!").color("#FF6600"));
            return;
        }

        // ── Persist everything ───────────────────────────────────
        try {
            rankService.setRankDisplayName(rank.getId(), newName.trim()).join();
            rankService.setRankCurrencyCost(rank.getId(), "money", moneyCost).join();

            player.sendMessage(Message.raw("Rank saved!").color("#00FF00"));
            // Go back to index
            var indexPage = new RankEditorIndexPage(playerRef, player, plugin, rankService);
            player.getPageManager().openCustomPage(ref, store, indexPage);
        } catch (Exception ex) {
            player.sendMessage(Message.raw("Failed to save rank.").color("#FF0000"));
        }
    }

    private void handleDelete(Ref<EntityStore> ref, Store<EntityStore> store) {
        String id = rank.getId();
        try {
            boolean ok = rankService.deleteRank(id).join();
            if (ok) {
                player.sendMessage(Message.raw("Rank deleted: " + id).color("#00FF00"));
                var indexPage = new RankEditorIndexPage(playerRef, player, plugin, rankService);
                player.getPageManager().openCustomPage(ref, store, indexPage);
            } else {
                player.sendMessage(Message.raw("Failed to delete rank.").color("#FF0000"));
            }
        } catch (Exception e) {
            player.sendMessage(Message.raw("Failed to delete rank.").color("#FF0000"));
        }
    }

    @Override
    public void onDismiss(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store) {
    }

    // ─── Event Data ────────────────────────────────────────────────
    public static class DetailEventData {
        public static final String KEY_ACTION = "Action";
        public static final String KEY_DISPLAY_NAME = "@DisplayName";
        public static final String KEY_MONEY_COST = "@MoneyCost";

        public static final BuilderCodec<DetailEventData> CODEC = BuilderCodec
                .builder(DetailEventData.class, DetailEventData::new)
                .append(new KeyedCodec<>(KEY_ACTION, Codec.STRING),
                        DetailEventData::setAction, DetailEventData::getAction)
                .add()
                .append(new KeyedCodec<>(KEY_DISPLAY_NAME, Codec.STRING),
                        DetailEventData::setDisplayName, DetailEventData::getDisplayName)
                .add()
                .append(new KeyedCodec<>(KEY_MONEY_COST, Codec.STRING),
                        DetailEventData::setMoneyCost, DetailEventData::getMoneyCost)
                .add()
                .build();

        private String action;
        private String displayName;
        private String moneyCost;

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getMoneyCost() { return moneyCost; }
        public void setMoneyCost(String moneyCost) { this.moneyCost = moneyCost; }
    }
}






