package org.overengineer.bettacraft.tankmates;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankWaterVolume;

/**
 * M19: the items around the tankmates: fish bags with a group of one species (from the net and the trader), fish bags
 * with a clutch of eggs taken out of a tank with the net (to raise them in a tank without adults), and the breeding box.
 */
public final class TankmateItems {
    /** Most tankmates in one fish bag. */
    public static final int MAX_PER_BAG = 12;

    private TankmateItems() {}

    // ------------------------------------------------------------------ bags

    /** A fish bag with {@code n} fish of a species (genes from the supplier, sexes alternating or random). */
    public static ItemStack groupBag(TankmateSpecies s, int n, java.util.function.Supplier<TankmateGenes> genes, java.util.random.RandomGenerator r,
                                     boolean pairs) {
        List<CompoundTag> fish = new ArrayList<>();
        for (int i = 0; i < Math.min(n, MAX_PER_BAG); i++) {
            boolean female = pairs ? i % 2 == 1 : r.nextBoolean();
            ItemStack tmp = TankmateBucketItem.withFish(new ItemStack(ModItems.FISH_BAG.get()), s, genes.get(), female, LifeStage.ADULT);
            fish.add(tmp.get(DataComponents.BUCKET_ENTITY_DATA).copyTag());
        }
        return groupBag(s, fish);
    }

    public static ItemStack groupBag(TankmateSpecies s, List<CompoundTag> fish) {
        ItemStack bag = new ItemStack(ModItems.FISH_BAG.get());
        bag.set(ModDataComponents.BAG_ENTITY.get(), BuiltInRegistries.ENTITY_TYPE.getKey(ModEntities.TANKMATES.get(s).get()));
        if (fish.size() == 1) {
            bag.set(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(fish.getFirst()));
        } else {
            bag.set(ModDataComponents.TANKMATE_GROUP.get(), List.copyOf(fish));
        }
        bag.set(DataComponents.MAX_STACK_SIZE, 1);
        return bag;
    }

    @Nullable
    public static TankmateSpecies bagSpecies(ItemStack stack) {
        if (stack.getItem() instanceof TankmateBucketItem b) return b.species(); // M22.3
        EntityType<?> t = FishBagItem.containedType(stack);
        return t == null ? null : TankmateSpecies.of(t);
    }

    /** The fish (bucket data) in a bag: the group, or the one fish. */
    public static List<CompoundTag> bagFish(ItemStack stack) {
        List<CompoundTag> g = stack.get(ModDataComponents.TANKMATE_GROUP.get());
        if (g != null && !g.isEmpty()) return g;
        CustomData cd = stack.get(DataComponents.BUCKET_ENTITY_DATA);
        return cd == null ? List.of() : List.of(cd.copyTag());
    }

    // ------------------------------------------------------------------ right-click on a tank

    /** Group bags, clutch bags and the breeding box on a tank; PASS for anything else. */
    public static ItemInteractionResult useOnTank(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        boolean box = stack.is(ModItems.BREEDING_BOX.get());
        // M22.3: a bucket with several fish of one species too
        boolean group = (stack.getItem() instanceof FishBagItem || stack.getItem() instanceof TankmateBucketItem) && stack.has(ModDataComponents.TANKMATE_GROUP.get());
        boolean clutch = stack.getItem() instanceof FishBagItem && stack.has(ModDataComponents.TANKMATE_CLUTCH.get());
        if (!box && !group && !clutch) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        ServerLevel server = (ServerLevel) level;
        TankBlockEntity c = TankMultiblock.getController(level, pos);
        if (c == null) {
            player.displayClientMessage(Component.translatable("message.bettacraft.tank.not_formed"), true);
            return ItemInteractionResult.CONSUME;
        }
        if (!TankInhabitants.canModify(player, c.getTank())) {
            player.displayClientMessage(Component.translatable("message.bettacraft.tank.locked"), true);
            return ItemInteractionResult.CONSUME;
        }
        Component msg;
        if (box) msg = installBox(server, c, player, stack);
        else if (group) msg = insertGroup(server, c, player, hand, stack);
        else msg = insertClutch(server, c, player, hand, stack);
        if (msg != null) player.displayClientMessage(msg, true);
        return ItemInteractionResult.CONSUME;
    }

