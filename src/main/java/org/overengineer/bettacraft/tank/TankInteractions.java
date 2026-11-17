package org.overengineer.bettacraft.tank;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/** Item interactions with a tank: water bucket fills, empty bucket drains (PLAN 3.2). */
public final class TankInteractions {
    private TankInteractions() {}

    public static ItemInteractionResult useItemOn(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand,
                                                  net.minecraft.world.phys.BlockHitResult hit) {
        // M21.5: a clip-on feeder in the hand goes onto the side of the glass (no tank menu)
        if (stack.is(org.overengineer.bettacraft.registry.ModItems.CLIP_FEEDER.get())) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        ItemInteractionResult r = org.overengineer.bettacraft.genetics.MagnifierItem.useOnTank(stack, level, pos, player, hand); // M22.3
        if (r != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return r;
        r = org.overengineer.bettacraft.critters.TankCritters.tryInsert(stack, level, pos, player, hand); // M14
        if (r != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return r;
        r = org.overengineer.bettacraft.tankmates.TankmateItems.useOnTank(stack, level, pos, player, hand); // M19: groups, clutches, breeding box
        if (r != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return r;
        r = TankInhabitants.tryInsert(stack, level, pos, player, hand);
        if (r != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return r;
        r = org.overengineer.bettacraft.big.DolphinCare.feed(stack, level, pos, player, hand); // M30: raw fish for the dolphins
        if (r != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return r;
        r = org.overengineer.bettacraft.foreign.ForeignFood.handFeed(stack, level, pos, player, hand); // M35: greens, meat for the animals of other mods
        if (r != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return r;
        r = org.overengineer.bettacraft.mobs.GlassMobs.handFeed(stack, level, pos, player, hand); // M31 part d: food for the mobs in glass, or bait
        if (r != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return r;
        r = org.overengineer.bettacraft.tankmates.Archerfish.useOnTank(stack, level, pos, player, hand); // M49: fruit flies over the archerfish
        if (r != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return r;
        r = org.overengineer.bettacraft.terrarium.TerrariumFood.handFeed(stack, level, pos, player, hand); // M31 part b: greens for the turtles
        if (r != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return r;
        r = TankCare.handFeed(stack, level, pos, player, hand, hit.getLocation());
        if (r != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return r;
        r = org.overengineer.bettacraft.foreign.MinecraftPets.handBait(stack, level, pos, player, hand); // M39 part c: bait for the Minecraft pets
        if (r != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return r;
        r = org.overengineer.bettacraft.reef.ReefItems.useOnTank(stack, level, pos, player, hand); // M20: sea salt, calcium
        if (r != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return r;
        r = org.overengineer.bettacraft.reef.ClownLarvae.useOnTank(stack, level, pos, player, hand); // M48 part b: phytoplankton
        if (r != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return r;
        r = org.overengineer.bettacraft.critters.AmanoLarvae.useOnTank(stack, level, pos, player, hand); // M49: a bag of Amano larvae
        if (r != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return r;
        r = org.overengineer.bettacraft.chem.ChemItem.useOnTank(stack, level, pos, player, hand); // M25: measuring, salt, leaves
        if (r != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return r;
        r = org.overengineer.bettacraft.fertilizer.Fertilizer.useOnTank(stack, level, pos, player, hand); // M37: a portion of fertilizer
        if (r != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return r;
        boolean sea = stack.is(org.overengineer.bettacraft.registry.ModItems.SEA_WATER_BUCKET.get());
        boolean ro = stack.is(org.overengineer.bettacraft.registry.ModItems.RO_WATER_BUCKET.get()); // M25
        boolean fill = stack.is(Items.WATER_BUCKET) || sea || ro;
        boolean drain = stack.is(Items.BUCKET);
        if (!fill && !drain) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        ServerLevel server = (ServerLevel) level;
        TankBlockEntity controller = TankMultiblock.getController(level, pos);
        if (controller == null) {
            return ItemInteractionResult.CONSUME;
        }
        TankData tank = controller.getTank();
        if (!TankInhabitants.canModify(player, tank)) {
            player.displayClientMessage(Component.translatable("message.bettacraft.tank.locked"), true);
            return ItemInteractionResult.CONSUME;
        }
        if (fill) {
            // M33.1: a fixed terrarium takes no water, a fixed paludarium only up to its land
            if (org.overengineer.bettacraft.terrarium.WaterLimit.blocks(server, controller, TankData.MB_PER_BLOCK)) {
                org.overengineer.bettacraft.terrarium.WaterLimit.tell(server, controller, player);
                return ItemInteractionResult.CONSUME;
            }
            if (tank.waterMb() + TankData.MB_PER_BLOCK > tank.capacityMb()) {
                player.displayClientMessage(Component.translatable("message.bettacraft.tank.full"), true);
                return ItemInteractionResult.CONSUME;
            }
            float qualityBefore = tank.waterQuality();
            boolean hadWater = tank.waterMb() > 0;
            if (ro) {
                // M25: reverse osmosis water: a water change with nothing dissolved (TDS 0, pH 6.8)
                changeWater(server, controller, TankData.MB_PER_BLOCK, 0.0F, org.overengineer.bettacraft.chem.WaterChemistry.RO_TDS,
                        org.overengineer.bettacraft.chem.WaterChemistry.RO_PH);
            } else {
                changeWater(server, controller, TankData.MB_PER_BLOCK, sea ? Salinity.SEA : 0.0F);
            }
            if (hadWater && qualityBefore < WaterQuality.GOOD_FROM && tank.waterQuality() > qualityBefore) {
                org.overengineer.bettacraft.progression.Progress.award(player, org.overengineer.bettacraft.progression.Progress.WATER_CHANGE,
                        Math.round(tank.waterQuality() * 100));
            }
            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
            }
            level.playSound(null, pos, org.overengineer.bettacraft.registry.ModSounds.TANK_FILL.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            org.overengineer.bettacraft.progression.Progress.award(player, org.overengineer.bettacraft.progression.Progress.TANK_FILLED, (int) (100 * tank.waterMb() / Math.max(1, tank.capacityMb())));
            level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
        } else {
            if (tank.waterMb() < TankData.MB_PER_BLOCK) {
                player.displayClientMessage(Component.translatable("message.bettacraft.tank.not_enough_water"), true);
                return ItemInteractionResult.CONSUME;
            }
            double minFill = org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.TANK_MIN_FILL_TO_INSERT, 0.5);
            if (tank.waterMb() - TankData.MB_PER_BLOCK < minFill * tank.capacityMb() && !TankInhabitants.liveInhabitants(server, tank).isEmpty()) {
                // keep the inhabitants in water
                player.displayClientMessage(Component.translatable("message.bettacraft.tank.keep_water", Math.round(minFill * 100)), true);
                return ItemInteractionResult.CONSUME;
            }
            boolean salty = tank.marine();
            changeWater(server, controller, -TankData.MB_PER_BLOCK);
            // M20: salt water comes out as a bucket of sea water
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(salty
                    ? org.overengineer.bettacraft.registry.ModItems.SEA_WATER_BUCKET.get() : Items.WATER_BUCKET)));
            level.playSound(null, pos, org.overengineer.bettacraft.registry.ModSounds.TANK_DRAIN.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        }
        return ItemInteractionResult.CONSUME;
    }

    /**
     * Adds (or removes, if negative) water and updates the block states bottom-up. Added water is fresh water: it
     * dilutes the waste (M9 water quality, a water change = draining and refilling); removing water keeps the quality.
     */
    public static void changeWater(ServerLevel level, TankBlockEntity controller, long deltaMb) {
        changeWater(level, controller, deltaMb, 0.0F);
    }

    /**
     * M20: adds water of salinity {@code addedPpt} (removing ignores it): salt and calcium mix by volume (sea water
     * brings its calcium), the waste is diluted like with fresh water.
     */
    public static void changeWater(ServerLevel level, TankBlockEntity controller, long deltaMb, float addedPpt) {
        changeWater(level, controller, deltaMb, addedPpt, org.overengineer.bettacraft.chem.WaterChemistry.TAP_TDS,
                addedPpt >= Salinity.BRACKISH_FROM ? org.overengineer.bettacraft.chem.WaterChemistry.SEA_PH : org.overengineer.bettacraft.chem.WaterChemistry.TAP_PH);
    }

    /** M25: as {@link #changeWater(ServerLevel, TankBlockEntity, long, float)} with the TDS and pH of the added water (reverse osmosis water). */
    public static void changeWater(ServerLevel level, TankBlockEntity controller, long deltaMb, float addedPpt, float addedTds, float addedPh) {
        TankData tank = controller.getTank();
        long before = tank.waterMb();
        tank.setWaterMb(before + deltaMb);
        long added = tank.waterMb() - before;
        if (added > 0) {
            float salinityBefore = tank.salinity();
            org.overengineer.bettacraft.reef.ReefDosing.Snapshot reef = org.overengineer.bettacraft.reef.ReefDosing.Snapshot.of(tank); // M25 part c
            tank.setSalinity(Salinity.mix(salinityBefore, before, addedPpt, added));
            tank.setCalcium(Salinity.mixCalcium(tank.calcium(), before, Salinity.calciumOf(addedPpt), added));
            reef.mixIn(tank, before, addedPpt, added);
            if (Salinity.kind(salinityBefore) != Salinity.kind(tank.salinity()) && before > 0) {
                TankCare.log(level, controller, "log.bettacraft.salinity_" + Salinity.kind(tank.salinity()).name().toLowerCase(java.util.Locale.ROOT),
                        String.format(java.util.Locale.ROOT, "%.1f", tank.salinity()));
            }
            tank.setWaterQuality(WaterQuality.mix(tank.waterQuality(), before, added));
            tank.setNitrate(TankNitrate.mix(tank.nitrate(), before, 0F, added)); // M24: fresh water has no nitrate
            org.overengineer.bettacraft.chem.TankChemistry.mixIn(tank, before, added, addedTds, addedPh); // M25: tap water (or reverse osmosis water)
            tank.setFertilizer(before <= 0 ? 0F : (float) (tank.fertilizer() * (double) before / (before + added))); // M37: fresh water dilutes it
            tank.setLastFreshWater(level.getGameTime());
            if (before > 0) tank.setLastWaterChange(level.getGameTime());
            // M19: fresh water has the ambient temperature; a large cool water change sets off the corydoras
            org.overengineer.bettacraft.tankmates.TankmateBreeding.onFreshWater(level, controller, before, added);
        }
        TankMultiblock.applyStates(level, tank);
        controller.markTankChanged();
    }

    /**
     * M24: adds (or removes, negative) water that carries values ({@link WaterValues}, from a pipe or another tank): all
     * values mix by volume. Without values it is tap water ({@link #changeWater}, fresh: dilutes, counts as a water change).
     */
    public static void addWater(ServerLevel level, TankBlockEntity controller, long deltaMb, @javax.annotation.Nullable WaterValues v) {
        if (v == null || deltaMb <= 0) {
            changeWater(level, controller, deltaMb, 0.0F);
            return;
        }
        TankData tank = controller.getTank();
        long before = tank.waterMb();
        tank.setWaterMb(before + deltaMb);
        long added = tank.waterMb() - before;
        if (added > 0) {
            float salinityBefore = tank.salinity();
            org.overengineer.bettacraft.reef.ReefDosing.Snapshot reef = org.overengineer.bettacraft.reef.ReefDosing.Snapshot.of(tank); // M25 part c
            tank.setSalinity(Salinity.mix(salinityBefore, before, v.salinity(), added));
            tank.setCalcium(Salinity.mixCalcium(tank.calcium(), before, v.calcium(), added));
            reef.mixIn(tank, before, v.salinity(), added);
            if (Salinity.kind(salinityBefore) != Salinity.kind(tank.salinity()) && before > 0) {
                TankCare.log(level, controller, "log.bettacraft.salinity_" + Salinity.kind(tank.salinity()).name().toLowerCase(java.util.Locale.ROOT),
                        String.format(java.util.Locale.ROOT, "%.1f", tank.salinity()));
            }
            double total = before + added;
            tank.setWaterQuality(before <= 0 ? v.quality() : (float) ((tank.waterQuality() * (double) before + v.quality() * (double) added) / total));
            tank.setNitrate(TankNitrate.mix(tank.nitrate(), before, v.nitrate(), added));
            org.overengineer.bettacraft.chem.TankChemistry.mixIn(tank, before, added, v.tds(), v.ph()); // M25
            tank.setFertilizer(before <= 0 ? v.fertilizer() : (float) ((tank.fertilizer() * (double) before + v.fertilizer() * (double) added) / total)); // M37
            tank.setWaterTemp(before <= 0 ? v.temp() : (float) ((tank.waterTemp() * (double) before + v.temp() * (double) added) / total));
            if (!Float.isNaN(tank.oxygen()) && before <= 0) tank.setOxygen(Float.NaN);
        }
        TankMultiblock.applyStates(level, tank);
        controller.markTankChanged();
    }

    /**
     * M24: tops off evaporated water (level sensor): mixes like {@link #addWater} but is no water change (tap water has the
     * temperature of the place and no nitrate; the salt that stayed behind is diluted back to where it was).
     */
    public static void topOff(ServerLevel level, TankBlockEntity controller, long mb, @javax.annotation.Nullable WaterValues v) {
        if (mb <= 0) return;
        addWater(level, controller, mb, v != null ? v : WaterValues.tap(TankCare.ambient(level, controller.getTank())));
    }

    public static void showInfo(ServerPlayer player, BlockPos pos) {
        TankBlockEntity controller = TankMultiblock.getController(player.level(), pos);
        TankBlockEntity be = TankMultiblock.getBE(player.level(), pos);
        if (controller == null || be == null) {
            player.displayClientMessage(Component.translatable("message.bettacraft.tank.not_formed"), true);
            return;
        }
        TankData t = controller.getTank();
        player.displayClientMessage(Component.translatable("message.bettacraft.tank.info",
                t.sizeX(), t.sizeY(), t.sizeZ(), t.waterMb(), t.capacityMb(), t.scale(), be.getGlassId().toString()), true);
    }
}
