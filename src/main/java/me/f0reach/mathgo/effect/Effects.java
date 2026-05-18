package me.f0reach.mathgo.effect;

import me.f0reach.mathgo.MathGoPlugin;
import net.kyori.adventure.title.Title;

import static me.f0reach.mathgo.ui.Messages.get;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public final class Effects {
    private Effects() {}

    public static void correct(Player player, Location anchor) {
        World w = anchor.getWorld();
        if (w == null) return;
        w.spawnParticle(Particle.HAPPY_VILLAGER, anchor, 40, 0.8, 1.0, 0.8, 0);
        w.spawnParticle(Particle.END_ROD, anchor, 20, 0.4, 1.0, 0.4, 0.05);
        w.playSound(anchor, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.3f);
        player.sendMessage(get("effect.correct"));
    }

    public static void wrong(Player player, Location anchor) {
        World w = anchor.getWorld();
        if (w == null) return;
        w.spawnParticle(Particle.ANGRY_VILLAGER, anchor, 30, 0.6, 1.0, 0.6, 0);
        w.spawnParticle(Particle.SMOKE, anchor, 30, 0.5, 0.6, 0.5, 0.02);
        w.playSound(anchor, Sound.ENTITY_VILLAGER_NO, 1f, 0.9f);
        player.sendMessage(get("effect.wrong"));
    }

    public static void timeout(Player player, Location anchor) {
        World w = anchor.getWorld();
        if (w == null) return;
        w.spawnParticle(Particle.LARGE_SMOKE, anchor, 30, 0.5, 0.5, 0.5, 0.02);
        w.playSound(anchor, Sound.BLOCK_BELL_RESONATE, 1f, 0.6f);
        player.sendMessage(get("effect.timeout"));
    }

    public static void gameOver(MathGoPlugin plugin, Player player, Location anchor) {
        World w = anchor.getWorld();
        if (w == null) return;

        // Frame 0: warning rumble + screen redout title
        w.playSound(anchor, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.MASTER, 1.2f, 0.6f);
        w.playSound(anchor, Sound.ENTITY_CREEPER_PRIMED, SoundCategory.MASTER, 1.2f, 0.7f);
        w.spawnParticle(Particle.SMOKE, anchor.clone().add(0, 1, 0), 40, 1.5, 1.0, 1.5, 0.02);
        player.showTitle(Title.title(
                get("effect.game_over.title"),
                get("effect.game_over.subtitle"),
                Title.Times.times(Duration.ofMillis(100), Duration.ofSeconds(3), Duration.ofMillis(500))));
        player.sendMessage(get("effect.game_over.chat"));

        // Frame 8t (~0.4s): main blast — sound + explosion particle cluster + rail-debris falling blocks.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> spawnMainBlast(w, anchor), 8L);
        // Frame 16t (~0.8s): secondary debris and lava splash.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> spawnSecondaryBlast(w, anchor), 16L);
        // Frame 30t (~1.5s): trailing flames and smoke.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> spawnTrailingFires(w, anchor), 30L);
        // Frame 50t (~2.5s): final burst of embers.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            w.playSound(anchor, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.MASTER, 1f, 0.6f);
            w.spawnParticle(Particle.LAVA, anchor.clone().add(0, 1, 0), 30, 1.2, 1.0, 1.2, 0);
            w.spawnParticle(Particle.LARGE_SMOKE, anchor.clone().add(0, 1.5, 0), 60, 1.5, 1.0, 1.5, 0.05);
        }, 50L);
    }

    private static void spawnMainBlast(World w, Location anchor) {
        w.playSound(anchor, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 1.6f, 0.6f);
        w.playSound(anchor, Sound.ITEM_FIRECHARGE_USE, SoundCategory.MASTER, 1.6f, 0.8f);
        w.spawnParticle(Particle.EXPLOSION_EMITTER, anchor, 4, 1.5, 0.8, 1.5, 0);
        w.spawnParticle(Particle.FLAME, anchor.clone().add(0, 1, 0), 160, 1.8, 1.5, 1.8, 0.15);
        w.spawnParticle(Particle.LAVA, anchor, 25, 1.0, 0.8, 1.0, 0);
        w.spawnParticle(Particle.SOUL_FIRE_FLAME, anchor.clone().add(0, 1, 0), 40, 1.0, 1.0, 1.0, 0.1);
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        Material[] debris = {
                Material.COBBLESTONE, Material.NETHERRACK, Material.GRANITE, Material.DEEPSLATE_BRICKS,
                Material.RAIL, Material.MAGMA_BLOCK, Material.BLACKSTONE
        };
        // Many rail-debris falling blocks flung outward + upward.
        for (int i = 0; i < 50; i++) {
            Material m = debris[rng.nextInt(debris.length)];
            FallingBlock fb = w.spawnFallingBlock(
                    anchor.clone().add(rng.nextDouble(-0.5, 0.5), 1.0, rng.nextDouble(-0.5, 0.5)),
                    m.createBlockData());
            double horiz = rng.nextDouble(0.5, 1.4);
            double angle = rng.nextDouble(0, Math.PI * 2);
            fb.setVelocity(new Vector(
                    Math.cos(angle) * horiz,
                    rng.nextDouble(0.6, 1.3),
                    Math.sin(angle) * horiz));
            fb.setDropItem(false);
            fb.setHurtEntities(false);
        }
    }

    private static void spawnSecondaryBlast(World w, Location anchor) {
        w.playSound(anchor, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 1.2f, 0.5f);
        w.spawnParticle(Particle.EXPLOSION_EMITTER, anchor.clone().add(1, 0, 1), 2, 0.5, 0.5, 0.5, 0);
        w.spawnParticle(Particle.EXPLOSION_EMITTER, anchor.clone().add(-1, 0, -1), 2, 0.5, 0.5, 0.5, 0);
        w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, anchor.clone().add(0, 2, 0), 30, 1.5, 0.5, 1.5, 0.02);
        w.spawnParticle(Particle.LARGE_SMOKE, anchor.clone().add(0, 1, 0), 80, 2.0, 1.5, 2.0, 0.05);
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        Material[] embers = {
                Material.MAGMA_BLOCK, Material.NETHERRACK, Material.BLACKSTONE, Material.COBBLED_DEEPSLATE
        };
        for (int i = 0; i < 30; i++) {
            FallingBlock fb = w.spawnFallingBlock(
                    anchor.clone().add(rng.nextDouble(-1.5, 1.5), 0.5, rng.nextDouble(-1.5, 1.5)),
                    embers[rng.nextInt(embers.length)].createBlockData());
            fb.setVelocity(new Vector(
                    rng.nextDouble(-0.6, 0.6),
                    rng.nextDouble(0.3, 0.8),
                    rng.nextDouble(-0.6, 0.6)));
            fb.setDropItem(false);
            fb.setHurtEntities(false);
        }
    }

    private static void spawnTrailingFires(World w, Location anchor) {
        w.playSound(anchor, Sound.BLOCK_FIRE_AMBIENT, SoundCategory.MASTER, 1.5f, 0.9f);
        w.playSound(anchor, Sound.ENTITY_GHAST_SHOOT, SoundCategory.MASTER, 0.8f, 0.7f);
        w.spawnParticle(Particle.FLAME, anchor.clone().add(0, 0.5, 0), 120, 2.0, 0.8, 2.0, 0.05);
        w.spawnParticle(Particle.SMOKE, anchor.clone().add(0, 1.5, 0), 80, 2.0, 1.0, 2.0, 0.05);
        w.spawnParticle(Particle.ASH, anchor.clone().add(0, 2.5, 0), 50, 2.0, 1.0, 2.0, 0.03);
    }

    public static void goalReached(Player player, Location anchor) {
        World w = anchor.getWorld();
        if (w == null) return;
        w.playSound(anchor, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        w.spawnParticle(Particle.FIREWORK, anchor, 80, 1.5, 1.5, 1.5, 0.2);
        player.sendMessage(get("effect.goal_reached"));
    }
}
