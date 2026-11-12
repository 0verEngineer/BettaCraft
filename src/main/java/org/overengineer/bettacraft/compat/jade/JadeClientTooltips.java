package org.overengineer.bettacraft.compat.jade;

import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.BettaTooltip;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.genetics.GenotypeText;
import org.overengineer.bettacraft.tank.TankCare;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Client side of the Jade integration: tank and betta tooltips (PLAN 10, OFFEN 33). */
final class JadeClientTooltips {
    private JadeClientTooltips() {}

    private static String f1(float f) {
        return String.format(Locale.ROOT, "%.1f", f);
    }

    private static Component pct(float f) {
        int p = Math.round(f * 100);
        return Component.literal(p + "%").withStyle(p > 50 ? ChatFormatting.GREEN : p > 25 ? ChatFormatting.GOLD : ChatFormatting.RED);
    }

    /** If the view ray hits a tank block but a betta inside the tank is behind the glass, show that betta instead. */
    static snownee.jade.api.Accessor<?> retarget(snownee.jade.api.IWailaClientRegistration reg, snownee.jade.api.Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor block) || !(block.getBlock() instanceof org.overengineer.bettacraft.tank.TankBlock)) return accessor;
        net.minecraft.world.entity.player.Player player = accessor.getPlayer();
        net.minecraft.world.level.Level level = accessor.getLevel();
        if (player == null || level == null) return accessor;
        net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
        net.minecraft.world.phys.Vec3 end = eye.add(player.getViewVector(1.0F).scale(player.entityInteractionRange() + 3.0));
        net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(eye, end).inflate(1.0);
        net.minecraft.world.phys.EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level, player,
                eye, end, area, e -> e instanceof BettaEntity && e.isAlive(), 0.0F);
        if (hit == null) return accessor;
        return reg.entityAccessor().level(level).player(player).hit(hit).entity(hit.getEntity())
                .serverConnected(accessor.isServerConnected()).showDetails(accessor.showDetails()).requireVerification().build();
    }

    static final class Tank implements IBlockComponentProvider {
        static final Tank INSTANCE = new Tank();

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag d = accessor.getServerData().getCompound("bettacraft");
            if (d.isEmpty()) return;
            if (!d.getString("name").isEmpty()) tooltip.add(Component.literal(d.getString("name")).withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.translatable("jade.bettacraft.tank.water", f1(d.getFloat("temp")), d.getLong("water"), d.getLong("cap")));
            MutableComponent animals = Component.translatable("jade.bettacraft.tank.animals", d.getInt("count"), d.getInt("adults"));
            if (d.getBoolean("crowded")) animals.append(" ").append(Component.translatable("gui.bettacraft.tank.crowded").withStyle(ChatFormatting.RED));
            tooltip.add(animals);
            String nest = d.getString("nest");
            if (!"none".equals(nest) && !nest.isEmpty()) {
                tooltip.add(Component.translatable("jade.bettacraft.tank.nest", Component.translatable("jade.bettacraft.nest." + nest, d.getInt("eggs"))));
            }
            ListTag warn = d.getList("warnings", Tag.TAG_STRING);
            for (int i = 0; i < warn.size(); i++) {
                tooltip.add(Component.translatable("gui.bettacraft.breeding.warn." + warn.getString(i)).withStyle(ChatFormatting.RED));
            }
            if (accessor.showDetails()) {
                ListTag list = d.getList("list", Tag.TAG_COMPOUND);
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag e = list.getCompound(i);
                    MutableComponent line = Component.literal("  ");
                    if (e.contains("sex")) line.append("female".equals(e.getString("sex")) ? "♀ " : "♂ ");
                    line.append(TankCare.argComponent(e.getString("name")));
                    if (e.contains("stage") && !LifeStage.ADULT.id().equals(e.getString("stage"))) {
                        line.append(" (").append(Component.translatable(LifeStage.byId(e.getString("stage")).translationKey())).append(")");
                    }
                    line.append(" ").append(pct(e.getFloat("health")));
                    if (e.getBoolean("sick")) line.append(" ").append(Component.translatable("gui.bettacraft.fish.sick").withStyle(ChatFormatting.RED));
                    tooltip.add(line);
                }
            } else if (d.getInt("count") > 0) {
                tooltip.add(Component.translatable("jade.bettacraft.sneak").withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return BettaCraftJadePlugin.TANK;
        }
    }

    static final class Betta implements IEntityComponentProvider {
        static final Betta INSTANCE = new Betta();

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (!(accessor.getEntity() instanceof BettaEntity b)) return;
            CompoundTag d = accessor.getServerData().getCompound("bettacraft");
            MutableComponent stage = Component.translatable(b.lifeStage().translationKey());
            if (!b.isAdult() && d.contains("progress")) {
                stage = Component.translatable("gui.bettacraft.fish.stage_progress", stage, Math.round(d.getFloat("progress") * 100));
            }
            tooltip.add(Component.translatable("jade.bettacraft.betta.type", BettaTooltip.shape(b.genome())));
            tooltip.add(Component.translatable("gui.bettacraft.fish.sex_stage", BettaTooltip.sex(b.isFemale()), stage));
            if (d.isEmpty()) return;
            if (d.getBoolean("in_tank")) {
                MutableComponent h = Component.translatable("jade.bettacraft.betta.health", pct(d.getFloat("health")), pct(d.getFloat("satiation")));
                if (d.getBoolean("sick")) h.append(" ").append(Component.translatable("gui.bettacraft.fish.sick").withStyle(ChatFormatting.RED));
                else if (d.getBoolean("hungry")) h.append(" ").append(Component.translatable("jade.bettacraft.betta.hungry").withStyle(ChatFormatting.GOLD));
                tooltip.add(h);
            }
            if (d.contains("genotype", Tag.TAG_COMPOUND)) {
                BettaGenotype g = BettaGenotype.load(d.getCompound("genotype"));
                if (accessor.showDetails()) {
                    for (Component c : GenotypeText.summary(g)) tooltip.add(c);
                    tooltip.add(Component.translatable("gui.bettacraft.fish.generation", d.getInt("generation")).withStyle(ChatFormatting.GRAY));
                } else {
                    tooltip.add(GenotypeText.summary(g).getFirst());
                }
            }
        }

        @Override
        public ResourceLocation getUid() {
            return BettaCraftJadePlugin.BETTA;
        }
    }
}