    @Nullable
    private static Component installBox(ServerLevel level, TankBlockEntity c, Player player, ItemStack stack) {
        TankData t = c.getTank();
        if (t.breedingBox()) return Component.translatable("message.bettacraft.breeding_box.already");
        if (t.waterMb() <= 0) return Component.translatable("message.bettacraft.breeding_box.no_water");
        t.setBreedingBox(true);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, c.getBlockPos(), SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 0.8F, 1.3F);
        TankCare.log(level, c, "log.bettacraft.tm.box_in", TankCare.nameArg(player));
        c.markTankChanged();
        return Component.translatable("message.bettacraft.breeding_box.installed").withStyle(ChatFormatting.GREEN);
    }

    /** Takes the breeding box out (menu): the item back, the young in it swim free. */
    @Nullable
    public static Component removeBox(ServerLevel level, TankBlockEntity c, Player player) {
        TankData t = c.getTank();
        if (!t.breedingBox()) return Component.translatable("message.bettacraft.breeding_box.none");
        t.setBreedingBox(false);
        var r = level.getRandom();
        int freed = 0;
        for (Clutch k : new ArrayList<>(t.clutches())) {
            if (k.kind != Clutch.Kind.BOX) continue;
            t.clutches().remove(k);
            LifeStage st = k.age >= k.totalDays() * 0.5 ? LifeStage.FRY : LifeStage.FRY;
            for (int i = 0; i < k.count; i++) {
                Vec3 p = new TankWaterVolume(t).clamp(k.pos().add((r.nextDouble() - 0.5) * 0.3, -0.1, (r.nextDouble() - 0.5) * 0.3), 0.1, 0.1, 0.05);
                if (TankmateBreeding.spawn(level, c, k.species, org.overengineer.bettacraft.world.Wildlife.mutate(k.species, TankmateGenes.cross(k.mother, k.father, r::nextLong), r::nextLong), r.nextBoolean(), st, k.generation, p) != null) freed++;
            }
        }
        ItemStack item = new ItemStack(ModItems.BREEDING_BOX.get());
        if (!player.getInventory().add(item)) player.drop(item, false);
        level.playSound(null, c.getBlockPos(), SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 0.8F, 1.2F);
        TankCare.log(level, c, "log.bettacraft.tm.box_out", TankCare.nameArg(player), String.valueOf(freed));
        c.markTankChanged();
        return null;
    }

    /** Puts every fish of a group bag into the tank (as far as the stocking allows); the rest stays in the bag. */
    @Nullable
    private static Component insertGroup(ServerLevel level, TankBlockEntity c, Player player, InteractionHand hand, ItemStack stack) {
        TankmateSpecies s = bagSpecies(stack);
        if (s == null) return Component.translatable("message.bettacraft.tank.rejected", stack.getHoverName());
        EntityType<?> type = ModEntities.TANKMATES.get(s).get();
        List<CompoundTag> fish = new ArrayList<>(bagFish(stack));
        boolean bucket = stack.getItem() instanceof TankmateBucketItem; // M22.3: the water of a bucket comes along
        int in = 0;
        Component error = null;
        while (!fish.isEmpty()) {
            ItemStack single = new ItemStack(ModItems.FISH_BAG.get());
            single.set(ModDataComponents.BAG_ENTITY.get(), BuiltInRegistries.ENTITY_TYPE.getKey(type));
            single.set(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(fish.getFirst()));
            error = TankInhabitants.insert(level, c, single, type, player, in == 0 ? (bucket ? TankInhabitants.BUCKET_MB : TankInhabitants.BAG_MB) : 0L);
            if (error != null) break;
            fish.removeFirst();
            in++;
        }
        if (in > 0) level.playSound(null, c.getBlockPos(), SoundEvents.BUCKET_EMPTY_FISH, SoundSource.BLOCKS, 1.0F, 1.0F);
        ItemStack rest = fish.isEmpty() ? new ItemStack(bucket ? net.minecraft.world.item.Items.BUCKET : ModItems.FISH_BAG.get())
                : org.overengineer.bettacraft.containers.AnimalContainers.withFish(stack, s, fish);
        player.setItemInHand(hand, player.getAbilities().instabuild && fish.isEmpty() ? stack : rest);
        if (error != null && in == 0) return error;
        if (!fish.isEmpty()) return Component.translatable("message.bettacraft.tankmates.some_in", in, fish.size()).withStyle(ChatFormatting.GOLD);
        return Component.translatable("message.bettacraft.tankmates.all_in", in, Component.translatable(s.translationKey())).withStyle(ChatFormatting.GREEN);
    }

    /** A clutch from the net goes on the bottom of this tank (raised without the parents: no guarding male). */
    @Nullable
    private static Component insertClutch(ServerLevel level, TankBlockEntity c, Player player, InteractionHand hand, ItemStack stack) {
        Clutch old = Clutch.load(stack.get(ModDataComponents.TANKMATE_CLUTCH.get()));
        if (old == null) return Component.translatable("message.bettacraft.tankmates.clutch_bad");
        TankData t = c.getTank();
        if (t.waterMb() <= 0) return Component.translatable("message.bettacraft.breeding_box.no_water");
        TankWaterVolume w = new TankWaterVolume(t);
        var r = level.getRandom();
        Vec3 p = w.clamp(new Vec3(w.box().minX + r.nextDouble() * (w.box().maxX - w.box().minX), w.box().minY + 0.03,
                w.box().minZ + r.nextDouble() * (w.box().maxZ - w.box().minZ)), 0.15, 0.03, 0.02);
        Clutch k = new Clutch(t.nextClutchId(), old.species, Clutch.Kind.SCATTER, p.x, p.y, p.z, old.count, old.mother, old.father);
        k.age = old.age;
        k.generation = old.generation;
        k.cover = Math.max(old.cover, 0.5);
        t.clutches().add(k);
        TankInteractions.changeWater(level, c, TankInhabitants.BAG_MB, c.getTank().salinity()); // M20
        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(ModItems.FISH_BAG.get())));
        level.playSound(null, c.getBlockPos(), SoundEvents.BUCKET_EMPTY_FISH, SoundSource.BLOCKS, 0.8F, 1.3F);
        TankCare.log(level, c, "log.bettacraft.tm.clutch_in", "@" + k.species.translationKey(), String.valueOf(k.count));
        c.markTankChanged();
        return Component.translatable("message.bettacraft.tankmates.clutch_in", k.count, Component.translatable(k.species.translationKey()))
                .withStyle(ChatFormatting.GREEN);
    }

    // ------------------------------------------------------------------ the net on a clutch

    public record Aimed(Clutch clutch, double distance) {}

    /** The clutch the player looks at (eggs on the glass, in plants, a cave or a foam nest), or null. */
    @Nullable
    public static Aimed aimed(TankData t, Vec3 eye, Vec3 look) {
        Aimed best = null;
        for (Clutch k : t.clutches()) {
            if (k.kind == Clutch.Kind.BOX || !k.hasYoung()) continue;
            Vec3 to = k.pos().subtract(eye);
            double along = to.dot(look);
            if (along <= 0 || along > 6) continue;
            double off = to.subtract(look.scale(along)).length();
            double radius = k.kind == Clutch.Kind.FOAM ? 0.3 : 0.14;
            if (off > radius) continue;
            if (best == null || along < best.distance) best = new Aimed(k, along);
        }
        return best;
    }

    /** The net takes the clutch into an empty fish bag of the player. */
    @Nullable
    public static Component net(ServerLevel level, TankBlockEntity c, Player player, Clutch k) {
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
        TankData t = c.getTank();
        t.clutches().remove(k);
        ItemStack bag = new ItemStack(ModItems.FISH_BAG.get());
        bag.set(ModDataComponents.TANKMATE_CLUTCH.get(), k.save());
        bag.set(DataComponents.MAX_STACK_SIZE, 1);
        if (!player.getAbilities().instabuild) inv.getItem(slot).shrink(1);
        if (!inv.add(bag)) player.drop(bag, false);
        level.playSound(null, c.getBlockPos(), SoundEvents.BUCKET_FILL_FISH, SoundSource.BLOCKS, 0.8F, 1.4F);
        TankCare.log(level, c, "log.bettacraft.tm.clutch_out", TankCare.nameArg(player), "@" + k.species.translationKey(), String.valueOf(k.count));
        c.markTankChanged();
        return Component.translatable("message.bettacraft.tankmates.clutch_out", k.count, Component.translatable(k.species.translationKey()))
                .withStyle(ChatFormatting.GREEN);
    }

    // ------------------------------------------------------------------ bag texts

    /** Name of a filled bag with tankmates or a clutch, or null. */
    @Nullable
    public static Component bagName(ItemStack stack) {
        CompoundTag cl = stack.get(ModDataComponents.TANKMATE_CLUTCH.get());
        if (cl != null) {
            Clutch k = Clutch.load(cl);
            if (k != null) return Component.translatable("item.bettacraft.fish_bag.clutch", k.count, Component.translatable(k.species.translationKey()));
        }
        List<CompoundTag> g = stack.get(ModDataComponents.TANKMATE_GROUP.get());
        TankmateSpecies s = bagSpecies(stack);
        if (g != null && !g.isEmpty() && s != null) {
            return Component.translatable("item.bettacraft.fish_bag.critters", g.size(), Component.translatable(s.translationKey()));
        }
        return null;
    }

    /** Tooltip of a bag with tankmates (the fish, at most six lines) or a clutch; false if it holds neither. */
    public static boolean appendBag(ItemStack stack, List<Component> tooltip) {
        CompoundTag cl = stack.get(ModDataComponents.TANKMATE_CLUTCH.get());
        if (cl != null) {
            Clutch k = Clutch.load(cl);
            if (k == null) return false;
            tooltip.add(Component.translatable(k.stage() == 0 ? "tooltip.bettacraft.tankmate.clutch_eggs" : "tooltip.bettacraft.tankmate.clutch_larvae",
                    String.format(java.util.Locale.ROOT, "%.1f", k.daysLeft())).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.bettacraft.tankmate.clutch_hint").withStyle(ChatFormatting.DARK_GRAY));
            return true;
        }
        TankmateSpecies s = bagSpecies(stack);
        if (s == null) return false;
        List<CompoundTag> fish = bagFish(stack);
        if (fish.size() <= 1) {
            TankmateBucketItem.append(s, fish.isEmpty() ? null : fish.getFirst().getCompound(TankmateEntity.KEY), tooltip);
            if (!fish.isEmpty()) TankmateBucketItem.genesLine(s, fish.getFirst(), tooltip); // M22.3
            return true;
        }
        tooltip.add(Component.literal(s.latin()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        int females = 0;
        for (CompoundTag f : fish) if (f.getCompound(TankmateEntity.KEY).getBoolean("female")) females++;
        tooltip.add(Component.translatable("tooltip.bettacraft.tankmate.group", fish.size() - females, females).withStyle(ChatFormatting.GRAY));
        if (org.overengineer.bettacraft.config.CommonConfig.bigContainers()) {
            // M22.3: every fish with all its traits (sex, stage, colour, genes when analysed)
            TankmateBucketItem.appendEach(s, fish, tooltip);
            return true;
        }
        if (TankmateGenes.livebearer(s) && org.overengineer.bettacraft.config.CommonConfig.livebearerGenetics()) {
            // M21.6: the strains in the bag
            java.util.Map<String, Integer> v = new java.util.LinkedHashMap<>();
            java.util.Map<String, Component> names = new java.util.HashMap<>();
            for (CompoundTag f : fish) {
                Component n = TankmateBucketItem.livebearerName(s, f.getCompound(TankmateEntity.KEY));
                if (n == null) continue;
                v.merge(n.getString(), 1, Integer::sum);
                names.putIfAbsent(n.getString(), n);
            }
            int lines = 0;
            for (var e : v.entrySet()) {
                if (lines++ >= 6) break;
                tooltip.add(Component.literal(e.getValue() + " × ").append(names.get(e.getKey())).withStyle(ChatFormatting.GOLD));
            }
        } else if (s.varieties().size() > 1) {
            java.util.Map<String, Integer> v = new java.util.LinkedHashMap<>();
            for (CompoundTag f : fish) v.merge(TankmateGenes.load(f.getCompound(TankmateEntity.KEY).getCompound("genes"), s).phenotype(s), 1, Integer::sum);
            for (var e : v.entrySet()) tooltip.add(Component.literal(e.getValue() + " × ").append(Component.translatable(s.varietyKey(e.getKey())))
                    .withStyle(ChatFormatting.GOLD));
        }
        return true;
    }

    /** Releases a group bag (or bucket) into open water: every fish swims off. */
    public static int release(ServerLevel level, ItemStack stack, BlockPos pos, @Nullable Player player) {
        TankmateSpecies s = bagSpecies(stack);
        if (s == null) return 0;
        int n = 0;
        for (CompoundTag f : bagFish(stack)) {
            TankmateEntity e = ModEntities.TANKMATES.get(s).get().create(level);
            if (e == null) continue;
            e.moveTo(pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.4, pos.getY() + 0.2, pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.4,
                    player == null ? level.random.nextFloat() * 360F : player.getYRot(), 0);
            e.loadFromBucketTag(f);
            e.setFromBucket(true);
            if (level.addFreshEntity(e)) n++;
        }
        return n;
    }
}
