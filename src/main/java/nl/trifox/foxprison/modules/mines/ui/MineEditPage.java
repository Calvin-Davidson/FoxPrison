package nl.trifox.foxprison.modules.mines.ui;

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
import nl.trifox.foxprison.modules.mines.MineService;
import nl.trifox.foxprison.modules.mines.config.AutoResetDefinition;
import nl.trifox.foxprison.modules.mines.config.BoxRegionDefinition;
import nl.trifox.foxprison.modules.mines.config.MineDefinition;
import nl.trifox.foxprison.modules.ranks.config.RankDefinition;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Detailed mine editing page — exposes all configurable fields for a single
 * mine in a scrollable form with a single Save button.
 */
public class MineEditPage extends InteractiveCustomUIPage<MineEditPage.DetailEventData> {

    private final FoxPrisonPlugin plugin;
    private final Player player;
    private final MineService mineService;
    private final MineDefinition mine;
    private boolean autoResetEnabled;
    private final Set<String> allowedRanks;

    public MineEditPage(PlayerRef playerRef, Player player, FoxPrisonPlugin plugin,
                        MineService mineService, MineDefinition mine) {
        super(playerRef, CustomPageLifetime.CanDismiss, DetailEventData.CODEC);
        this.player = player;
        this.plugin = plugin;
        this.mineService = mineService;
        this.mine = mine;
        this.autoResetEnabled = mine.getAutoReset().isEnabled();
        this.allowedRanks = new LinkedHashSet<>(Arrays.asList(mine.getRequirements().getAllowedRanks()));
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, UICommandBuilder cmd, UIEventBuilder events,
                      @NonNullDecl Store<EntityStore> store) {

        cmd.append("Pages/FoxPrison_MineEditPage.ui");

        // Back → index
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#BackButton",
                EventData.of(DetailEventData.KEY_ACTION, "back"),
                false);

        // ── Populate current values ──────────────────────────────
        cmd.set("#MineNameLabel.Text", mine.getDisplayName());
        cmd.set("#MineIdLabel.Text", "ID: " + mine.getId());
        cmd.set("#WorldNameLabel.Text", "World: " + mine.getWorld());
        cmd.set("#DisplayNameField.Value", mine.getDisplayName());
        cmd.set("#OrderField.Value", String.valueOf(mine.getOrder()));

        AutoResetDefinition ar = mine.getAutoReset();
        cmd.set("#AutoResetStatusLabel.Text", autoResetEnabled ? "Enabled" : "Disabled");
        cmd.set("#IntervalField.Value", String.valueOf(ar.getIntervalSeconds()));
        cmd.set("#ThresholdField.Value", String.valueOf(ar.getBlocksBrokenThreshold()));
        cmd.set("#MinDelayField.Value", String.valueOf(ar.getMinSecondsBetweenResets()));

        // Region info
        BoxRegionDefinition[] boxes = mine.getRegion().getBoxes();
        if (boxes.length == 0) {
            cmd.set("#RegionInfoLabel.Text", "No region boxes defined");
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(boxes.length).append(" box(es): ");
            for (int i = 0; i < boxes.length; i++) {
                var b = boxes[i];
                sb.append("[")
                  .append(b.getNormalizedMin().getX()).append(",")
                  .append(b.getNormalizedMin().getY()).append(",")
                  .append(b.getNormalizedMin().getZ())
                  .append(" -> ")
                  .append(b.getNormalizedMax().getX()).append(",")
                  .append(b.getNormalizedMax().getY()).append(",")
                  .append(b.getNormalizedMax().getZ())
                  .append("]");
                if (i < boxes.length - 1) sb.append(", ");
            }
            cmd.set("#RegionInfoLabel.Text", sb.toString());
        }

        // ── Rank checkboxes ──────────────────────────────────────
        RankDefinition[] allRanks = FoxPrisonPlugin.getRankModule().getRankService().getAllRanks();
        cmd.clear("#RankChecks");

        for (int i = 0; i < allRanks.length; i++) {
            RankDefinition rank = allRanks[i];
            boolean checked = allowedRanks.contains(rank.getId());

            String template = checked
                    ? "Pages/FoxPrison_RankCheckEntryChecked.ui"
                    : "Pages/FoxPrison_RankCheckEntry.ui";
            cmd.append("#RankChecks", template);

            String prefix = "#RankChecks[" + i + "] ";
            cmd.set(prefix + "#RankLabel.Text", rank.getDisplayName() + "  (" + rank.getId() + ")");

            events.addEventBinding(CustomUIEventBindingType.ValueChanged,
                    prefix + "#RankCheckBox",
                    EventData.of(DetailEventData.KEY_ACTION, "toggleRank")
                            .append(DetailEventData.KEY_RANK, rank.getId()),
                    false);
        }

