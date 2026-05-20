package me.f0reach.mathgo.track.template;

import me.f0reach.mathgo.MathGoPlugin;
import me.f0reach.mathgo.track.Direction;
import me.f0reach.mathgo.track.SegmentRole;
import me.f0reach.mathgo.ui.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.BlockVector;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TemplateAuthoringService {
    public static final String WAND_KEY = "mathgo_template_wand";
    public static final String MARKER_KEY = "mathgo_marker_kind";

    private final MathGoPlugin plugin;
    private final Map<UUID, TemplateDraft> drafts = new HashMap<>();

    public TemplateAuthoringService(MathGoPlugin plugin) {
        this.plugin = plugin;
    }

    public TemplateDraft draftOf(Player p) {
        return drafts.computeIfAbsent(p.getUniqueId(), k -> new TemplateDraft());
    }

    /** Returns the existing draft for {@code p} without creating one. */
    @org.jetbrains.annotations.Nullable
    public TemplateDraft peekDraft(Player p) {
        return drafts.get(p.getUniqueId());
    }

    public void clear(Player p) {
        drafts.remove(p.getUniqueId());
    }

    /** Unified cuboid selection (block-coord inclusive) sourced from WorldEdit or the draft. */
    public record Selection(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public boolean contains(BlockVector v) {
            return v.getBlockX() >= minX && v.getBlockX() <= maxX
                    && v.getBlockY() >= minY && v.getBlockY() <= maxY
                    && v.getBlockZ() >= minZ && v.getBlockZ() <= maxZ;
        }
    }

    /**
     * Resolves the player's current template selection — preferring a WorldEdit cuboid when present,
     * falling back to the draft's {@code pos1/pos2}. Returns {@code null} when no selection is set or
     * the two corners disagree on world.
     */
    @org.jetbrains.annotations.Nullable
    public Selection selectionFor(Player player, TemplateDraft draft) {
        var weSel = plugin.worldEditAvailable()
                ? me.f0reach.mathgo.integration.WorldEditBridge.getSelection(player) : null;
        if (weSel != null) {
            return new Selection(player.getWorld(),
                    weSel.minX(), weSel.minY(), weSel.minZ(),
                    weSel.maxX(), weSel.maxY(), weSel.maxZ());
        }
        if (draft.pos1() != null && draft.pos2() != null) {
            Location p1 = draft.pos1();
            Location p2 = draft.pos2();
            World w = p1.getWorld();
            if (w == null || w != p2.getWorld()) return null;
            return new Selection(w,
                    Math.min(p1.getBlockX(), p2.getBlockX()),
                    Math.min(p1.getBlockY(), p2.getBlockY()),
                    Math.min(p1.getBlockZ(), p2.getBlockZ()),
                    Math.max(p1.getBlockX(), p2.getBlockX()),
                    Math.max(p1.getBlockY(), p2.getBlockY()),
                    Math.max(p1.getBlockZ(), p2.getBlockZ()));
        }
        return null;
    }

    public ItemStack createWand() {
        ItemStack stack = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Messages.get("template.wand.name"));
        meta.getPersistentDataContainer().set(wandKey(), PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isWand(ItemStack stack) {
        if (stack == null) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        Byte v = meta.getPersistentDataContainer().get(wandKey(), PersistentDataType.BYTE);
        return v != null && v == 1;
    }

    private NamespacedKey wandKey() {
        return new NamespacedKey(plugin, WAND_KEY);
    }

    private NamespacedKey markerKey() {
        return new NamespacedKey(plugin, MARKER_KEY);
    }

    /** Marker items needed to fully describe a template of the given role. */
    public java.util.List<MarkerBlocks> markersFor(SegmentRole role) {
        if (role == SegmentRole.QUESTION) {
            return java.util.List.of(MarkerBlocks.ENTRY, MarkerBlocks.EXIT,
                    MarkerBlocks.QUESTION_STOP, MarkerBlocks.QUESTION_DISPLAY);
        }
        return java.util.List.of(MarkerBlocks.ENTRY, MarkerBlocks.EXIT);
    }

    /**
     * Builds an inventory item that looks like the marker block but is tagged so it can be
     * recognized on placement. Place the item in the world to register the corresponding anchor
     * in the player's draft; the placement is cancelled by {@code MarkerPlaceListener}.
     */
    public ItemStack createMarker(MarkerBlocks marker) {
        ItemStack stack = new ItemStack(marker.material());
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Messages.get("template.marker.item_name", Messages.u("kind", marker.name())));
        meta.getPersistentDataContainer().set(markerKey(), PersistentDataType.STRING, marker.name());
        stack.setItemMeta(meta);
        return stack;
    }

    /** Returns the marker kind tagged on the item, or {@code null} if this is a regular item. */
    @org.jetbrains.annotations.Nullable
    public MarkerBlocks markerOf(@org.jetbrains.annotations.Nullable ItemStack stack) {
        if (stack == null) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        String kind = meta.getPersistentDataContainer().get(markerKey(), PersistentDataType.STRING);
        if (kind == null) return null;
        try {
            return MarkerBlocks.valueOf(kind);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public File templatesRoot() {
        return new File(plugin.getDataFolder(), "templates");
    }

    /** Result of a save attempt: ok==true with file on success, false with reason otherwise. */
    public record SaveResult(boolean ok, String message, File file, String selectionSource) {
        public SaveResult(boolean ok, String message, File file) { this(ok, message, file, ""); }
    }

    public SaveResult save(Player player, SegmentRole role, String id, int weight) {
        TemplateDraft draft = draftOf(player);
        if (draft.entry() == null || draft.entryFacing() == null) {
            return new SaveResult(false, "entry が未設定です。", null);
        }
        if (draft.exit() == null || draft.exitFacing() == null) {
            return new SaveResult(false, "exit が未設定です。", null);
        }
        if (role == SegmentRole.QUESTION && draft.stop() == null) {
            return new SaveResult(false, "question テンプレには anchor stop が必要です。", null);
        }

        // Resolve the cuboid corners — prefer WorldEdit selection if available, then fall back to draft.pos1/pos2.
        Selection selection = selectionFor(player, draft);
        if (selection == null) {
            if (draft.pos1() != null && draft.pos2() != null) {
                return new SaveResult(false, "pos1 と pos2 のワールドが一致しません。", null);
            }
            return new SaveResult(false,
                    plugin.worldEditAvailable()
                            ? "WorldEdit 選択も pos1/pos2 も未設定です。"
                            : "pos1/pos2 が未設定です。",
                    null);
        }
        World world = selection.world();
        int x1 = selection.minX(), y1 = selection.minY(), z1 = selection.minZ();
        int x2 = selection.maxX(), y2 = selection.maxY(), z2 = selection.maxZ();
        boolean fromWorldEdit = plugin.worldEditAvailable()
                && me.f0reach.mathgo.integration.WorldEditBridge.getSelection(player) != null;
        String selectionSource = fromWorldEdit ? "WorldEdit" : "draft";

        // Validate marker positions are inside the selection.
        if (!selection.contains(draft.entry()) || !selection.contains(draft.exit())) {
            return new SaveResult(false, "entry/exit が選択範囲外です。", null);
        }
        if (draft.stop() != null && !selection.contains(draft.stop())) {
            return new SaveResult(false, "stop が選択範囲外です。", null);
        }
        if (draft.display() != null && !selection.contains(draft.display())) {
            return new SaveResult(false, "display が選択範囲外です。", null);
        }

        // Sanitize id.
        if (!id.matches("^[A-Za-z0-9_\\-]+$")) {
            return new SaveResult(false, "id は半角英数 / _ / - のみ使用できます。", null);
        }

        // Place markers transiently, capturing original block data so we can restore.
        Map<BlockVector, BlockData> originalBlocks = new HashMap<>();
        placeMarker(world, draft.entry(), MarkerBlocks.ENTRY, draft.entryFacing(), originalBlocks);
        placeMarker(world, draft.exit(), MarkerBlocks.EXIT, draft.exitFacing(), originalBlocks);
        if (draft.stop() != null) {
            placeMarker(world, draft.stop(), MarkerBlocks.QUESTION_STOP, draft.entryFacing(), originalBlocks);
        }
        if (draft.display() != null) {
            placeMarker(world, draft.display(), MarkerBlocks.QUESTION_DISPLAY, draft.entryFacing(), originalBlocks);
        }

        File roleDir = new File(templatesRoot(), role.name().toLowerCase());
        roleDir.mkdirs();
        String fileName = weight == 1 ? id + ".nbt" : id + "@" + weight + ".nbt";
        File target = new File(roleDir, fileName);

        SaveResult result;
        try {
            StructureManager mgr = Bukkit.getStructureManager();
            Structure structure = mgr.createStructure();
            Location c1 = new Location(world, x1, y1, z1);
            Location c2 = new Location(world, x2, y2, z2);
            structure.fill(c1, c2, false);
            mgr.saveStructure(target, structure);
            result = new SaveResult(true, "保存しました: " + target.getName() + " (" + selectionSource + ")",
                    target, selectionSource);
        } catch (IOException e) {
            result = new SaveResult(false, "保存に失敗: " + e.getMessage(), null);
        } finally {
            // Restore original blocks.
            for (Map.Entry<BlockVector, BlockData> entry : originalBlocks.entrySet()) {
                BlockVector v = entry.getKey();
                world.getBlockAt(v.getBlockX(), v.getBlockY(), v.getBlockZ())
                        .setBlockData(entry.getValue(), false);
            }
        }
        return result;
    }

    private static void placeMarker(World world, BlockVector pos, MarkerBlocks marker, Direction facing,
                                    Map<BlockVector, BlockData> save) {
        Block b = world.getBlockAt(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
        BlockVector key = pos.clone();
        if (!save.containsKey(key)) {
            save.put(key, b.getBlockData().clone());
        }
        b.setBlockData(marker.dataFacing(facing), false);
    }

}
