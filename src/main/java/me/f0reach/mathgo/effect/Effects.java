package me.f0reach.mathgo.effect;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public final class Effects {
    private Effects() {}

    public static void correct(Player player, Location anchor) {
        World w = anchor.getWorld();
        if (w == null) return;
        w.spawnParticle(Particle.HAPPY_VILLAGER, anchor, 40, 0.8, 1.0, 0.8, 0);
        w.spawnParticle(Particle.END_ROD, anchor, 20, 0.4, 1.0, 0.4, 0.05);
        w.playSound(anchor, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.3f);
        player.sendMessage(Component.text("せいかい！", NamedTextColor.GREEN));
    }

    public static void wrong(Player player, Location anchor) {
        World w = anchor.getWorld();
        if (w == null) return;
        w.spawnParticle(Particle.ANGRY_VILLAGER, anchor, 30, 0.6, 1.0, 0.6, 0);
        w.spawnParticle(Particle.SMOKE, anchor, 30, 0.5, 0.6, 0.5, 0.02);
        w.playSound(anchor, Sound.ENTITY_VILLAGER_NO, 1f, 0.9f);
        player.sendMessage(Component.text("ざんねん…", NamedTextColor.RED));
    }

    public static void timeout(Player player, Location anchor) {
        World w = anchor.getWorld();
        if (w == null) return;
        w.spawnParticle(Particle.LARGE_SMOKE, anchor, 30, 0.5, 0.5, 0.5, 0.02);
        w.playSound(anchor, Sound.BLOCK_BELL_RESONATE, 1f, 0.6f);
        player.sendMessage(Component.text("じかんぎれ！", NamedTextColor.GOLD));
    }

    public static void gameOver(Player player, Location anchor) {
        World w = anchor.getWorld();
        if (w == null) return;
        w.playSound(anchor, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.7f);
        w.spawnParticle(Particle.EXPLOSION_EMITTER, anchor, 3, 1.5, 1.0, 1.5, 0);
        w.spawnParticle(Particle.FLAME, anchor, 80, 1.5, 1.5, 1.5, 0.1);
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < 18; i++) {
            FallingBlock fb = w.spawnFallingBlock(anchor.clone().add(0, 1, 0),
                    (i % 2 == 0 ? Material.COBBLESTONE : Material.NETHERRACK).createBlockData());
            fb.setVelocity(new Vector(
                    rng.nextDouble(-0.5, 0.5),
                    rng.nextDouble(0.3, 0.9),
                    rng.nextDouble(-0.5, 0.5)));
            fb.setDropItem(false);
            fb.setHurtEntities(false);
        }
        player.sendMessage(Component.text("ゲームオーバー…", NamedTextColor.DARK_RED));
    }

    public static void goalReached(Player player, Location anchor) {
        World w = anchor.getWorld();
        if (w == null) return;
        w.playSound(anchor, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        w.spawnParticle(Particle.FIREWORK, anchor, 80, 1.5, 1.5, 1.5, 0.2);
        player.sendMessage(Component.text("クリア！おめでとう！", NamedTextColor.GOLD));
    }
}
