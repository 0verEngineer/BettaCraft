package org.overengineer.bettacraft.tank.interior;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * One tank's region in the interior dimension. Regions sit on a grid with fixed spacing (like Compact Machines).
 * <p>
 * Layout (world coordinates in {@code bettacraft:tank_interior}):
 * floor at y = {@link #FLOOR_Y}, interior volume y in [{@link #BASE_Y}, BASE_Y + sizeY),
 * x in [originX, originX + sizeX), z in [originZ, originZ + sizeZ); invisible walls one block outside, ceiling on top.
 * <p>
 * M10: the region covers the tank's bounding box; {@link #cells} says which tank blocks (cells of scale^3 interior
 * blocks) belong to the tank. Cells outside the shape are closed off by invisible walls on their faces towards the
 * shape (and get no floor), so nothing can be built there and the miniature leaves them empty.
 */
public final class InteriorRegion {
    /** Max interior extent is 64 tank blocks * 16 = 1024; the spacing leaves room for walls and a gap. */
    public static final int SPACING = 2048;
    public static final int GRID_WIDTH = 256;
    public static final int FLOOR_Y = 0;
    public static final int BASE_Y = 1;

    private final int id;
    private int sizeX, sizeY, sizeZ;
    private int scale;
    private ResourceKey<Level> tankDimension;
    private BlockPos tankMin;
    @Nullable
    private UUID tankId;
    @Nullable
    private UUID owner;
    private boolean built;
    /** M53c: serial number of this allocation (ids are reused; 0 = a region of an earlier version). */
    private long serial;
    /** Tank blocks of the shape as a bit set over the cell grid (sizeX / scale ...), see {@link org.overengineer.bettacraft.tank.TankShape#encode}. */
    private byte[] cells;
    /** Equipment and floating plants inside the region (PLAN 7.3), kept up to date by block updates. */
    private final InteriorEquipment equipment = new InteriorEquipment();
    /** Aquarium plants of the region (M13): growth data, the blocks follow them. */
    private final org.overengineer.bettacraft.plants.PlantIndex plants = new org.overengineer.bettacraft.plants.PlantIndex();

    public InteriorRegion(int id, int sizeX, int sizeY, int sizeZ, int scale, ResourceKey<Level> tankDimension, BlockPos tankMin,
                          @Nullable UUID tankId, @Nullable UUID owner) {
        this(id, sizeX, sizeY, sizeZ, scale, tankDimension, tankMin, tankId, owner, null);
    }

    public InteriorRegion(int id, int sizeX, int sizeY, int sizeZ, int scale, ResourceKey<Level> tankDimension, BlockPos tankMin,
                          @Nullable UUID tankId, @Nullable UUID owner, @Nullable byte[] cells) {
        this.id = id;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.scale = scale;
        this.tankDimension = tankDimension;
        this.tankMin = tankMin.immutable();
        this.tankId = tankId;
        this.owner = owner;
        int cs = Math.max(1, scale);
        this.cells = cells != null ? cells : org.overengineer.bettacraft.tank.TankShape.fullBox(sizeX / cs, sizeY / cs, sizeZ / cs);
    }

    /** Cell grid size (tank blocks of the bounding box). */
    public int cellsX() { return sizeX / Math.max(1, scale); }
    public int cellsY() { return sizeY / Math.max(1, scale); }
    public int cellsZ() { return sizeZ / Math.max(1, scale); }

    /** Whether the tank block at cell (cx, cy, cz) (relative to the tank's bounding-box min) belongs to the tank. */
    public boolean hasCell(int cx, int cy, int cz) {
        if (cx < 0 || cy < 0 || cz < 0 || cx >= cellsX() || cy >= cellsY() || cz >= cellsZ()) return false;
        return org.overengineer.bettacraft.tank.TankShape.bit(cells, cellsX(), cellsZ(), cx, cy, cz);
    }

    /** Whether the interior-local position lies in a cell of the tank's shape. */
    public boolean inShape(int lx, int ly, int lz) {
        int s = Math.max(1, scale);
        return lx >= 0 && ly >= 0 && lz >= 0 && hasCell(lx / s, ly / s, lz / s);
    }

    /** True if every cell of the bounding box belongs to the tank (a cuboid tank, as before M10). */
    public boolean fullBox() {
        for (int y = 0; y < cellsY(); y++)
            for (int z = 0; z < cellsZ(); z++)
                for (int x = 0; x < cellsX(); x++) if (!hasCell(x, y, z)) return false;
        return true;
    }

    public byte[] cells() {
        return cells.clone();
    }

    public int id() { return id; }
    public int sizeX() { return sizeX; }
    public int sizeY() { return sizeY; }
    public int sizeZ() { return sizeZ; }
    public int scale() { return scale; }
    public ResourceKey<Level> tankDimension() { return tankDimension; }
    public BlockPos tankMin() { return tankMin; }
    @Nullable public UUID tankId() { return tankId; }
    @Nullable public UUID owner() { return owner; }
    public boolean built() { return built; }
    public long serial() { return serial; }
    void setSerial(long serial) { this.serial = serial; }
    void setBuilt(boolean built) { this.built = built; }
    public InteriorEquipment equipment() { return equipment; }
    public org.overengineer.bettacraft.plants.PlantIndex plants() { return plants; }

