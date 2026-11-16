package org.overengineer.bettacraft.compat.create;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.tank.TankDrive;

/**
 * M41: Create's rotation for the FE buffer of a tank, with real stress ({@link TankDrive} plans, this charges). Compiled
 * against Create (compileOnly, never in our jar) and only loaded when Create is there ({@link #register} from the mod
 * constructor behind a {@code ModList} check, like {@link CreateDisplays}); without Create the drive adapter does not exist.
 * <ul>
 *     <li><b>Drive adapter</b> ({@link DriveAdapterBlock}): a kinetic block of Create's network with a shaft on its front; its
 *     back sits on a tank wall or under a tank. Its stress impact follows the load the tank gives it.</li>
 *     <li><b>Shaft on a cabinet</b>: the cabinet is not a kinetic block (it stays a plain block entity of ours), so the shaft,
 *     cogwheel or gearbox at the cabinet's drive side carries the cabinet's stress in its network
 *     ({@link KineticNetwork#updateStressFor}: its own impact plus the cabinet's), renewed every care step and taken back when
 *     the cabinet stops driving ({@link TankDrive.Bridge#release}) or was not served for a while.</li>
 * </ul>
 */
public final class CreateDrive {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BettaCraft.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BettaCraft.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BettaCraft.MOD_ID);

    public static final DeferredBlock<DriveAdapterBlock> ADAPTER = BLOCKS.register("drive_adapter", () -> new DriveAdapterBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5F, 6F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredItem<BlockItem> ADAPTER_ITEM = ITEMS.registerSimpleBlockItem(ADAPTER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DriveAdapterBlockEntity>> ADAPTER_BE = BLOCK_ENTITIES.register("drive_adapter",
            () -> BlockEntityType.Builder.of(DriveAdapterBlockEntity::new, ADAPTER.get()).build(null));

    /** Ticks after the last care step that served a cabinet's shaft before its stress is taken back. */
    static final long STALE = 60;

    private CreateDrive() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        // Create's item tooltip (goggles) shows the impact at full load
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent e) -> e.enqueueWork(() ->
                BlockStressValues.IMPACTS.register(ADAPTER.get(), () -> TankDrive.stressPerFe() * TankDrive.fePerInput() / TankDrive.fullSpeed())));
        TankDrive.BRIDGE = new Bridge();
        TankDrive.ADAPTER_ITEM = ADAPTER_ITEM::get;
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % 20 == 0) releaseStale();
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> CHARGED.clear());
    }

    // ------------------------------------------------------------------ the shaft at a cabinet

    /** The cabinets whose shaft carries stress now. */
    private static final Map<CabinetBlockEntity, CabinetInput> CHARGED = new HashMap<>();

    /** The kinetic block entity at the drive side of the cabinet whose shaft points at it (not a drive adapter), or null. */
    @Nullable
    static KineticBlockEntity shaftAt(Level level, CabinetBlockEntity cab) {
        Direction face = cab.driveFace();
        if (face == null) return null;
        BlockPos n = cab.getBlockPos().relative(face);
        if (!level.isLoaded(n)) return null;
        BlockEntity be = level.getBlockEntity(n);
        if (!(be instanceof KineticBlockEntity k) || be instanceof DriveAdapterBlockEntity) return null;
        BlockState st = be.getBlockState();
        if (!(st.getBlock() instanceof IRotate r) || !r.hasShaftTowards(level, n, st, face.getOpposite())) return null;
        return k;
    }

    private static void releaseStale() {
        if (CHARGED.isEmpty()) return;
        for (Iterator<Map.Entry<CabinetBlockEntity, CabinetInput>> it = CHARGED.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<CabinetBlockEntity, CabinetInput> e = it.next();
            CabinetInput in = e.getValue();
            Level level = e.getKey().getLevel();
            if (e.getKey().isRemoved() || level == null || level.getGameTime() - in.lastLoad > STALE || level.getGameTime() < in.lastLoad) {
                it.remove();
                apply(in.shaft);
            }
        }
    }

    /** The stress a kinetic block entity has of its own (its impact per RPM). */
    static float own(KineticBlockEntity k) {
        return k.calculateStressApplied();
    }

    /** The stress of a shaft: its own and that of every cabinet it drives (two cabinets may share one shaft). */
    static void apply(KineticBlockEntity k) {
        float sum = 0F;
        for (CabinetInput in : CHARGED.values()) if (in.shaft == k) sum += in.impact;
        setStress(k, own(k) + sum);
    }

    /** Sets the stress impact of a member of a network (only when it changed: the network then syncs its members). */
    static void setStress(KineticBlockEntity k, float impact) {
        if (k.isRemoved() || !k.hasNetwork()) return;
        KineticNetwork net = k.getOrCreateNetwork();
        if (net == null) return;
        Float cur = net.members.get(k);
        if (cur != null && Math.abs(cur - impact) < 1e-4F) return;
        net.updateStressFor(k, impact);
    }

    /** The shaft at a cabinet as an input of its tank: it charges its stress onto that shaft. */
    static final class CabinetInput implements TankDrive.Input {
        final CabinetBlockEntity cab;
        KineticBlockEntity shaft;
        long lastLoad;
        float impact;
        double fe;
        boolean delivering;

        CabinetInput(CabinetBlockEntity cab, KineticBlockEntity shaft) {
            this.cab = cab;
            this.shaft = shaft;
        }

        @Override
        public BlockPos pos() {
            return cab.getBlockPos();
        }

        @Override
        public float speed() {
            return shaft.isRemoved() ? 0F : Math.abs(shaft.getTheoreticalSpeed());
        }

        @Override
        public boolean overstressed() {
            return !shaft.isRemoved() && shaft.isOverStressed();
        }

        @Override
        public float stressImpact() {
            return impact;
        }

        @Override
        public void load(double fePerTick, boolean delivering) {
            this.fe = fePerTick;
            this.delivering = delivering;
            Level level = cab.getLevel();
            lastLoad = level == null ? 0 : level.getGameTime();
            float rpm = speed();
            impact = rpm > 0F ? TankDrive.impact(fePerTick, rpm, TankDrive.stressPerFe(), TankDrive.idleStress()) : 0F;
            apply(shaft);
        }
    }

    /** The FE per tick the shaft of this cabinet gives its tank (for the overlays), or -1 while it drives nothing. */
    public static double cabinetFe(CabinetBlockEntity cab) {
        CabinetInput in = CHARGED.get(cab);
        return in == null ? -1 : in.delivering ? in.fe : 0;
    }

    /** Whether the network of this cabinet's shaft is overstressed. */
    public static boolean cabinetOverstressed(CabinetBlockEntity cab) {
        CabinetInput in = CHARGED.get(cab);
        return in != null && in.overstressed();
    }

    // ------------------------------------------------------------------ the bridge for TankDrive

    private static final class Bridge implements TankDrive.Bridge {
        @Override
        public boolean isAdapter(BlockState state, Direction outward) {
            return state.is(ADAPTER.get()) && state.getValue(DriveAdapterBlock.FACING) == outward;
        }

        @Nullable
        @Override
        public TankDrive.Input adapter(Level level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof DriveAdapterBlockEntity a && !a.isRemoved() ? a : null;
        }

        @Nullable
        @Override
        public TankDrive.Input cabinet(Level level, CabinetBlockEntity cab) {
            KineticBlockEntity k = shaftAt(level, cab);
            CabinetInput in = CHARGED.get(cab);
            if (k == null) {
                release(level, cab);
                return null;
            }
            if (in != null && in.shaft != k) {
                KineticBlockEntity old = in.shaft; // another block took the place of the shaft
                in.shaft = k;
                apply(old);
            }
            if (in == null) CHARGED.put(cab, in = new CabinetInput(cab, k));
            return in;
        }

        @Override
        public double cabinetFe(CabinetBlockEntity cab) {
            return CreateDrive.cabinetFe(cab);
        }

        @Override
        public boolean cabinetOverstressed(CabinetBlockEntity cab) {
            return CreateDrive.cabinetOverstressed(cab);
        }

        @Override
        public void release(Level level, CabinetBlockEntity cab) {
            CabinetInput in = CHARGED.remove(cab);
            if (in != null) apply(in.shaft);
        }
    }
}
