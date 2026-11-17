package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Spider;
import org.joml.Vector3f;
import org.overengineer.bettacraft.BettaCraft;

/**
 * M31 part d: a spider in glass ({@link org.overengineer.bettacraft.mobs.GlassMobs}, shrunk) that climbs the glass of its
 * tank lies with its belly on the glass, head up, like the climbing geckos ({@link HerpRenderer}); a vanilla spider climbs
 * upright. The walker turns it towards the wall and sets the synced climbing flag; the client only sees the small scale.
 */
public final class GlassMobRendering {
    /** Below this scale a spider is one in glass (wild ones are 1; the cave spider in glass 0.3). */
    static final float GLASS_SCALE = 0.6F;
    static final Set<LivingEntity> TURNED = Collections.newSetFromMap(new IdentityHashMap<>());

    private GlassMobRendering() {}


}