    void retarget(ResourceKey<Level> dim, BlockPos tankMin, @Nullable UUID tankId) {
        this.tankDimension = dim;
        this.tankMin = tankMin.immutable();
        this.tankId = tankId;
    }

    public static int originX(int id) {
        return (id % GRID_WIDTH) * SPACING;
    }

    public static int originZ(int id) {
        return (id / GRID_WIDTH) * SPACING;
    }

    public int originX() { return originX(id); }
    public int originZ() { return originZ(id); }

    /** World position of interior-local coordinates (0,0,0 = first block above the floor, min corner). */
    public BlockPos toWorld(int lx, int ly, int lz) {
        return new BlockPos(originX() + lx, BASE_Y + ly, originZ() + lz);
    }

    public boolean containsInterior(BlockPos worldPos) {
        int lx = worldPos.getX() - originX(), ly = worldPos.getY() - BASE_Y, lz = worldPos.getZ() - originZ();
        return lx >= 0 && ly >= 0 && lz >= 0 && lx < sizeX && ly < sizeY && lz < sizeZ;
    }

    /** Inside the volume and in a cell of the shape (M10). */
    public boolean containsShapeInterior(BlockPos worldPos) {
        return containsInterior(worldPos) && inShape(localX(worldPos.getX()), localY(worldPos.getY()), localZ(worldPos.getZ()));
    }

    /** True if the position is inside the region including floor, walls and ceiling. */
    public boolean containsShell(BlockPos worldPos) {
        int lx = worldPos.getX() - originX(), ly = worldPos.getY() - BASE_Y, lz = worldPos.getZ() - originZ();
        return lx >= -1 && ly >= -1 && lz >= -1 && lx <= sizeX && ly <= sizeY && lz <= sizeZ;
    }

    /** Local coordinates (interior volume, y = 0 is the first block above the floor) of a world position. */
    public int localX(int worldX) { return worldX - originX(); }
    public int localY(int worldY) { return worldY - BASE_Y; }
    public int localZ(int worldZ) { return worldZ - originZ(); }

    /** M53c: chunk x range [0] .. [1] and z range [2] .. [3] (inclusive) of the grid cell of a region id (see {@link #idAt}). */
    public static int[] cellChunks(int id) {
        int x = originX(id) - 512, z = originZ(id) - 512;
        return new int[]{x >> 4, (x + SPACING - 1) >> 4, z >> 4, (z + SPACING - 1) >> 4};
    }

    /**
     * M53c: region file x range [0] .. [1] and z range [2] .. [3] (inclusive, 512 blocks each) of the grid cell of a region id.
     * A cell starts 512 blocks before the origin and is 2048 wide, so its 4 x 4 region files hold no chunk of another cell.
     */
    public static int[] cellRegionFiles(int id) {
        int[] c = cellChunks(id);
        return new int[]{c[0] >> 5, c[1] >> 5, c[2] >> 5, c[3] >> 5};
    }

    /** Region id owning a world position of the interior dimension (grid cell), or -1. */
    public static int idAt(int x, int z) {
        // A cell spans [origin - 512, origin + 1536): enough for the 1024 max interior plus walls.
        int ix = Math.floorDiv(x + 512, SPACING);
        int iz = Math.floorDiv(z + 512, SPACING);
        if (ix < 0 || ix >= GRID_WIDTH || iz < 0) return -1;
        return iz * GRID_WIDTH + ix;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putIntArray("size", new int[]{sizeX, sizeY, sizeZ});
        t.putInt("scale", scale);
        t.putString("tank_dim", tankDimension.location().toString());
        t.putIntArray("tank_min", new int[]{tankMin.getX(), tankMin.getY(), tankMin.getZ()});
        if (tankId != null) t.putUUID("tank_id", tankId);
        if (owner != null) t.putUUID("owner", owner);
        t.putBoolean("built", built);
        if (serial != 0) t.putLong("serial", serial);
        t.put("equipment", equipment.save());
        t.putByteArray("cells", cells.clone());
        t.put("plants", plants.saveAll());
        return t;
    }

    public static InteriorRegion load(CompoundTag t) {
        int[] s = t.getIntArray("size");
        int[] m = t.getIntArray("tank_min");
        ResourceLocation dim = ResourceLocation.tryParse(t.getString("tank_dim"));
        InteriorRegion r = new InteriorRegion(t.getInt("id"),
                s.length == 3 ? s[0] : 16, s.length == 3 ? s[1] : 16, s.length == 3 ? s[2] : 16,
                t.getInt("scale"),
                ResourceKey.create(Registries.DIMENSION, dim == null ? Level.OVERWORLD.location() : dim),
                m.length == 3 ? new BlockPos(m[0], m[1], m[2]) : BlockPos.ZERO,
                t.hasUUID("tank_id") ? t.getUUID("tank_id") : null,
                t.hasUUID("owner") ? t.getUUID("owner") : null,
                t.contains("cells", net.minecraft.nbt.Tag.TAG_BYTE_ARRAY) ? t.getByteArray("cells") : null);
        r.built = t.getBoolean("built");
        r.serial = t.getLong("serial");
        r.equipment.load(t.getCompound("equipment"));
        r.plants.load(t.getCompound("plants"));
        return r;
    }
}
