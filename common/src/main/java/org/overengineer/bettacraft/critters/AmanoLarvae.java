package org.overengineer.bettacraft.critters;

import java.util.random.RandomGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankWaterVolume;

/**
 * M49 part b: the larvae of the Amano shrimp ({@link CritterSpecies#AMANO_SHRIMP}, switch {@code features.brackish.amanoShrimp}).
 * The shrimp mate in fresh water; when the eggs of a berried female are ripe she lets out a cloud of larvae
 * ({@link CritterObject.Kind#LARVAE}). In fresh water they die after {@code brackish.amanoFreshLarvaDays}: the aquarium net
 * catches the cloud into an empty fish bag, and the bag on a brackish (or salt water) tank lets them out there. After
 * {@code brackish.amanoLarvaDays} in brackish water a share of them ({@code brackish.amanoLarvaSurvival}, twice as many with
 * phytoplankton in the water, at most 80 %) settle as young shrimp; take those back into fresh water.
 */
public final class AmanoLarvae {
    private AmanoLarvae() {}

    public static boolean enabled() {
        return Features.enabled(Feature.AMANO_SHRIMP);
    }

    /** Pure: the species whose eggs hatch into larvae instead of young shrimp. */
    public static boolean hasLarvae(CritterSpecies s) {
        return s == CritterSpecies.AMANO_SHRIMP;
    }

    /** Pure: the Amanos mate (and carry eggs) in fresh water only. */
    public static boolean matesIn(double salinity) {
        return Salinity.kind(salinity) == Salinity.Kind.FRESH;
    }

    /** Pure: brackish or salt water, where the larvae grow up. */
    public static boolean grows(double salinity) {
        return Salinity.kind(salinity) != Salinity.Kind.FRESH;
    }

    /** Pure: the share that settles: the survival, doubled with phytoplankton, at most 80 %. */
    public static double survival(double base, boolean phytoplankton) {
        return Math.max(0, Math.min(0.8, base * (phytoplankton ? 2.0 : 1.0)));
    }

    /** The cloud of larvae of a female whose eggs are ripe. */
    public static CritterObject hatch(Critter mother, int id, RandomGenerator rnd) {
        CritterObject o = new CritterObject(id, CritterObject.Kind.LARVAE, mother.x, mother.y + 0.15, mother.z);
        CritterSpecies s = mother.species;
        o.mother = mother.genes;
        o.father = mother.father != null ? mother.father : mother.genes;
        o.count = s.broodMin() + rnd.nextInt(s.broodMax() - s.broodMin() + 1);
        o.generation = mother.generation + 1;
        o.species = s;
        o.yaw = (float) (rnd.nextDouble() * 360);
        return o;
    }

    public static double larvaDays() {
        return ServerConfig.getOr(ServerConfig.AMANO_LARVA_DAYS, 3.0);
    }

    public static double freshDays() {
        return ServerConfig.getOr(ServerConfig.AMANO_FRESH_LARVA_DAYS, 1.0);
    }