        // ── Event bindings ───────────────────────────────────────

        // Toggle auto-reset (immediate visual feedback, persisted on save)
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#ToggleAutoResetButton",
                EventData.of(DetailEventData.KEY_ACTION, "toggleAutoReset"),
                false);

        // Save — reads all text fields at once
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#SaveButton",
                EventData.of(DetailEventData.KEY_ACTION, "save")
                        .append(DetailEventData.KEY_DISPLAY_NAME, "#DisplayNameField.Value")
                        .append(DetailEventData.KEY_ORDER, "#OrderField.Value")
                        .append(DetailEventData.KEY_INTERVAL, "#IntervalField.Value")
                        .append(DetailEventData.KEY_THRESHOLD, "#ThresholdField.Value")
                        .append(DetailEventData.KEY_MIN_DELAY, "#MinDelayField.Value"),
                false);

        // Action buttons
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#TeleportButton",
                EventData.of(DetailEventData.KEY_ACTION, "teleport"),
                false);

        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#ResetButton",
                EventData.of(DetailEventData.KEY_ACTION, "reset"),
                false);

        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#SetSpawnButton",
                EventData.of(DetailEventData.KEY_ACTION, "setSpawn"),
                false);

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
                var indexPage = new MineEditorIndexPage(playerRef, player, plugin, mineService);
                player.getPageManager().openCustomPage(ref, store, indexPage);
            }
            case "toggleAutoReset" -> handleToggleAutoReset();
            case "toggleRank" -> handleToggleRank(eventData);
            case "save" -> handleSave(eventData, ref, store);
            case "teleport" -> handleTeleport(ref, store);
            case "reset" -> handleReset();
            case "setSpawn" -> handleSetSpawn();
            case "delete" -> handleDelete(ref, store);
        }
    }

    private void handleToggleAutoReset() {
        autoResetEnabled = !autoResetEnabled;
        rebuild();
    }

    private void handleToggleRank(DetailEventData data) {
        String rankId = data.getRank();
        if (rankId == null) return;

        if (allowedRanks.contains(rankId)) {
            allowedRanks.remove(rankId);
        } else {
            allowedRanks.add(rankId);
        }
        // No rebuild needed — checkbox state is already toggled client-side
    }

    private void handleSave(DetailEventData data, Ref<EntityStore> ref, Store<EntityStore> store) {
        // ── Display name ─────────────────────────────────────────
        String newName = data.getDisplayName();
        if (newName == null || newName.trim().isEmpty()) {
            player.sendMessage(Message.raw("Display name cannot be empty!").color("#FF6600"));
            return;
        }

        // ── Order ────────────────────────────────────────────────
        int order;
        try {
            order = Integer.parseInt(data.getOrder().trim());
        } catch (NumberFormatException | NullPointerException e) {
            player.sendMessage(Message.raw("Order must be a valid number!").color("#FF0000"));
            return;
        }
        if (order < 0 || order > 9999) {
            player.sendMessage(Message.raw("Order must be between 0 and 9999!").color("#FF6600"));
            return;
        }

        // ── Auto-reset fields ────────────────────────────────────
        int interval, threshold, minDelay;
        try {
            interval = Integer.parseInt(data.getInterval().trim());
            threshold = Integer.parseInt(data.getThreshold().trim());
            minDelay = Integer.parseInt(data.getMinDelay().trim());
        } catch (NumberFormatException | NullPointerException e) {
            player.sendMessage(Message.raw("Auto-reset fields must be valid numbers!").color("#FF0000"));
            return;
        }

        // ── Persist everything ───────────────────────────────────
        try {
            mineService.setDisplayName(mine.getId(), newName.trim()).join();
            mineService.setMineOrder(mine.getId(), order).join();
            mineService.setAutoReset(mine.getId(),
                    autoResetEnabled, interval, threshold, minDelay).join();
            mineService.setAllowedRanks(mine.getId(),
                    allowedRanks.toArray(new String[0])).join();

            player.sendMessage(Message.raw("Mine saved!").color("#00FF00"));
            player.getPageManager().setPage(ref, store, Page.None);
        } catch (Exception ex) {
            player.sendMessage(Message.raw("Failed to save mine.").color("#FF0000"));
        }
    }

    private void handleTeleport(Ref<EntityStore> ref, Store<EntityStore> store) {
        mineService.teleportToMine(playerRef, mine);
        player.getPageManager().setPage(ref, store, Page.None);
        player.sendMessage(Message.raw("Teleported to mine: " + mine.getDisplayName()).color("#00FF00"));
    }

    private void handleReset() {
        player.sendMessage(Message.raw("Resetting mine...").color("#00FF00"));
        mineService.resetMine(mine.getId());
    }

    private void handleSetSpawn() {
        try {
            var transform = playerRef.getTransform();
            boolean ok = mineService.setSpawnPoint(mine.getId(), transform).join();
            if (ok) {
                player.sendMessage(Message.raw("Mine spawn set to your current position.").color("#00FF00"));
                rebuild();
            } else {
                player.sendMessage(Message.raw("Failed to set spawn.").color("#FF0000"));
            }
        } catch (Exception e) {
            player.sendMessage(Message.raw("Failed to set spawn.").color("#FF0000"));
        }
    }

    private void handleDelete(Ref<EntityStore> ref, Store<EntityStore> store) {
        String id = mine.getId();
        try {
            boolean ok = mineService.deleteMine(id).join();
            if (ok) {
                player.sendMessage(Message.raw("Mine deleted: " + id).color("#00FF00"));
                var indexPage = new MineEditorIndexPage(playerRef, player, plugin, mineService);
                player.getPageManager().openCustomPage(ref, store, indexPage);
            } else {
                player.sendMessage(Message.raw("Failed to delete mine.").color("#FF0000"));
            }
        } catch (Exception e) {
            player.sendMessage(Message.raw("Failed to delete mine.").color("#FF0000"));
        }
    }

    @Override
    public void onDismiss(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store) {
    }

    // ─── Event Data ────────────────────────────────────────────────
    public static class DetailEventData {
        public static final String KEY_ACTION = "Action";
        public static final String KEY_DISPLAY_NAME = "@DisplayName";
        public static final String KEY_ORDER = "@Order";
        public static final String KEY_INTERVAL = "@Interval";
        public static final String KEY_THRESHOLD = "@Threshold";
        public static final String KEY_MIN_DELAY = "@MinDelay";
        public static final String KEY_RANK = "Rank";

        public static final BuilderCodec<DetailEventData> CODEC = BuilderCodec
                .builder(DetailEventData.class, DetailEventData::new)
                .append(new KeyedCodec<>(KEY_ACTION, Codec.STRING),
                        DetailEventData::setAction, DetailEventData::getAction)
                .add()
                .append(new KeyedCodec<>(KEY_DISPLAY_NAME, Codec.STRING),
                        DetailEventData::setDisplayName, DetailEventData::getDisplayName)
                .add()
                .append(new KeyedCodec<>(KEY_ORDER, Codec.STRING),
                        DetailEventData::setOrder, DetailEventData::getOrder)
                .add()
                .append(new KeyedCodec<>(KEY_INTERVAL, Codec.STRING),
                        DetailEventData::setInterval, DetailEventData::getInterval)
                .add()
                .append(new KeyedCodec<>(KEY_THRESHOLD, Codec.STRING),
                        DetailEventData::setThreshold, DetailEventData::getThreshold)
                .add()
                .append(new KeyedCodec<>(KEY_MIN_DELAY, Codec.STRING),
                        DetailEventData::setMinDelay, DetailEventData::getMinDelay)
                .add()
                .append(new KeyedCodec<>(KEY_RANK, Codec.STRING),
                        DetailEventData::setRank, DetailEventData::getRank)
                .add()
                .build();

        private String action;
        private String displayName;
        private String order;
        private String interval;
        private String threshold;
        private String minDelay;
        private String rank;

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getOrder() { return order; }
        public void setOrder(String order) { this.order = order; }
        public String getInterval() { return interval; }
        public void setInterval(String interval) { this.interval = interval; }
        public String getThreshold() { return threshold; }
        public void setThreshold(String threshold) { this.threshold = threshold; }
        public String getMinDelay() { return minDelay; }
        public void setMinDelay(String minDelay) { this.minDelay = minDelay; }
        public String getRank() { return rank; }
        public void setRank(String rank) { this.rank = rank; }
    }
}


