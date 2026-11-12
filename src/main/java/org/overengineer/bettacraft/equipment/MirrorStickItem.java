package org.overengineer.bettacraft.equipment;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.entity.betta.BettaCare;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.progression.Progress;

/**
 * Mirror stick (M11, optional-features #2): while a player holds it (either hand), every adult male betta within
 * {@link #RANGE} blocks that the player looks at (within {@link #HALF_ANGLE_COS} of the view direction) and that can
 * see the mirror (glass and water do not block, see {@link BettaCare#canSee}) flares at it: fins spread, it holds still
 * and turns towards the mirror. Purely cosmetic, except for the "mirror training" of the breeding state
 * (config {@code breeding.mirrorReadinessBonus}, 1 = off). Runs only on the server.
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public class MirrorStickItem extends Item {
    public static final double RANGE = 6.0;
    /** cos(50°): the fish must be roughly in front of the player. */
    public static final double HALF_ANGLE_COS = Math.cos(Math.toRadians(50));
    private static final int INTERVAL = 10;
    /** A flare lasts a little longer than the check interval, so it stays on while the mirror is held up. */
    private static final int HOLD = 40;

    public MirrorStickItem(Properties properties) {
        super(properties);
    }

    public static boolean holds(Player p) {
        return p.getMainHandItem().getItem() instanceof MirrorStickItem || p.getOffhandItem().getItem() instanceof MirrorStickItem;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.isSpectator()) return;
        if ((p.tickCount + p.getId()) % INTERVAL != 0 || !holds(p)) return;
        int n = flareNear(p.serverLevel(), p);
        if (n > 0) Progress.award(p, Progress.MIRROR);
    }

    /** Lets every adult male in front of {@code p} flare at the mirror; returns how many did. */
    public static int flareNear(ServerLevel level, Player p) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        long now = level.getGameTime();
        int n = 0;
        AABB area = new AABB(eye, eye).inflate(RANGE);
        for (BettaEntity b : level.getEntitiesOfClass(BettaEntity.class, area, o -> o.isAlive() && o.isAdult() && !o.isFemale())) {
            Vec3 to = b.getEyePosition().subtract(eye);
            double len = to.length();
            if (len > RANGE || len < 1e-3) continue;
            if (to.scale(1 / len).dot(look) < HALF_ANGLE_COS) continue;
            if (!BettaCare.canSee(level, eye, b.getEyePosition())) continue;
            b.flareAtMirror(eye, now + HOLD);
            n++;
        }
        return n;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.bettacraft.mirror_stick").withStyle(ChatFormatting.GRAY));
    }
}
