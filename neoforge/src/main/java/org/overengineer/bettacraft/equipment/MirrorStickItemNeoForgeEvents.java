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
import static org.overengineer.bettacraft.equipment.MirrorStickItem.*;

/** M52: the NeoForge event handlers of {@link MirrorStickItem} (moved here from the common class). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
final class MirrorStickItemNeoForgeEvents {
    private MirrorStickItemNeoForgeEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.isSpectator()) return;
        if ((p.tickCount + p.getId()) % INTERVAL != 0 || !holds(p)) return;
        int n = flareNear(p.serverLevel(), p);
        if (n > 0) Progress.award(p, Progress.MIRROR);
    }
}
