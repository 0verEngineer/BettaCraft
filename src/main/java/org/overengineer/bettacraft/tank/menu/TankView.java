package org.overengineer.bettacraft.tank.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bucketable;
import org.overengineer.bettacraft.entity.betta.BettaCare;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.BettaHealth;
import org.overengineer.bettacraft.equipment.FeederBlockEntity;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.tank.SignPosition;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;

/**
 * What the tank menu shows (PLAN 8). Built on the server per player, sent as NBT only when it changed while the
 * menu is open ({@link TankMenu#broadcastChanges}).
 */
public final class TankView {
    public static final int LOG_LINES = 40;

    public record Inhabitant(UUID uuid, String customName, String typeKey, boolean betta, boolean female, String stage,
                             boolean adult, float health, float satiation, boolean sick, boolean bucketable, BettaGenome genome) {
        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", uuid);
            t.putString("name", customName);
            t.putString("type", typeKey);
            t.putBoolean("betta", betta);
            t.putBoolean("female", female);
            t.putString("stage", stage);
            t.putBoolean("adult", adult);
            t.putFloat("health", health);
            t.putFloat("sat", satiation);
            t.putBoolean("sick", sick);
            t.putBoolean("bucketable", bucketable);
            BettaGenome.CODEC.encodeStart(NbtOps.INSTANCE, genome).result().ifPresent(g -> t.put("genome", g));
            return t;
        }

