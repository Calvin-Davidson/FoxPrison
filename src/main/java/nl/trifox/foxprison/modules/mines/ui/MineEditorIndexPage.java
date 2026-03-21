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
import nl.trifox.foxprison.modules.mines.config.BoxRegionDefinition;
import nl.trifox.foxprison.modules.mines.config.MineDefinition;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Index page for the mine editor — lists all mines with teleport/edit/reset
 * buttons.
 */
public class MineEditorIndexPage extends InteractiveCustomUIPage<MineEditorIndexPage.IndexEventData> {

    private final FoxPrisonPlugin plugin;
    private final MineService mineService;
    private final Player player;

    public MineEditorIndexPage(PlayerRef playerRef, Player player, FoxPrisonPlugin plugin, MineService mineService) {
        super(playerRef, CustomPageLifetime.CanDismiss, IndexEventData.CODEC);
        this.player = player;
        this.plugin = plugin;
        this.mineService = mineService;
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, UICommandBuilder cmd, UIEventBuilder events,
                      @NonNullDecl Store<EntityStore> store) {

        cmd.append("Pages/FoxPrison_MineEditorIndex.ui");

        // Back button closes the page
        events.addEventBinding(CustomUIEventBindingType.Activating,
                "#CloseButton",
                EventData.of(IndexEventData.KEY_ACTION, "close"),
                false);

        List<MineDefinition> allMines = new ArrayList<>(mineService.getAllMines());
        allMines.sort(Comparator.comparingInt(MineDefinition::getOrder)
                .thenComparing(MineDefinition::getId, String.CASE_INSENSITIVE_ORDER));

        cmd.set("#TotalMinesLabel.Text", "Total: " + allMines.size() + " Mines");
        cmd.clear("#IndexCards");

        int index = 0;
        for (MineDefinition mine : allMines) {
            cmd.append("#IndexCards", "Pages/FoxPrison_MineEditorEntry.ui");

            String prefix = "#IndexCards[" + index + "] ";
            cmd.set(prefix + "#MineName.Text", mine.getDisplayName());

            // Build info line
            BoxRegionDefinition[] boxes = mine.getRegion().getBoxes();
            String info = "ID: " + mine.getId()
                    + "  |  World: " + mine.getWorld()
                    + "  |  Regions: " + boxes.length
                    + "  |  Order: " + mine.getOrder();
            cmd.set(prefix + "#MineInfo.Text", info);

            events.addEventBinding(CustomUIEventBindingType.Activating,
                    prefix + "#EditButton",
                    EventData.of(IndexEventData.KEY_ACTION, "edit")
                            .append(IndexEventData.KEY_MINE, mine.getId()),
                    false);

            events.addEventBinding(CustomUIEventBindingType.Activating,
                    prefix + "#TeleportButton",
                    EventData.of(IndexEventData.KEY_ACTION, "teleport")
                            .append(IndexEventData.KEY_MINE, mine.getId()),
                    false);

            events.addEventBinding(CustomUIEventBindingType.Activating,
                    prefix + "#ResetButton",
                    EventData.of(IndexEventData.KEY_ACTION, "reset")
                            .append(IndexEventData.KEY_MINE, mine.getId()),
                    false);

            index++;
        }
    }

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store,
                                IndexEventData eventData) {
        String action = eventData.getAction();
        if (action == null) return;

        String mineId = eventData.getMine();

        switch (action) {
            case "close" -> {
                player.getPageManager().setPage(ref, store, Page.None);
            }
            case "teleport" -> handleTeleport(ref, store, mineId);
            case "edit" -> handleEdit(ref, store, mineId);
            case "reset" -> handleReset(mineId);
        }
    }

    private void handleTeleport(Ref<EntityStore> ref, Store<EntityStore> store, String mineId) {
        var mineOpt = mineService.getMine(mineId);
        if (mineOpt.isEmpty()) {
            player.sendMessage(Message.raw("Mine not found: " + mineId).color("#FF0000"));
            return;
        }
        mineService.teleportToMine(playerRef, mineOpt.get());
        player.sendMessage(Message.raw("Teleported to mine: " + mineOpt.get().getDisplayName()).color("#00FF00"));
        player.getPageManager().setPage(ref, store, Page.None);
    }

    private void handleEdit(Ref<EntityStore> ref, Store<EntityStore> store, String mineId) {
        var mineOpt = mineService.getMine(mineId);
        if (mineOpt.isEmpty()) {
            player.sendMessage(Message.raw("Mine not found: " + mineId).color("#FF0000"));
            return;
        }

        var editPage = new MineEditPage(playerRef, player, plugin, mineService, mineOpt.get());
        player.getPageManager().openCustomPage(ref, store, editPage);
    }

    private void handleReset(String mineId) {
        player.sendMessage(Message.raw("Resetting mine: " + mineId + "...").color("#00FF00"));
        mineService.resetMine(mineId);
    }

    @Override
    public void onDismiss(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store) {
    }

    // ─── Event Data ────────────────────────────────────────────────
    public static class IndexEventData {
        public static final String KEY_ACTION = "Action";
        public static final String KEY_MINE = "Mine";

        public static final BuilderCodec<IndexEventData> CODEC = BuilderCodec
                .builder(IndexEventData.class, IndexEventData::new)
                .append(new KeyedCodec<>(KEY_ACTION, Codec.STRING),
                        IndexEventData::setAction, IndexEventData::getAction)
                .add()
                .append(new KeyedCodec<>(KEY_MINE, Codec.STRING),
                        IndexEventData::setMine, IndexEventData::getMine)
                .add()
                .build();

        private String action;
        private String mine;

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getMine() { return mine; }
        public void setMine(String mine) { this.mine = mine; }
    }
}



