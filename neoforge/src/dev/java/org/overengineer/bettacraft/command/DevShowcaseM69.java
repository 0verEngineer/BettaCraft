package org.overengineer.bettacraft.command;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.aquarist.AquaristArrival;
import org.overengineer.bettacraft.aquarist.AquaristEntity;
import org.overengineer.bettacraft.aquarist.AquaristRecords;
import org.overengineer.bettacraft.aquarist.AquaristTeam;
import org.overengineer.bettacraft.aquarist.AquaristWork;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.platform.item.IItemHandler;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;

/**
 * M69 in the showcase: the station "Aquaristen-Team". Nine tanks of the dev player "Dev" (offline UUID) with a pair of bettas each,
 * behind them three homes (breeding basin, a chest with emeralds, bread and food, a reservoir, a bed). Lena and Finn are hired and share the tanks:
 * each tank goes to the one whose basin is nearest (no tank twice); the third home is free. Lena worked twelve days without a
 * break, there was another breeding: her friend is on offer (tab Friend, "Freund" shines), hiring him in her menu fills the
 * third home and the team (3/3). {@code /bettacraft aquarist friend} brings the offer back after a refusal.
 */
final class DevShowcaseM69 {
    /** The player the showcase's aquarists work for: the dev client's name on NeoForge. */
    static final String PLAYER = "Dev";
    static final List<String> PROBLEMS = new ArrayList<>();

    private DevShowcaseM69() {}

    static String teamRow(ServerLevel level, BlockPos at) {
        PROBLEMS.clear();
        UUID dev = net.minecraft.core.UUIDUtil.createOfflinePlayerUUID(PLAYER);
        // the ground of the station (the row's floor is at y -1, the cabinets' level)
        for (int x = -1; x <= 46; x++) for (int z = 0; z <= 5; z++) level.setBlockAndUpdate(at.offset(x, -2, z), Blocks.SMOOTH_STONE.defaultBlockState());
        BlockPos[] basins = {at.offset(4, -1, 4), at.offset(22, -1, 4), at.offset(38, -1, 4)};
        Block[] beds = {Blocks.RED_BED, Blocks.BLUE_BED, Blocks.YELLOW_BED};
        for (int i = 0; i < 3; i++) home(level, basins[i], beds[i]);
        List<TankBlockEntity> tanks = new ArrayList<>();
        RandomSource r = RandomSource.create(6900);
        for (int i = 0; i < 9; i++) {
            BlockPos min = at.offset(i * 5, -1, 0);
            TankBlockEntity c = DevShowcase.tank(level, min, 3, 2, 2, 0F, true);
            c.getTank().setOwner(dev);
            AquaristRecords.get(level.getServer()).noteTank(level, c);
            for (int k = 0; k < 2; k++) {
                BettaData d = BettaData.create(BettaGenotype.randomDomestic(RandomSource.create(r.nextLong())), k == 1, 0);
                TankInhabitants.insert(level, c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d), ModEntities.BETTA.get(), null, false);
            }
            DevShowcase.name(level, min, "Team " + (i + 1));
            tanks.add(c);
        }
        // Lena and Finn (the third home stays free for the friend)
        AquaristEntity lena = AquaristArrival.spawnHiredHomeless(level, dev, PLAYER, Vec3.atCenterOf(basins[0].offset(0, 0, -2)));
        lena.setCustomName(Component.literal("Lena"));
        if (!lena.setHome(level, basins[0], basins[0].offset(4, 0, 0))) PROBLEMS.add("Lena: no home");
        AquaristEntity finn = AquaristTeam.spawnMember(level, dev, PLAYER, Vec3.atCenterOf(basins[1].offset(0, 0, -2)));
        finn.setCustomName(Component.literal("Finn"));
        if (!finn.setHome(level, basins[1], basins[1].offset(4, 0, 0))) PROBLEMS.add("Finn: no home");
        // the friend's conditions are met and his offer stands
        AquaristRecords rec = AquaristRecords.get(level.getServer());
        AquaristRecords.PlayerRecord pr = rec.player(dev);
        long now = AquaristTeam.now(level.getServer());
        for (AquaristRecords.Member m : pr.team) {
            m.since = now - (m.id.equals(lena.getUUID()) ? 12 : 9) * 24000L;
            m.paidSince = m.since;
        }
        pr.lastHire = now - 9 * 24000L;
        pr.breedings = pr.breedingsAtHire + 1;
        pr.friendOffered = true;
        pr.friendName = "Mika";
        pr.name = PLAYER;
        rec.setDirty();
        // no tank twice
        Set<UUID> seen = new HashSet<>();
        int cared = 0;
        for (AquaristEntity a : List.of(lena, finn)) {
            for (TankBlockEntity c : AquaristWork.tanks(level, a)) {
                cared++;
                if (!seen.add(c.getTank().id())) PROBLEMS.add("a tank with two aquarists: " + c.getBlockPos());
            }
        }
        if (cared != tanks.size()) PROBLEMS.add("tanks looked after: " + cared + " of " + tanks.size());
        return "Lena " + AquaristWork.tanks(level, lena).size() + " tanks, Finn " + AquaristWork.tanks(level, finn).size() + " tanks, friend Mika on offer, "
                + (PROBLEMS.isEmpty() ? "no tank twice" : PROBLEMS.toString());
    }

    /** A breeding basin with a chest of wages east of it, a reservoir west and a bed (head 4 blocks east), behind the tanks. */
    private static void home(ServerLevel level, BlockPos basin, Block bed) {
        level.setBlockAndUpdate(basin, ModBlocks.BREEDING_BASIN.get().defaultBlockState());
        level.setBlockAndUpdate(basin.east(), Blocks.CHEST.defaultBlockState());
        IItemHandler chest = org.overengineer.bettacraft.platform.Platform.get().itemHandler(level, basin.east(), null);
        if (chest != null) {
            chest.insertItem(0, new ItemStack(Items.EMERALD, 64), false);
            chest.insertItem(1, new ItemStack(Items.BREAD, 64), false);
            chest.insertItem(2, new ItemStack(ModItems.BETTA_PELLETS.get(), 64), false);
        }
        level.setBlockAndUpdate(basin.west(), ModBlocks.RESERVOIR.get().defaultBlockState());
        BlockState foot = bed.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST).setValue(BedBlock.PART, BedPart.FOOT);
        level.setBlockAndUpdate(basin.offset(3, 0, 0), foot);
        level.setBlockAndUpdate(basin.offset(4, 0, 0), foot.setValue(BedBlock.PART, BedPart.HEAD));
    }
}