        static Inhabitant load(CompoundTag t) {
            BettaGenome g = BettaGenome.CODEC.parse(NbtOps.INSTANCE, t.getCompound("genome")).result().orElse(BettaGenome.DEFAULT);
            return new Inhabitant(t.getUUID("id"), t.getString("name"), t.getString("type"), t.getBoolean("betta"), t.getBoolean("female"),
                    t.getString("stage"), t.getBoolean("adult"), t.getFloat("health"), t.getFloat("sat"), t.getBoolean("sick"),
                    t.getBoolean("bucketable"), g);
        }
    }

    public record LogLine(long time, String key, List<String> args) {}

    public BlockPos pos = BlockPos.ZERO;
    public String name = "";
    public boolean signVisible = true;
    public SignPosition signPos = SignPosition.TOP_CENTER;
    public Direction front = Direction.SOUTH;
    public boolean locked;
    public boolean canModify = true;
    public boolean isOwner;
    public String ownerName = "";
    public long waterMb, capacityMb;
    public int sizeX, sizeY, sizeZ;
    public float waterTemp, targetTemp, ambient;
    public double heaterBlocks;
    public int feederFood = -1, feederPerDay;
    public int lights, lightsLit;
    public int adults, hardCap;
    public boolean crowded;
    public boolean hasBucket, hasBag;
    public long minInsertMb;
    public final List<Inhabitant> inhabitants = new ArrayList<>();
    public final List<LogLine> log = new ArrayList<>();
    public long gameTime;

    public static CompoundTag build(ServerPlayer player, TankBlockEntity controller) {
        ServerLevel level = player.serverLevel();
        TankData t = controller.getTank();
        CompoundTag out = new CompoundTag();
        out.putLong("pos", controller.getBlockPos().asLong());
        out.putString("name", t.name());
        out.putBoolean("sign", t.signVisible());
        out.putString("sign_pos", t.signPos().getSerializedName());
        out.putString("front", t.front().getSerializedName());
        out.putBoolean("locked", t.locked());
        out.putBoolean("can_modify", TankInhabitants.canModify(player, t));
        out.putBoolean("owner", player.getUUID().equals(t.owner()));
        String ownerName = "";
        if (t.owner() != null) {
            var sp = level.getServer().getPlayerList().getPlayer(t.owner());
            if (sp != null) ownerName = sp.getGameProfile().getName();
            else ownerName = level.getServer().getProfileCache() == null ? "" : level.getServer().getProfileCache().get(t.owner())
                    .map(com.mojang.authlib.GameProfile::getName).orElse("");
        }
        out.putString("owner_name", ownerName);
        out.putLong("water", t.waterMb());
        out.putLong("cap", t.capacityMb());
        out.putIntArray("size", new int[]{t.sizeX(), t.sizeY(), t.sizeZ()});
        out.putFloat("temp", Math.round(t.waterTemp() * 10) / 10.0F);
        out.putFloat("target", t.targetTemp());
        out.putFloat("ambient", Math.round(TankCare.ambient(level, t) * 10) / 10.0F);
        out.putDouble("heaters", TankCare.heaterBlocks(t));
        FeederBlockEntity feeder = TankCare.findFeeder(level, t);
        out.putInt("feeder", feeder == null ? -1 : feeder.foodCount());
        out.putInt("feeder_per_day", feeder == null ? 0 : feeder.feedingsPerDay());
        int[] lights = TankCare.lights(level, t);
        out.putInt("lights", lights[0]);
        out.putInt("lights_lit", lights[1]);
        out.putBoolean("has_bucket", TankInhabitants.findContainer(player, false) >= 0);
        out.putBoolean("has_bag", TankInhabitants.findContainer(player, true) >= 0);
        out.putLong("min_insert", Math.round(org.overengineer.bettacraft.config.ServerConfig.getOr(
                org.overengineer.bettacraft.config.ServerConfig.TANK_MIN_FILL_TO_INSERT, 0.5) * t.capacityMb()));

        BettaHealth.Params p = BettaCare.params();
        ListTag inh = new ListTag();
        List<Entity> live = TankInhabitants.liveInhabitants(level, t);
        live.sort(java.util.Comparator.comparing(e -> e.getUUID()));
        for (Entity e : live) {
            Inhabitant i;
            String custom = e.hasCustomName() ? e.getCustomName().getString() : "";
            if (e instanceof BettaEntity b) {
                BettaHealth h = b.health();
                i = new Inhabitant(e.getUUID(), custom, e.getType().getDescriptionId(), true, b.isFemale(), b.stage(), b.isAdult(),
                        round(h.healthFraction(p)), round(h.satiation(p)), h.isSick(), true, b.genome());
            } else {
                i = new Inhabitant(e.getUUID(), custom, e.getType().getDescriptionId(), false, false, "", TankInhabitants.isAdult(e),
                        1F, 1F, false, e instanceof Bucketable, BettaGenome.DEFAULT);
            }
            inh.add(i.save());
        }
        out.put("inhabitants", inh);
        out.putInt("adults", TankInhabitants.countAdults(live));
        out.putInt("hard_cap", TankInhabitants.hardCap(t.waterBlocks()));
        out.putBoolean("crowded", controller.isCrowded());

        ListTag lg = new ListTag();
        List<TankData.LogEntry> entries = t.log();
        for (int k = Math.max(0, entries.size() - LOG_LINES); k < entries.size(); k++) {
            TankData.LogEntry e = entries.get(k);
            CompoundTag c = new CompoundTag();
            c.putLong("t", e.time());
            c.putString("k", e.key());
            ListTag a = new ListTag();
            for (String s : e.args()) a.add(StringTag.valueOf(s));
            c.put("a", a);
            lg.add(c);
        }
        out.put("log", lg);
        return out;
    }

    private static float round(float f) {
        return Math.round(f * 100) / 100.0F;
    }

    public static TankView read(CompoundTag t) {
        TankView v = new TankView();
        v.pos = BlockPos.of(t.getLong("pos"));
        v.name = t.getString("name");
        v.signVisible = t.getBoolean("sign");
        v.signPos = SignPosition.byName(t.getString("sign_pos"));
        Direction f = Direction.byName(t.getString("front"));
        v.front = f == null ? Direction.SOUTH : f;
        v.locked = t.getBoolean("locked");
        v.canModify = t.getBoolean("can_modify");
        v.isOwner = t.getBoolean("owner");
        v.ownerName = t.getString("owner_name");
        v.waterMb = t.getLong("water");
        v.capacityMb = t.getLong("cap");
        int[] s = t.getIntArray("size");
        if (s.length == 3) {
            v.sizeX = s[0];
            v.sizeY = s[1];
            v.sizeZ = s[2];
        }
        v.waterTemp = t.getFloat("temp");
        v.targetTemp = t.getFloat("target");
        v.ambient = t.getFloat("ambient");
        v.heaterBlocks = t.getDouble("heaters");
        v.feederFood = t.getInt("feeder");
        v.feederPerDay = t.getInt("feeder_per_day");
        v.lights = t.getInt("lights");
        v.lightsLit = t.getInt("lights_lit");
        v.hasBucket = t.getBoolean("has_bucket");
        v.hasBag = t.getBoolean("has_bag");
        v.minInsertMb = t.getLong("min_insert");
        ListTag inh = t.getList("inhabitants", Tag.TAG_COMPOUND);
        for (int i = 0; i < inh.size(); i++) v.inhabitants.add(Inhabitant.load(inh.getCompound(i)));
        v.adults = t.getInt("adults");
        v.hardCap = t.getInt("hard_cap");
        v.crowded = t.getBoolean("crowded");
        ListTag lg = t.getList("log", Tag.TAG_COMPOUND);
        for (int i = 0; i < lg.size(); i++) {
            CompoundTag c = lg.getCompound(i);
            List<String> args = new ArrayList<>();
            ListTag a = c.getList("a", Tag.TAG_STRING);
            for (int k = 0; k < a.size(); k++) args.add(a.getString(k));
            v.log.add(new LogLine(c.getLong("t"), c.getString("k"), args));
        }
        return v;
    }
}
