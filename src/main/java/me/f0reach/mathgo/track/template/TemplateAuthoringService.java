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

    private final MathGoPlugin plugin;
    private final Map<UUID, TemplateDraft> drafts = new HashMap<>();

    public TemplateAuthoringService(MathGoPlugin plugin) {
        this.plugin = plugin;
    }

    public TemplateDraft draftOf(Player p) {
        return drafts.computeIfAbsent(p.getUniqueId(), k -> new TemplateDraft());
    }

    public void clear(Player p) {
        drafts.remove(p.getUniqueId());
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
        int x1, y1, z1, x2, y2, z2;
        World world;
        String selectionSource;
        var weSelection = plugin.worldEditAvailable()
                ? me.f0reach.mathgo.integration.WorldEditBridge.getSelection(player) : null;
        if (weSelection != null) {
            world = player.getWorld();
            x1 = weSelection.minX(); y1 = weSelection.minY(); z1 = weSelection.minZ();
            x2 = weSelection.maxX(); y2 = weSelection.maxY(); z2 = weSelection.maxZ();
            selectionSource = "WorldEdit";
        } else {
            if (draft.pos1() == null || draft.pos2() == null) {
                return new SaveResult(false,
                        plugin.worldEditAvailable()
                                ? "WorldEdit 選択も pos1/pos2 も未設定です。"
                                : "pos1/pos2 が未設定です。",
                        null);
            }
            Location p1 = draft.pos1();
            Location p2 = draft.pos2();
            world = p1.getWorld();
            if (world == null || world != p2.getWorld()) {
                return new SaveResult(false, "pos1 と pos2 のワールドが一致しません。", null);
            }
            x1 = Math.min(p1.getBlockX(), p2.getBlockX());
            y1 = Math.min(p1.getBlockY(), p2.getBlockY());
            z1 = Math.min(p1.getBlockZ(), p2.getBlockZ());
            x2 = Math.max(p1.getBlockX(), p2.getBlockX());
            y2 = Math.max(p1.getBlockY(), p2.getBlockY());
            z2 = Math.max(p1.getBlockZ(), p2.getBlockZ());
            selectionSource = "draft";
        }

        // Validate marker positions are inside the selection.
        if (!within(draft.entry(), x1, y1, z1, x2, y2, z2)
                || !within(draft.exit(), x1, y1, z1, x2, y2, z2)) {
            return new SaveResult(false, "entry/exit が選択範囲外です。", null);
        }
        if (draft.stop() != null && !within(draft.stop(), x1, y1, z1, x2, y2, z2)) {
            return new SaveResult(false, "stop が選択範囲外です。", null);
        }
        if (draft.display() != null && !within(draft.display(), x1, y1, z1, x2, y2, z2)) {
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

    private static boolean within(BlockVector v, int x1, int y1, int z1, int x2, int y2, int z2) {
        return v.getBlockX() >= x1 && v.getBlockX() <= x2
                && v.getBlockY() >= y1 && v.getBlockY() <= y2
                && v.getBlockZ() >= z1 && v.getBlockZ() <= z2;
    }
}