    /** The care step (after the small animals' step, which ages the larvae): they die in fresh water or settle in brackish water. */
    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        TankData t = c.getTank();
        if (t.critterObjects().isEmpty() || days <= 0) return;
        boolean changed = false;
        var rnd = NetCatching.random(level);
        for (var it = t.critterObjects().iterator(); it.hasNext(); ) {
            CritterObject o = it.next();
            if (o.kind != CritterObject.Kind.LARVAE) continue;
            boolean brackish = t.waterMb() > 0 && grows(t.salinity());
            if (!enabled() || t.waterMb() <= 0 || !brackish && o.age >= freshDays()) {
                it.remove();
                changed = true;
                TankCare.log(level, c, "log.bettacraft.amano.larvae_died", String.valueOf(o.count));
                continue;
            }
            // they drift a little in the water
            o.y = Math.max(t.min().getY() + 0.1, Math.min(t.waterSurfaceY() - 0.1, o.y + (rnd.nextDouble() - 0.5) * 0.02));
            if (!brackish || o.age < larvaDays()) continue;
            it.remove();
            changed = true;
            double share = survival(ServerConfig.getOr(ServerConfig.AMANO_LARVA_SURVIVAL, 0.35), t.phytoplankton() > 1e-6);
            int n = 0;
            for (int i = 0; i < o.count; i++) if (rnd.nextDouble() < share) n++;
            n = Math.min(n, Math.max(0, 60 - t.critters().size()));
            TankWaterVolume w = new TankWaterVolume(t);
            for (int i = 0; i < n; i++) {
                CritterGenes g = o.mother != null && o.father != null ? CritterGenes.cross(o.mother, o.father, o.species.loci().length, rnd) : CritterGenes.WILD;
                Vec3 p = w.clamp(new Vec3(o.x + (rnd.nextDouble() - 0.5) * 0.6, t.min().getY() + 0.05, o.z + (rnd.nextDouble() - 0.5) * 0.6), 0.1, 0.02, 0.1);
                Critter k = new Critter(t.nextCritterId(), o.species, rnd.nextBoolean(), g, p.x, p.y, p.z);
                k.age = 0;
                k.generation = o.generation;
                k.yaw = (float) (rnd.nextDouble() * 360);
                k.look = CritterLooks.random(k.species, rnd);
                t.critters().add(k);
            }
            TankCare.log(level, c, n > 0 ? "log.bettacraft.amano.young" : "log.bettacraft.amano.none_settled", String.valueOf(n), String.valueOf(o.count));
        }
        if (changed) {
            c.markTankChanged();
            TankCritters.sync(level, c);
        }
    }

    /** Larvae clouds in this tank: {clouds, larvae}. */
    public static int[] count(TankData t) {
        int clouds = 0, n = 0;
        for (CritterObject o : t.critterObjects()) {
            if (o.kind != CritterObject.Kind.LARVAE) continue;
            clouds++;
            n += o.count;
        }
        return new int[]{clouds, n};
    }

    // ------------------------------------------------------------------ the net and the bag

    /** The net on a cloud of larvae: into an empty fish bag of the player. */
    public static Component net(ServerLevel level, TankBlockEntity c, Player player, CritterObject o) {
        var inv = player.getInventory();
        int slot = -1;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof FishBagItem && !FishBagItem.isFilled(s)) {
                slot = i;
                break;
            }
        }
        if (slot < 0) return Component.translatable("message.bettacraft.tankmates.need_bag").withStyle(ChatFormatting.GOLD);
        c.getTank().critterObjects().remove(o);
        ItemStack bag = new ItemStack(ModItems.FISH_BAG.get());
        bag.set(ModDataComponents.CRITTER_LARVAE.get(), o.save());
        bag.set(DataComponents.MAX_STACK_SIZE, 1);
        if (!player.getAbilities().instabuild) inv.getItem(slot).shrink(1);
        if (!inv.add(bag)) player.drop(bag, false);
        level.playSound(null, c.getBlockPos(), SoundEvents.BUCKET_FILL_FISH, SoundSource.BLOCKS, 0.8F, 1.5F);
        TankCare.log(level, c, "log.bettacraft.amano.larvae_out", TankCare.nameArg(player), String.valueOf(o.count));
        c.markTankChanged();
        TankCritters.sync(level, c);
        return Component.translatable("message.bettacraft.amano.larvae_out", o.count).withStyle(ChatFormatting.GREEN);
    }

    /** A bag of larvae on a tank: they drift in its water (they keep their age). */
    public static InteractionResult useOnTank(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!(stack.getItem() instanceof FishBagItem) || !stack.has(ModDataComponents.CRITTER_LARVAE.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        TankBlockEntity c = TankMultiblock.getController(level, pos);
        if (c == null) {
            player.sendOverlayMessage(Component.translatable("message.bettacraft.tank.not_formed"));
            return InteractionResult.CONSUME;
        }
        if (!TankInhabitants.canModify(player, c.getTank())) {
            player.sendOverlayMessage(Component.translatable("message.bettacraft.tank.locked"));
            return InteractionResult.CONSUME;
        }
        TankData t = c.getTank();
        if (t.waterMb() <= 0) {
            player.sendOverlayMessage(Component.translatable("message.bettacraft.breeding_box.no_water").withStyle(ChatFormatting.GOLD));
            return InteractionResult.CONSUME;
        }
        CritterObject o = CritterObject.load(stack.get(ModDataComponents.CRITTER_LARVAE.get()));
        if (o == null || o.kind != CritterObject.Kind.LARVAE) return InteractionResult.CONSUME;
        Component msg = insert((ServerLevel) level, c, o, player);
        if (!player.getAbilities().instabuild) player.setItemInHand(hand, new ItemStack(ModItems.FISH_BAG.get()));
        player.sendOverlayMessage(msg);
        return InteractionResult.CONSUME;
    }

    /** Lets a cloud of larvae out into the tank (keeps its age); the message for the player. */
    public static Component insert(ServerLevel level, TankBlockEntity c, CritterObject o, Player player) {
        TankData t = c.getTank();
        TankWaterVolume w = new TankWaterVolume(t);
        Vec3 mid = w.clamp(new Vec3((t.min().getX() + t.max().getX() + 1) / 2.0, (t.min().getY() + t.waterSurfaceY()) / 2.0,
                (t.min().getZ() + t.max().getZ() + 1) / 2.0), 0.1, 0.1, 0.1);
        CritterObject in = new CritterObject(t.nextCritterId(), CritterObject.Kind.LARVAE, mid.x, mid.y, mid.z);
        in.mother = o.mother;
        in.father = o.father;
        in.count = o.count;
        in.generation = o.generation;
        in.species = o.species;
        in.age = o.age;
        t.critterObjects().add(in);
        level.playSound(null, c.getBlockPos(), SoundEvents.BUCKET_EMPTY_FISH, SoundSource.BLOCKS, 0.8F, 1.5F);
        TankCare.log(level, c, "log.bettacraft.amano.larvae_in", TankCare.nameArg(player), String.valueOf(o.count));
        c.markTankChanged();
        TankCritters.sync(level, c);
        return Component.translatable(grows(t.salinity()) ? "message.bettacraft.amano.larvae_in" : "message.bettacraft.amano.larvae_in_fresh", o.count)
                .withStyle(grows(t.salinity()) ? ChatFormatting.GREEN : ChatFormatting.GOLD);
    }
}
