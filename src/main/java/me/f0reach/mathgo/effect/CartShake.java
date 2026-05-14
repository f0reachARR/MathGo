package me.f0reach.mathgo.effect;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Minecart;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public final class CartShake {
    private CartShake() {}

    public static void apply(Minecart cart) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        Vector jitter = new Vector(
                rng.nextDouble(-0.02, 0.02),
                0.0,
                rng.nextDouble(-0.02, 0.02));
        cart.setVelocity(jitter);
        World w = cart.getWorld();
        if (rng.nextInt(3) == 0) {
            w.spawnParticle(Particle.SMOKE, cart.getLocation().add(0, 0.3, 0), 2, 0.1, 0.1, 0.1, 0.005);
        }
    }
}
