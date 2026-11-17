package org.overengineer.bettacraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;
import org.overengineer.bettacraft.registry.ModParticles;

/**
 * M23: an air bubble of an air stone or sponge filter. It rises (a little faster as it goes, wobbling) up to the given
 * height and pops there; unlike Minecraft's bubble it does not need a water block (the tank interior has none).
 */
public class AirBubbleParticle extends SingleQuadParticle {
    private final float top;
    private final double phase;

    protected AirBubbleParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, float top, float size, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.top = top;
        this.xd = vx;
        this.yd = Math.max(0.01, vy);
        this.zd = vz;
        this.quadSize = size * (0.7F + random.nextFloat() * 0.6F);
        this.hasPhysics = false;
        this.gravity = 0;
        this.phase = random.nextDouble() * Math.PI * 2;
        double rise = Math.max(0.05, top - y);
        this.lifetime = (int) Math.min(600, Math.max(4, rise / this.yd * 1.4));

    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime || y >= top) {
            remove();
            return;
        }
        yd = Math.min(yd * 1.02, 0.25 * quadSize * 8 + 0.02);
        double w = Math.sin(age * 0.4 + phase) * quadSize * 0.15;
        move(xd * 0.9 + w, yd, zd * 0.9 - w);
        xd *= 0.9;
        zd *= 0.9;
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE; // like Minecraft's bubble: drawn before the tank water, seen through it
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<ModParticles.AirBubble> {
        @Override
        public Particle createParticle(ModParticles.AirBubble o, ClientLevel level, double x, double y, double z, double vx, double vy, double vz, RandomSource random) {
            return new AirBubbleParticle(level, x, y, z, vx, vy, vz, o.top(), o.size(), sprites.get(random));
        }
    }
}
