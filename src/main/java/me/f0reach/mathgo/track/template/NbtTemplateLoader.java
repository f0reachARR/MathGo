package me.f0reach.mathgo.track.template;

import me.f0reach.mathgo.track.Direction;
import me.f0reach.mathgo.track.SegmentRole;
import me.f0reach.mathgo.track.TemplateLibrary;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.BlockVector;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans {@code <plugin-data>/templates/<role>/*.nbt}, places each into a scratch area to discover marker
 * metadata, then registers an {@link NbtTemplate} in the supplied {@link TemplateLibrary}.
 */
public final class NbtTemplateLoader {
    private static final Pattern FILENAME = Pattern.compile("^(?<id>[A-Za-z0-9_\\-]+)(?:@(?<weight>\\d+))?\\.nbt$");

    private final File templatesRoot;
    private final World scratchWorld;
    private final int scratchOriginX;
    private final int scratchOriginY;
    private final int scratchOriginZ;
    private final Logger logger;

    public NbtTemplateLoader(File templatesRoot, World scratchWorld,
                             int scratchOriginX, int scratchOriginY, int scratchOriginZ,
                             Logger logger) {
        this.templatesRoot = templatesRoot;
        this.scratchWorld = scratchWorld;
        this.scratchOriginX = scratchOriginX;
        this.scratchOriginY = scratchOriginY;
        this.scratchOriginZ = scratchOriginZ;
        this.logger = logger;
    }

    public void loadAll(TemplateLibrary library) {
        if (!templatesRoot.exists()) {
            templatesRoot.mkdirs();
            return;
        }
        for (SegmentRole role : SegmentRole.values()) {
            File roleDir = new File(templatesRoot, role.name().toLowerCase());
            if (!roleDir.isDirectory()) continue;
            File[] files = roleDir.listFiles((d, name) -> name.endsWith(".nbt"));
            if (files == null) continue;
            for (File file : files) {
                try {
                    NbtTemplate t = loadOne(file, role);
                    if (t != null) {
                        library.register(t);
                        logger.info("Loaded NBT template " + t.id() + " (role=" + role
                                + ", weight=" + t.weight() + ") from " + file.getName());
                    }
                } catch (Exception e) {
                    logger.warning("Failed to load template " + file + ": " + e.getMessage());
                }
            }
        }
    }

    private NbtTemplate loadOne(File file, SegmentRole role) throws IOException {
        Matcher m = FILENAME.matcher(file.getName());
        if (!m.matches()) {
            logger.warning("Skipping template with bad filename: " + file.getName());
            return null;
        }
        String id = m.group("id");
        int weight = m.group("weight") != null ? Integer.parseInt(m.group("weight")) : 1;

        StructureManager mgr = Bukkit.getStructureManager();
        Structure structure = mgr.loadStructure(file);
        BlockVector size = structure.getSize();
        int sizeX = size.getBlockX(), sizeY = size.getBlockY(), sizeZ = size.getBlockZ();

        // Pre-load the scratch chunks covering the placement area.
        for (int cx = scratchOriginX >> 4; cx <= (scratchOriginX + sizeX) >> 4; cx++) {
            for (int cz = scratchOriginZ >> 4; cz <= (scratchOriginZ + sizeZ) >> 4; cz++) {
                Chunk chunk = scratchWorld.getChunkAt(cx, cz);
                if (!chunk.isLoaded()) chunk.load(true);
            }
        }

        Location origin = new Location(scratchWorld, scratchOriginX, scratchOriginY, scratchOriginZ);
        // Place WITHOUT marker stripper so the markers are visible during the scan.
        structure.place(origin, false, StructureRotation.NONE, Mirror.NONE, -1, 1.0f, new Random(), List.of(), List.of());

        BlockVector entryLocal = null;
        Direction entryFacing = null;
        BlockVector exitLocal = null;
        Direction exitFacing = null;
        BlockVector stopLocal = null;
        BlockVector displayLocal = null;

        try {
            for (int lx = 0; lx < sizeX; lx++) {
                for (int ly = 0; ly < sizeY; ly++) {
                    for (int lz = 0; lz < sizeZ; lz++) {
                        int wx = scratchOriginX + lx;
                        int wy = scratchOriginY + ly;
                        int wz = scratchOriginZ + lz;
                        Material mat = scratchWorld.getBlockAt(wx, wy, wz).getType();
                        MarkerBlocks marker = MarkerBlocks.of(mat);
                        if (marker == null) continue;
                        BlockData data = scratchWorld.getBlockAt(wx, wy, wz).getBlockData();
                        Direction facing = Direction.NORTH;
                        if (data instanceof Directional dir) {
                            BlockFace face = dir.getFacing();
                            facing = MarkerBlocks.directionFromFacing(face);
                        }
                        BlockVector pos = new BlockVector(lx, ly, lz);
                        switch (marker) {
                            case ENTRY -> { entryLocal = pos; entryFacing = facing; }
                            case EXIT -> { exitLocal = pos; exitFacing = facing; }
                            case QUESTION_STOP -> stopLocal = pos;
                            case QUESTION_DISPLAY -> displayLocal = pos;
                        }
                    }
                }
            }
        } finally {
            // Clear scratch area regardless of scan outcome.
            for (int lx = 0; lx < sizeX; lx++) {
                for (int ly = 0; ly < sizeY; ly++) {
                    for (int lz = 0; lz < sizeZ; lz++) {
                        scratchWorld.getBlockAt(scratchOriginX + lx, scratchOriginY + ly, scratchOriginZ + lz)
                                .setType(Material.AIR, false);
                    }
                }
            }
        }

        if (entryLocal == null || entryFacing == null) {
            logger.warning("Template " + id + " missing ENTRY marker; skipping.");
            return null;
        }
        if (exitLocal == null || exitFacing == null) {
            logger.warning("Template " + id + " missing EXIT marker; skipping.");
            return null;
        }
        if (role == SegmentRole.QUESTION && stopLocal == null) {
            logger.warning("Question template " + id + " missing QUESTION_STOP marker; skipping.");
            return null;
        }

        return new NbtTemplate(id, role, weight, structure, size,
                entryLocal, entryFacing, exitLocal, exitFacing, stopLocal, displayLocal);
    }
}
