package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.overengineer.bettacraft.client.screen.TankScreen;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.plants.FloatingPlant;
import org.overengineer.bettacraft.tank.plants.TankPlants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development-only multiplayer test, client side ({@code -Dbettacraft.smoketest.mp=true}, started twice by
 * {@code gradlew runMultiplayerTest} as Dev1 and Dev2): connects to the dedicated server
 * ({@code bettacraft.smoketest.mp.address}, set by the build: the dev_server_port of the branch, never the default port), then follows the server's script (system chat
 * lines {@code bcmp:<command>:<target>:<args>}, see {@code DevSmokeMP}, hidden from the chat): reports what it sees of
 * the tank, right-clicks like a player (the use key, with the crosshair where the server turned the player), works the
 * tank menu, measures the gliding of the plants frame by frame, takes screenshots ({@code mp_<name>_*.png} in its game
 * directory) and quits. Never active in normal play.
 */
final class DevClientSmokeMP {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static final String PREFIX = "bcmp:";

    private record Step(int delay, Runnable action) {}

    private static final Deque<Step> steps = new ArrayDeque<>();
    private static int wait, connectTries, ticks, retryAt = -1;
    private static boolean connecting, ready, sizeSet;
    /** M40 part c: the first pipe cover report was sent (the second one reports the cover taken off). */
    private static boolean pipeCoverSeen;
    @Nullable
    private static BlockPos tank;
    private static int readyIn = -1, quitIn = -1;
    // motion sampling (frames)
    private static int sampleFrames = -1;
    private static final Map<Integer, double[]> lastPos = new HashMap<>();
    private static final Map<Integer, double[]> firstPos = new HashMap<>();
    private static int frames, jumps;
    // M15: the latest action bar message
    private static String lastOverlay = "";
    private static double maxStep;

    private DevClientSmokeMP() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Opening e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (e.getNewScreen() instanceof TitleScreen && !connecting) {
                connecting = true;
                connect(e.getNewScreen());
            } else if (e.getNewScreen() instanceof DisconnectedScreen && !ready && connectTries < 12) {
                // the server may still be starting: try again in 5 s
                LOGGER.warn("[mp] connection failed (try {}), retrying", connectTries);
                retryAt = ticks + 100;
            }
            if (e.getNewScreen() instanceof net.minecraft.client.gui.screens.PauseScreen) e.setCanceled(true);
            mc.options.pauseOnLostFocus = false;
        });
        NeoForge.EVENT_BUS.addListener((ClientChatReceivedEvent.System e) -> {
            String text = e.getMessage().getString();
            if (e.isOverlay()) lastOverlay = text; // M15: the action bar (e.g. why a block cannot be broken)
            if (!text.startsWith(PREFIX)) return;
            e.setCanceled(true);
            onScript(text.substring(PREFIX.length()));
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Post e) -> frame(e.getPartialTick().getGameTimeDeltaPartialTick(false)));
    }

    private static String name() {
        return Minecraft.getInstance().getUser().getName();
    }

    private static void connect(net.minecraft.client.gui.screens.Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        // M62: no default (the server of runMultiplayerTest is on the branch's dev_server_port, which the build passes)
        String addr = System.getProperty("bettacraft.smoketest.mp.address");
        if (addr == null) throw new IllegalStateException("bettacraft.smoketest.mp.address is not set (gradlew runMultiplayerTest sets it)");
        connectTries++;
        LOGGER.info("[mp] {} connecting to {} (try {})", name(), addr, connectTries);
        ConnectScreen.startConnecting(parent, mc, ServerAddress.parseString(addr), new ServerData("Bettacraft MP test", addr, ServerData.Type.OTHER),
                false, null);
    }

    // ------------------------------------------------------------------ script from the server

    private static void onScript(String line) {
        String[] a = line.split(":", 3);
        String cmd = a[0], target = a.length > 1 ? a[1] : "all", args = a.length > 2 ? a[2] : "";
        if (!"all".equals(target) && !name().equals(target)) return;
        LOGGER.info("[mp] {} script: {}", name(), line);
        switch (cmd) {
            case "tank" -> {
                tank = pos(args);
                readyIn = 120; // chunks, block entities and plants arrive
            }
            case "state" -> {
                String[] s = args.split(",", 2);
                tank = pos(s[1]);
                then(40, () -> sendState(s[0]));
            }
            case "critters" -> {
                // M14: the shrimp and snails of the tank on this client (the latest synced positions) and the algae
                String[] s = args.split(",", 2);
                tank = pos(s[1]);
                then(30, () -> sendCritters(s[0]));
            }
            case "aqua" -> {
                // M13: how many aquarium plant blocks the miniature of the tank shows on this client
                String[] s = args.split(",", 2);
                tank = pos(s[1]);
                then(40, () -> sendAquatic(s[0]));
            }
            case "use" -> use("sneak".equals(args));
            case "travel" -> {
                // M50: the tank on the turning bearing: the states of the travelling tank this client got, the contraption, the
                // packed animals its copy of the tank shows
                BlockPos at = pos(args);
                whenReady(20, 8, () -> travelReceived() > 0, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    var list = mc.level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new net.minecraft.world.phys.AABB(at).inflate(6),
                            e -> e.getType().toShortString().contains("contraption"));
                    int packed = -1;
                    for (var e : list) packed = Math.max(packed, travelPacked(e));
                    send("travel " + travelReceived() + " " + !list.isEmpty() + " " + packed);
                });
            }
            case "aquarists" -> then(40, () -> { // M51: both aquarists and whom they work for
                Minecraft mc = Minecraft.getInstance();
                String[] ids = args.split(",");
                var n1 = mc.level.getEntity(Integer.parseInt(ids[0]));
                var n2 = mc.level.getEntity(Integer.parseInt(ids[1]));
                boolean seen = n1 instanceof org.overengineer.bettacraft.aquarist.AquaristEntity && n2 instanceof org.overengineer.bettacraft.aquarist.AquaristEntity;
                send("aquarists " + seen + "," + (n1 instanceof org.overengineer.bettacraft.aquarist.AquaristEntity x ? x.employerName() : "-") + ","
                        + (n2 instanceof org.overengineer.bettacraft.aquarist.AquaristEntity y ? y.employerName() : "-"));
            });
            case "aquaristmenu" -> {
                then(40, () -> shot("aquarist_menu"));
                then(1, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    boolean open = mc.gui.screen() instanceof org.overengineer.bettacraft.client.screen.AquaristScreen;
                    send("aquaristmenu " + (open ? "ok" : "no-window:" + mc.gui.screen()));
                    if (open) mc.gui.setScreen(null);
                });
            }
            case "light" -> {
                // M18: the colour of the dyed light and the tint of its tank's water on this client
                String[] s = args.split(";", 2);
                BlockPos light = pos(s[0]), controller = pos(s[1]);
                then(40, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    var st = mc.level.getBlockState(light);
                    String col = st.getBlock() instanceof org.overengineer.bettacraft.equipment.TankLightBlock
                            ? st.getValue(org.overengineer.bettacraft.equipment.TankLightBlock.COLOR).getName() : "none";
                    send("light " + col + " " + Integer.toHexString(org.overengineer.bettacraft.client.tank.TankLightTint.colorOf(controller)));
                });
            }
            case "tankmates" -> {
                // M19: the tankmates (entities with their variety) and the clutches (tank view) of the small tank on this client
                BlockPos min = pos(args);
                whenReady(60, 10, () -> {
                    var cc = org.overengineer.bettacraft.tank.TankMultiblock.getController(Minecraft.getInstance().level, min);
                    return cc != null && cc.getTank() != null;
                }, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    var box = new net.minecraft.world.phys.AABB(min).expandTowards(2, 2, 2);
                    var mates = mc.level.getEntitiesOfClass(org.overengineer.bettacraft.entity.tankmate.TankmateEntity.class, box);
                    long albino = mates.stream().filter(m -> "albino".equals(m.variety())).count();
                    var c = org.overengineer.bettacraft.tank.TankMultiblock.getController(mc.level, min);
                    int clutches = c == null || c.getTank() == null ? -1 : c.getTank().clutches().size();
                    int eggs = c == null || c.getTank() == null ? -1 : c.getTank().clutches().stream().mapToInt(k -> k.count).sum();
                    java.util.List<String> strains = new java.util.ArrayList<>();
                    for (var m : mates) {
                        var ph = m.livebearerPhenotype();
                        if (ph == null) continue;
                        var st = org.overengineer.bettacraft.genetics.livebearer.LivebearerStrain.of(ph);
                        strains.add(st == null ? "none" : st.id());
                    }
                    java.util.Collections.sort(strains);
                    send("tankmates " + mates.size() + " " + albino + " " + clutches + " " + eggs + " " + (strains.isEmpty() ? "-" : String.join(",", strains)));
                });
            }
            case "m29" -> {
                // M29 part d: the big crayfish (species and colour of the tank view), the axolotls and the betta hybrid on this client
                BlockPos min = pos(args);
                then(60, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    var c = org.overengineer.bettacraft.tank.TankMultiblock.getController(mc.level, min);
                    java.util.List<String> cray = new java.util.ArrayList<>();
                    if (c != null && c.getTank() != null) {
                        for (var k : c.getTank().critters()) if (k.species.big()) cray.add(k.species.id() + ":" + k.clientColor);
                    }
                    java.util.Collections.sort(cray);
                    var box = new net.minecraft.world.phys.AABB(min).expandTowards(2, 2, 2);
                    java.util.List<String> axo = new java.util.ArrayList<>();
                    for (var ax : mc.level.getEntitiesOfClass(net.minecraft.world.entity.animal.axolotl.Axolotl.class, box)) axo.add(ax.getVariant().getSerializedName());
                    java.util.Collections.sort(axo);
                    String imb = "-";
                    for (var b : mc.level.getEntitiesOfClass(org.overengineer.bettacraft.entity.betta.BettaEntity.class, box)) {
                        imb = b.genome().species(); // the client gets the phenotype with the species id (hybrid), not the shares
                    }
                    send("m29 " + (cray.isEmpty() ? "-" : String.join(",", cray)) + " " + (axo.isEmpty() ? "-" : String.join(",", axo)) + " " + imb);
                });
            }
            case "m30" -> {
                // M30 part c: the big sea animals (entities) and the shark egg case (tank data) on this client
                BlockPos min = pos(args);
                then(60, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    var c = org.overengineer.bettacraft.tank.TankMultiblock.getController(mc.level, min);
                    int cases = 0;
                    if (c != null && c.getTank() != null) {
                        for (var k : c.getTank().clutches()) if (k.kind == org.overengineer.bettacraft.tankmates.Clutch.Kind.EGG_CASE) cases++;
                    }
                    java.util.List<String> big = new java.util.ArrayList<>();
                    for (var m : mc.level.getEntitiesOfClass(org.overengineer.bettacraft.entity.tankmate.TankmateEntity.class,
                            new net.minecraft.world.phys.AABB(min).expandTowards(16, 4, 8).inflate(0.5), e -> e.species().isLarge())) big.add(m.species().id());
                    java.util.Collections.sort(big);
                    send("m30 " + (big.isEmpty() ? "-" : String.join(",", big)) + " " + cases);
                });
            }
            case "m31" -> {
                // M31 part a: the terrarium's mode and climate in the tank view of this client
                BlockPos min = pos(args);
                then(60, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    var c = org.overengineer.bettacraft.tank.TankMultiblock.getController(mc.level, min);
                    TankData t = c == null ? null : c.getTank();
                    if (t == null) {
                        send("error m31: no tank view for " + min);
                        return;
                    }
                    // M31 part b: the variety of the slider and the growth of the hatchling (synced entity data)
                    int variety = -1, geckoVariety = -1;
                    float growth = 2F;
                    String pose = "none";
                    for (var s : mc.level.getEntitiesOfClass(org.overengineer.bettacraft.terrarium.HerpEntity.class, new net.minecraft.world.phys.AABB(min).inflate(8))) {
                        if (s.species() == org.overengineer.bettacraft.terrarium.HerpSpecies.CRESTED_GECKO) { // M31 part c
                            pose = s.herpPose().name();
                            geckoVariety = s.variety();
                            continue;
                        }
                        variety = Math.max(variety, s.variety());
                        growth = Math.min(growth, s.growth());
                    }
                    // M31 part d: the smallest zombie there (the one in glass)
                    float zombie = 9F;
                    for (var z : mc.level.getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Zombie.class, new net.minecraft.world.phys.AABB(min).inflate(8))) {
                        zombie = Math.min(zombie, z.getScale());
                    }
                    send("m31 " + t.terrariumMode().id() + " " + t.hasClimate() + " " + Math.round(t.uv()) + " " + variety + " " + Math.round(growth) + " " + pose + " "
                            + geckoVariety + " " + Math.round(zombie * 100) + " " + t.tankMode().id()); // M33.1: the tank type
                });
            }
            case "reef" -> {
                // M20: salinity of the tank view, the salt the water is drawn with, the reef fish and the coral blocks of the miniature
                BlockPos min = pos(args);
                then(80, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    var c = org.overengineer.bettacraft.tank.TankMultiblock.getController(mc.level, min);
                    TankData t = c == null ? null : c.getTank();
                    if (t == null) {
                        send("error reef: no tank view for " + min);
                        return;
                    }
                    var box = new net.minecraft.world.phys.AABB(min).expandTowards(2, 2, 2);
                    int fish = mc.level.getEntitiesOfClass(org.overengineer.bettacraft.entity.tankmate.TankmateEntity.class, box).size();
                    int coral = 0;
                    var region = t.regionId() < 0 ? null : org.overengineer.bettacraft.client.interior.ClientInteriorCache.get(t.regionId());
                    if (region != null) {
                        int sx = t.sizeX() * t.scale(), sy = t.sizeY() * t.scale(), sz = t.sizeZ() * t.scale();
                        for (int x = 0; x < sx; x++) for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) {
                            var sp = org.overengineer.bettacraft.plants.InteriorPlants.speciesOf(region.get(x, y, z));
                            if (sp != null && sp.marine()) coral++;
                        }
                    }
                    send("reef " + Math.round(t.salinity() * 10) + " " + org.overengineer.bettacraft.client.tank.TankLightTint.saltOf(c.getBlockPos()) + " "
                            + fish + " " + coral);
                });
            }
            case "seahorses" -> {
                // M21.8: the seahorses and the full brood pouch on this client, the cyst stack size it uses, the jars in slot 0 and 1 (M35.1: damaged?)
                BlockPos min = pos(args);
                then(60, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    var box = new net.minecraft.world.phys.AABB(min).expandTowards(2, 2, 2);
                    var mates = mc.level.getEntitiesOfClass(org.overengineer.bettacraft.entity.tankmate.TankmateEntity.class, box, m -> m.species().seahorse());
                    long pouches = mates.stream().filter(m -> m.isGravid() && !m.isFemale()).count();
                    int stack = new net.minecraft.world.item.ItemStack(org.overengineer.bettacraft.registry.ModItems.ARTEMIA_CYSTS.get()).getMaxStackSize();
                    var s0 = mc.player.getInventory().getItem(0);
                    var s1 = mc.player.getInventory().getItem(1);
                    send("seahorses " + mates.size() + " " + pouches + " " + stack + " " + s0.getCount() + " " + (s0.has(net.minecraft.core.component.DataComponents.DAMAGE) ? 1 : 0)
                            + " " + s1.getCount() + " " + (s1.has(net.minecraft.core.component.DataComponents.DAMAGE) ? 1 : 0));
                });
            }
            case "sizes" -> {
                // M36: the betta size the server sent and the hitboxes of the bettas of the main tank on this client
                BlockPos min = pos(args);
                then(20, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    var list = mc.level.getEntitiesOfClass(org.overengineer.bettacraft.entity.betta.BettaEntity.class, new net.minecraft.world.phys.AABB(min).inflate(8));
                    float f = org.overengineer.bettacraft.config.AnimalSizes.bettaFactor();
                    boolean follow = list.stream().allMatch(b -> Math.abs(b.getBbWidth() - 0.5F * b.lifeStage().scale() * f) < 1e-3F);
                    send("sizes " + org.overengineer.bettacraft.config.AnimalSizes.betta().name() + " " + list.size() + " " + follow);
                });
            }
            case "wildlife" -> {
                // M22.3: the wild small animals near the pool (species and colour as drawn) and the genes switch of the server
                BlockPos min = pos(args);
                then(40, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    var list = mc.level.getEntitiesOfClass(org.overengineer.bettacraft.entity.critter.WildCritterEntity.class, new net.minecraft.world.phys.AABB(min).inflate(6));
                    String kinds = list.stream().map(e -> e.drawn().species.id() + ":" + e.drawn().clientColor).sorted()
                            .collect(java.util.stream.Collectors.joining(","));
                    send("wildlife " + list.size() + " " + (kinds.isEmpty() ? "-" : kinds) + " " + org.overengineer.bettacraft.genetics.GeneVisibility.free());
                });
            }
            case "cabinets" -> {
                // M21.7: the material of the two cabinets on this client and the sound of the stone one
                BlockPos at = pos(args);
                then(40, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    String[] w = new String[2];
                    for (int i = 0; i < 2; i++) {
                        w[i] = mc.level.getBlockEntity(at.offset(0, 0, i)) instanceof org.overengineer.bettacraft.equipment.CabinetBlockEntity cab ? cab.woodId().toString() : "none";
                    }
                    var sound = mc.level.getBlockState(at).getSoundType(mc.level, at, mc.player);
                    send("cabinets " + w[0] + " " + w[1] + " " + (sound == net.minecraft.world.level.block.SoundType.DEEPSLATE_BRICKS ? "deepslate_bricks" : "other"));
                });
            }
            case "m44" -> {
                // M44: the fronts of the four cabinets (from the viewer's left; "-" = no joints) and, over 100 ticks, the walk
                // animation and the ground of the zombie in glass in the M31 terrarium
                String[] two = args.split(";");
                BlockPos at = pos(two[0]), m31 = pos(two[1]);
                double[] acc = new double[3];
                then(40, () -> {});
                for (int k = 0; k < 100; k++) {
                    then(1, () -> {
                        Minecraft mc = Minecraft.getInstance();
                        net.minecraft.world.entity.monster.zombie.Zombie z = null;
                        for (var e : mc.level.getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Zombie.class, new net.minecraft.world.phys.AABB(m31).inflate(8))) {
                            if (z == null || e.getScale() < z.getScale()) z = e;
                        }
                        if (z == null) return;
                        acc[0] += z.walkAnimation.speed();
                        acc[1]++;
                        if (z.onGround()) acc[2]++;
                    });
                }
                then(1, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    StringBuilder b = new StringBuilder();
                    for (int i = 0; i < 4; i++) {
                        var st = mc.level.getBlockState(at.offset(3 - i, 0, 0));
                        if (!st.hasProperty(org.overengineer.bettacraft.equipment.CabinetBlock.FRONT)) {
                            b.append('?');
                            continue;
                        }
                        b.append(switch (st.getValue(org.overengineer.bettacraft.equipment.CabinetBlock.FRONT)) {
                            case TWO -> 'T';
                            case PAIR_L -> 'L';
                            case PAIR_R -> 'R';
                            case SINGLE -> 'S';
                            case DRAWERS -> 'D';
                        });
                        if (!st.getValue(org.overengineer.bettacraft.equipment.CabinetBlock.JOINTS)) b.append('-');
                    }
                    long walk = acc[1] == 0 ? -1 : Math.round(acc[0] / acc[1] * 100), ground = acc[1] == 0 ? -1 : Math.round(acc[2] / acc[1] * 100);
                    send("m44 " + b + " " + walk + " " + ground);
                });
            }
            case "outlets" -> {
                // M22.4: the outlet side of the two cabinets as this client knows it (block entity update)
                BlockPos at = pos(args);
                then(30, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    String[] w = new String[2];
                    for (int i = 0; i < 2; i++) {
                        w[i] = mc.level.getBlockEntity(at.offset(0, 0, i)) instanceof org.overengineer.bettacraft.equipment.CabinetBlockEntity cab ? cab.outletSide().id() : "none";
                    }
                    send("outlets " + w[0] + "," + w[1]);
                });
            }
            case "cabpump" -> {
                // M29.1: the air pump in the cabinet as this client knows it, and its pump sound switches (client config)
                BlockPos at = pos(args);
                then(30, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    boolean pump = mc.level.getBlockEntity(at) instanceof org.overengineer.bettacraft.equipment.CabinetBlockEntity cab && cab.hasAirPump();
                    send("cabpump " + pump + "," + org.overengineer.bettacraft.config.ClientConfig.AIR_PUMP_SOUND.get() + ","
                            + org.overengineer.bettacraft.config.ClientConfig.FLOW_PUMP_SOUND.get());
                });
            }
            case "aeration" -> {
                // M23: the air pump runs on this client, the miniature shows both air stones bubbling, both halves of the panel
                BlockPos at = pos(args);
                then(80, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    boolean runs = mc.level.getBlockEntity(at.offset(-1, 0, 0)) instanceof org.overengineer.bettacraft.equipment.AirPumpBlockEntity pump && pump.running();
                    var c = org.overengineer.bettacraft.tank.TankMultiblock.getController(mc.level, at);
                    var region = c == null || c.getTank() == null ? null : org.overengineer.bettacraft.client.interior.ClientInteriorCache.get(c.getTank().regionId());
                    int bubblers = region == null ? -1 : region.bubblers().size();
                    int panels = 0;
                    for (int z = 0; z < 2; z++) if (mc.level.getBlockState(at.offset(0, 2, z)).getBlock() instanceof org.overengineer.bettacraft.equipment.LedPanelBlock) panels++;
                    send("aeration " + runs + "," + bubblers + "," + panels);
                });
            }
            case "monitor" -> {
                // M27: the screen formed on this client (the top left block is the controller), the automatic layout came with
                // the block entity, the tank's data with the bundled monitor payload
                BlockPos at = pos(args);
                then(100, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (!(mc.level.getBlockEntity(at) instanceof org.overengineer.bettacraft.monitor.MonitorBlockEntity m)) {
                        send("monitor none");
                        return;
                    }
                    // other stages' tanks are in range too: the automatic layout has two tiles per tank, at most four on 2x1
                    var t = m.clientTanks.stream().filter(k -> "MPTank".equals(k.name)).findFirst().orElse(null);
                    boolean tiles = m.layout().auto && m.layout().tiles.size() == Math.min(4, 2 * m.clientTanks.size());
                    send("monitor " + m.width() + "x" + m.height() + "," + (tiles ? "auto" : "layout" + m.layout().tiles.size() + "/" + m.clientTanks.size())
                            + "," + (t == null ? "-" : t.name) + "," + (t != null && t.live) + " " + (t == null ? "NaN" : String.format(java.util.Locale.ROOT, "%.2f",
                            t.value(org.overengineer.bettacraft.monitor.MonitorValue.TEMPERATURE))));
                });
            }
            case "monitorlayout" -> then(20, () -> {
                // M27: Dev1 sends a layout like the editor of part b will: one pH tile of the tank
                Minecraft mc = Minecraft.getInstance();
                BlockPos at = pos(args);
                var t = mc.level.getBlockEntity(at) instanceof org.overengineer.bettacraft.monitor.MonitorBlockEntity m
                        ? m.clientTanks.stream().filter(k -> "MPTank".equals(k.name)).findFirst().orElse(null) : null;
                if (t == null) {
                    send("error monitor layout: no screen data");
                    return;
                }
                var l = new org.overengineer.bettacraft.monitor.DashboardLayout();
                l.auto = false;
                l.tiles.add(org.overengineer.bettacraft.monitor.DashboardTile.value(l.nextId++, 0, 0, 4, 4, t.id,
                        org.overengineer.bettacraft.monitor.MonitorValue.PH, false));
                // part b: an alarm with redstone (the tank is warmer than 20 °C) and a traffic light (needs the detail)
                l.tiles.add(org.overengineer.bettacraft.monitor.DashboardTile.alarm(l.nextId++, 4, 0, 2, 4, t.id,
                        org.overengineer.bettacraft.monitor.MonitorValue.TEMPERATURE, false, 20F, true));
                l.tiles.add(new org.overengineer.bettacraft.monitor.DashboardTile(l.nextId++, 6, 0, 2, 4,
                        org.overengineer.bettacraft.monitor.DashboardTile.STATUS, t.id));
                org.overengineer.bettacraft.platform.network.PacketDistributor.sendToServer(new org.overengineer.bettacraft.network.MonitorLayoutPayload(at, l.save()));
            });
            case "monitorseen" -> then(60, () -> {
                Minecraft mc = Minecraft.getInstance();
                BlockPos at = pos(args);
                if (!(mc.level.getBlockEntity(at) instanceof org.overengineer.bettacraft.monitor.MonitorBlockEntity m)) {
                    send("monitorseen none");
                    return;
                }
                // part b: the alarm is triggered on this client too (it blinks), the traffic light has the tank's detail
                var tiles = m.layout().tiles;
                boolean alarm = tiles.size() > 1 && tiles.get(1).triggered(m.clientTank(tiles.get(1).tank));
                boolean detail = tiles.size() > 2 && m.clientTank(tiles.get(2).tank) != null && !m.clientTank(tiles.get(2).tank).detail.isEmpty();
                send("monitorseen " + tiles.size() + "," + m.layout().auto + "," + (tiles.isEmpty() ? "-" : tiles.get(0).value().id)
                        + "," + (alarm ? "alarm" : "quiet") + "," + (detail ? "detail" : "nodetail"));
            });
            case "plumbing" -> {
                // M24: the pipe between reservoir and pump joins both, the controller's settings reached this client
                BlockPos at = pos(args);
                then(60, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    var pipe = mc.level.getBlockState(at.offset(-2, 0, 0));
                    boolean joins = pipe.getBlock() instanceof org.overengineer.bettacraft.plumbing.PipeBlock
                            && pipe.getValue(org.overengineer.bettacraft.plumbing.PipeBlock.SIDES.get(net.minecraft.core.Direction.WEST))
                            && pipe.getValue(org.overengineer.bettacraft.plumbing.PipeBlock.SIDES.get(net.minecraft.core.Direction.EAST));
                    String wc = mc.level.getBlockEntity(at.offset(-1, 0, 1)) instanceof org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity w
                            ? w.intervalDays() + "," + w.share() : "none";
                    send("plumbing " + joins + "," + wc);
                });
            }
            case "pipecover" -> {
                // M40 part c: the covers of a pipe and a bundle on this client (block entity update), the bundle's lines, its light,
                // the pipe's shape; the first report "pipecover", the second (after the server took the pipe's cover off) "pipecoveroff"
                BlockPos at = pos(args);
                boolean second = pipeCoverSeen;
                pipeCoverSeen = true;
                then(60, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    BlockPos b = at.east();
                    var ca = org.overengineer.bettacraft.plumbing.PipeCovers.coverAt(mc.level, at);
                    var cb = org.overengineer.bettacraft.plumbing.PipeCovers.coverAt(mc.level, b);
                    int lines = org.overengineer.bettacraft.plumbing.PipeSlots.count(org.overengineer.bettacraft.plumbing.Pipes.slotsAt(mc.level, b));
                    int light = mc.level.getBlockState(b).getLightEmission(mc.level, b);
                    boolean full = net.minecraft.world.level.block.Block.isShapeFullBlock(mc.level.getBlockState(at).getShape(mc.level, at));
                    send((second ? "pipecoveroff " : "pipecover ") + (ca == null ? "none" : net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(ca.getBlock()))
                            + "," + (cb == null ? "none" : net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(cb.getBlock())) + "," + lines + "," + light + "," + full);
                });
            }
            case "waterchange" -> then(20, () -> {
                // M24: Dev1 sets the water change controller like its screen does and starts a change
                LOGGER.info("[mp] sending the water change settings for {}", pos(args));
                org.overengineer.bettacraft.platform.network.PacketDistributor.sendToServer(new org.overengineer.bettacraft.network.WaterChangerPayload(pos(args), 3, 30, true, true));
            });
            case "regulate" -> then(20, () -> {
                // M23: Dev1 sets the air pump to 50 % like the regulator screen does
                LOGGER.info("[mp] sending the regulator for {}", pos(args));
                org.overengineer.bettacraft.platform.network.PacketDistributor.sendToServer(new org.overengineer.bettacraft.network.RegulatorPayload(pos(args), 5, false));
            });
            case "outletmenu" -> {
                // M22.4: Dev1 opens the cabinet menu with a right-click on the cabinet's front and presses the outlet button
                // like the tank menu step: a right-click (use key) on the cabinet the server turned the player to
                then(30, () -> net.minecraft.client.KeyMapping.click(Minecraft.getInstance().options.keyUse.getKey()));
                whenReady(40, 10, () -> Minecraft.getInstance().gui.screen() instanceof org.overengineer.bettacraft.client.screen.CabinetScreen cs
                        && cs.getMenu().outletSide() != null, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (!(mc.gui.screen() instanceof org.overengineer.bettacraft.client.screen.CabinetScreen s) || mc.gameMode == null) {
                        send("outletmenu none:" + mc.gui.screen() + "_hit:" + (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult b ? b.getBlockPos().toShortString().replace(" ", "")
                                + "_" + mc.level.getBlockState(b.getBlockPos()).getBlock() : String.valueOf(mc.hitResult)).replace(" ", "_") + "_mode:" + mc.gameMode.getPlayerMode()
                                + "_at:" + mc.player.blockPosition().toShortString().replace(" ", ""));
                        return;
                    }
                    var side = s.getMenu().outletSide();
                    if (side != org.overengineer.bettacraft.equipment.OutletSide.LEFT) {
                        send("outletmenu side:" + side);
                        return;
                    }
                    double x = s.getGuiLeft() + org.overengineer.bettacraft.client.screen.CabinetScreen.OUTLET_X + 10;
                    double y = s.getGuiTop() + org.overengineer.bettacraft.client.screen.CabinetScreen.OUTLET_Y + 5;
                    s.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(0, 0)), false); // left -> right
                    s.mouseReleased(new net.minecraft.client.input.MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(0, 0)));
                    send("outletmenu ok");
                });
            }
            case "container" -> container(args);
            case "place" -> then(12, () -> {
                // M15: right-click where the server turned the player (in a tank interior)
                Minecraft mc = Minecraft.getInstance();
                HitResult hit = mc.hitResult;
                LOGGER.info("[mp] {} places {} on {}", name(), mc.player.getMainHandItem(),
                        hit instanceof BlockHitResult b ? mc.level.getBlockState(b.getBlockPos()) + " at " + b.getBlockPos() + " " + b.getDirection() : hit);
                KeyMapping.click(mc.options.keyUse.getKey());
            });
            case "seen" -> then(30, () -> sendSeen(args));
            case "mini" -> {
                String[] s = args.split(",", 2);
                then(60, () -> sendMini(s[0], pos(s[1])));
            }
            case "sneak" -> sneak(args);
            case "mine" -> {
                String[] s = args.split(",", 2);
                mine(s[0], pos(s[1]));
            }
            case "menu" -> menu();
            case "motion" -> {
                String[] s = args.split(",", 2);
                tank = pos(s[1]);
                int n = Integer.parseInt(s[0]);
                then(20, () -> startSampling(n * 3));
            }
            case "shot" -> {
                Minecraft mc = Minecraft.getInstance();
                then(40, () -> org.overengineer.bettacraft.client.DevHud.setHidden(mc, true));
                then(30, () -> shot("tank"));
                then(1, () -> org.overengineer.bettacraft.client.DevHud.setHidden(mc, false));
                then(80, () -> shot("overlay")); // Jade / The One Probe / WTHIT on the tank under the crosshair
                then(5, () -> send("shot"));
            }
            case "features" -> then(20, () -> sendFeatures(args)); // M33 part b
            case "preset" -> then(20, () -> send(args + " "
                    + org.overengineer.bettacraft.config.Features.remote() + " " + org.overengineer.bettacraft.config.Presets.active().id() + " "
                    + org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.BREEDING) + " "
                    + org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.CARE_MODE, null))); // M33 part c
            case "quit" -> then(20, () -> {
                Minecraft mc = Minecraft.getInstance();
                LOGGER.info("[mp] {} client finished", name());
                mc.disconnectFromWorld(net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE);
                quitIn = 40;
            });
            default -> LOGGER.warn("[mp] unknown script line {}", line);
        }
    }

    private static BlockPos pos(String s) {
        String[] p = s.split(",");
        return new BlockPos(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()), Integer.parseInt(p[2].trim()));
    }

    /** M33 part b: {@code <phase> <remote> <tankmates on> <neon buckets in the tab> <betta buckets in the tab>} on this client. */
    private static void sendFeatures(String phase) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        org.overengineer.bettacraft.client.ClientFeatures.invalidateCreativeTabs();
        net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(mc.player.connection.enabledFeatures(), false, mc.level.registryAccess());
        var items = org.overengineer.bettacraft.registry.ModCreativeTabs.MAIN.get().getDisplayItems();
        long neons = items.stream().filter(s -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().equals("neon_tetra_bucket")).count();
        boolean bettas = items.stream().anyMatch(s -> s.is(org.overengineer.bettacraft.registry.ModItems.BETTA_BUCKET.get()));
        send(phase + " " + org.overengineer.bettacraft.config.Features.remote() + " "
                + org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.TANKMATES) + " " + neons + " " + bettas);
    }

    private static void send(String report) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        LOGGER.info("[mp] {} report: {}", name(), report);
        mc.player.connection.sendCommand("bettacraftmp " + report);
    }

    @Nullable
    private static TankData clientTank() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || tank == null) return null;
        if (!(mc.level.getBlockEntity(tank) instanceof TankBlockEntity be) || !be.isController()) return null;
        return be.getTank();
    }

    /** What this client sees: water, plants (ids, the drawn positions) and the fish in the tank. */
    private static void sendState(String phase) {
        Minecraft mc = Minecraft.getInstance();
        TankData t = clientTank();
        if (t == null) {
            send("error state " + phase + ": no tank on the client at " + tank);
            return;
        }
        double time = mc.level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        long ids = 0;
        double cx = 0, cz = 0;
        for (FloatingPlant p : t.plants()) {
            ids += p.id;
            cx += p.renderX(time, TankPlants.SYNC_TICKS);
            cz += p.renderZ(time, TankPlants.SYNC_TICKS);
        }
        int n = t.plants().size();
        if (n > 0) {
            cx /= n;
            cz /= n;
        }
        var box = new net.minecraft.world.phys.AABB(net.minecraft.world.phys.Vec3.atCenterOf(t.min()), net.minecraft.world.phys.Vec3.atCenterOf(t.max())).inflate(0.6);
        int fish = mc.level.getEntitiesOfClass(BettaEntity.class, box).size();
        send(String.format(Locale.ROOT, "state %s %d %d %d %.4f %.4f %d", phase, t.waterMb(), n, ids, cx, cz, fish));
        shot("state" + phase);
    }

    /** M14: {@code critters <phase> <count> <idsum> <cx> <cy> <cz> <algae faces>} as this client has them. */
    private static void sendCritters(String phase) {
        TankData t = clientTank();
        if (t == null) {
            send("error critters " + phase + ": no tank on the client at " + tank);
            return;
        }
        long ids = 0;
        double x = 0, y = 0, z = 0;
        for (var k : t.critters()) {
            ids += k.id;
            x += k.x;
            y += k.y;
            z += k.z;
        }
        int n = t.critters().size();
        if (n > 0) {
            x /= n;
            y /= n;
            z /= n;
        }
        int algae = 0;
        for (var e : t.algae().values()) for (float v : e) if (v > 0) algae++;
        send(String.format(Locale.ROOT, "critters %s %d %d %.4f %.4f %.4f %d", phase, n, ids, x, y, z, algae));
        shot("critters" + phase);
    }

    /** M13: {@code aqua <phase> <plant blocks> <version>}: aquarium plant blocks in this client's copy of the tank interior. */
    private static void sendAquatic(String phase) {
        TankData t = clientTank();
        var region = t == null || t.regionId() < 0 ? null : org.overengineer.bettacraft.client.interior.ClientInteriorCache.get(t.regionId());
        if (region == null) {
            send("error aqua " + phase + ": no miniature on the client for " + tank);
            return;
        }
        int n = 0;
        int sx = t.sizeX() * t.scale(), sy = t.sizeY() * t.scale(), sz = t.sizeZ() * t.scale();
        for (int x = 0; x < sx; x++)
            for (int y = 0; y < sy; y++)
                for (int z = 0; z < sz; z++) if (region.get(x, y, z).getBlock() instanceof org.overengineer.bettacraft.plants.AquaticPlantBlock) n++;
        send(String.format(Locale.ROOT, "aqua %s %d", phase, n));
        shot("aqua" + phase);
    }

    // ------------------------------------------------------------------ M15: two players in one interior

    /** Cobblestone (Dev1) and planks (Dev2) this client sees around itself in the interior. */
    private static void sendSeen(String phase) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.level.dimension() != org.overengineer.bettacraft.registry.ModDimensions.TANK_INTERIOR) {
            send("error seen " + phase + ": not in a tank interior");
            return;
        }
        int cob = 0, pl = 0;
        BlockPos at = mc.player.blockPosition();
        for (BlockPos q : BlockPos.betweenClosed(at.offset(-48, -2, -48), at.offset(48, 16, 48))) {
            var st = mc.level.getBlockState(q);
            if (st.is(net.minecraft.world.level.block.Blocks.COBBLESTONE)) cob++;
            else if (st.is(net.minecraft.world.level.block.Blocks.OAK_PLANKS)) pl++;
        }
        send("seen " + phase + " " + cob + " " + pl);
        shot("interior_seen" + phase);
    }

    /** Cobblestone and planks in this client's miniature of the tank at {@code at}. */
    private static void sendMini(String phase, BlockPos at) {
        Minecraft mc = Minecraft.getInstance();
        TankBlockEntity c = mc.level == null ? null : org.overengineer.bettacraft.tank.TankMultiblock.getController(mc.level, at);
        TankData t = c == null ? null : c.getTank();
        var region = t == null || t.regionId() < 0 ? null : org.overengineer.bettacraft.client.interior.ClientInteriorCache.get(t.regionId());
        if (region == null) {
            send("error mini " + phase + ": no miniature on the client for " + at);
            return;
        }
        int cob = 0, pl = 0;
        int sx = t.sizeX() * t.scale(), sy = t.sizeY() * t.scale(), sz = t.sizeZ() * t.scale();
        for (int x = 0; x < sx; x++)
            for (int y = 0; y < sy; y++)
                for (int z = 0; z < sz; z++) {
                    var st = region.get(x, y, z);
                    if (st.is(net.minecraft.world.level.block.Blocks.COBBLESTONE)) cob++;
                    else if (st.is(net.minecraft.world.level.block.Blocks.OAK_PLANKS)) pl++;
                }
        send("mini " + phase + " " + cob + " " + pl);
        shot("miniature" + phase);
    }

    /** Holds sneak with the given pitch for some ticks (to leave a tank interior: look up); screenshots of the hint. */
    /**
     * M22.1: right-click with the item the server put into the hand, looking where the server turned the player: "sea"
     * (a sea water bucket into deep water), "ask" (a betta bucket onto a stone pad: the confirm screen must open, then
     * "Cancel"), "release" (the same, now "Empty"), "pour" (a bottle, sneaking). Then reports the hand, the block at the
     * spot and the bettas near it as this client sees them.
     */
    private static void container(String args) {
        Minecraft mc = Minecraft.getInstance();
        String[] s = args.split(",", 2);
        String what = s[0];
        BlockPos at = pos(s[1]);
        boolean[] screen = new boolean[1];
        if (what.equals("pour")) then(15, () -> mc.options.keyShift.setDown(true));
        then(15, () -> KeyMapping.click(mc.options.keyUse.getKey()));
        then(20, () -> {
            mc.options.keyShift.setDown(false);
            screen[0] = mc.gui.screen() instanceof org.overengineer.bettacraft.client.screen.ConfirmEmptyScreen;
            if (what.equals("ask") && screen[0]) shot("confirm_screen");
            if (mc.gui.screen() instanceof org.overengineer.bettacraft.client.screen.ConfirmEmptyScreen c) {
                if (what.equals("release")) c.confirm();
                else c.cancel();
            }
        });
        then(40, () -> {
            var st = mc.level.getBlockState(at);
            String block = st.isAir() ? "air" : mc.level.getFluidState(at).is(net.minecraft.tags.FluidTags.WATER) ? "water"
                    : net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath();
            int bettas = mc.level.getEntitiesOfClass(BettaEntity.class, new net.minecraft.world.phys.AABB(at).inflate(3)).size();
            String hand = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(mc.player.getMainHandItem().getItem()).toString();
            send("containers " + what + " " + hand + " " + block + " " + bettas + " " + (screen[0] ? "screen" : "none"));
        });
    }

    private static void sneak(String args) {
        Minecraft mc = Minecraft.getInstance();
        String[] s = args.split(",");
        float pitch = Float.parseFloat(s[0]);
        int ticks = Integer.parseInt(s[1]);
        then(10, () -> mc.player.setXRot(pitch));
        then(10, () -> shot("leave_hint_" + (pitch < 0 ? "up" : "down")));
        then(1, () -> mc.options.keyShift.setDown(true));
        then(Math.min(40, ticks / 2), () -> {
            mc.player.setXRot(pitch);
            shot("leave_progress_" + (pitch < 0 ? "up" : "down"));
        });
        then(Math.max(1, ticks - Math.min(40, ticks / 2)), () -> mc.options.keyShift.setDown(false));
        then(5, () -> send("sneak " + (mc.level != null && mc.level.dimension() == org.overengineer.bettacraft.registry.ModDimensions.TANK_INTERIOR ? "inside" : "outside")));
    }

    /** Mines the block at {@code at} like a player (start, keep hitting) and reports whether it is still there. */
    private static void mine(String phase, BlockPos at) {
        Minecraft mc = Minecraft.getInstance();
        lastOverlay = "";
        then(20, () -> {
            HitResult hit = mc.hitResult;
            LOGGER.info("[mp] {} mines {} (crosshair on {})", name(), at, hit instanceof BlockHitResult b ? b.getBlockPos() + " " + b.getDirection() : hit);
            if (!(hit instanceof BlockHitResult b) || !b.getBlockPos().equals(at)) send("error mine: the crosshair is not on " + at + ": " + hit);
            mc.gameMode.startDestroyBlock(at, net.minecraft.core.Direction.NORTH);
        });
        for (int i = 0; i < 40; i++) {
            then(1, () -> {
                if (mc.level.getBlockState(at).getBlock() instanceof TankBlock) {
                    mc.gameMode.continueDestroyBlock(at, net.minecraft.core.Direction.NORTH);
                    mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                }
            });
        }
        then(1, () -> mc.gameMode.stopDestroyBlock());
        then(1, () -> shot("mine" + phase));
        then(30, () -> {
            boolean still = mc.level.getBlockState(at).getBlock() instanceof TankBlock;
            LOGGER.info("[mp] {} action bar: {}", name(), lastOverlay);
            send("mine " + phase + " " + (still ? 1 : 0) + " " + (lastOverlay.contains("Dev1") ? 1 : 0));
        });
    }

    /** Right-click like a player: the use key, with the crosshair where the server turned the player. */
    private static void use(boolean sneak) {
        Minecraft mc = Minecraft.getInstance();
        then(30, () -> {
            if (sneak) mc.options.keyShift.setDown(true);
        });
        then(8, () -> {
            HitResult hit = mc.hitResult;
            boolean onTank = hit instanceof BlockHitResult b && mc.level.getBlockState(b.getBlockPos()).getBlock() instanceof TankBlock;
            LOGGER.info("[mp] {} uses {} (sneak {}) on {}", name(), mc.player.getMainHandItem(), sneak,
                    hit instanceof BlockHitResult b ? mc.level.getBlockState(b.getBlockPos()) + " " + b.getDirection() : hit);
            if (!onTank) send("error the crosshair is not on the tank: " + hit);
            KeyMapping.click(mc.options.keyUse.getKey());
        });
        then(15, () -> mc.options.keyShift.setDown(false));
    }

    /**
     * Opens the tank menu with an empty hand (from the side: no plant under the crosshair), checks that the seven tab
     * labels fit, goes to the plants tab (screenshot), presses "Thin out", then the water tab (screenshot), closes it.
     */
    private static void menu() {
        Minecraft mc = Minecraft.getInstance();
        StringBuilder result = new StringBuilder();
        then(30, () -> KeyMapping.click(mc.options.keyUse.getKey()));
        then(40, () -> {
            if (!(mc.gui.screen() instanceof TankScreen ts)) {
                result.append("no menu: ").append(mc.gui.screen());
                return;
            }
            result.append(tabsFit(mc, ts.tabButtonsForTest(), ts.panelBounds()[2] - 12));
            ts.selectTabForTest(0);
        });
        then(20, () -> shot("menu_overview"));
        then(1, () -> {
            if (mc.gui.screen() instanceof TankScreen ts) ts.showPlantsPageForTest();
        });
        then(30, () -> shot("menu_plants"));
        then(1, () -> {
            if (mc.gui.screen() instanceof TankScreen ts && !ts.pressThinForTest()) result.append(" thin button inactive");
        });
        then(40, () -> shot("menu_plants_thinned"));
        then(1, () -> {
            if (mc.gui.screen() instanceof TankScreen ts) ts.selectTabForTest(5);
        });
        then(30, () -> shot("menu_water"));
        then(1, () -> {
            if (mc.player != null) mc.player.closeContainer();
        });
        then(10, () -> {
            String r = result.toString().trim();
            send("menu " + (r.startsWith("tabs ok") && !r.contains("inactive") ? "ok " : "bad ") + r.replace(' ', '_'));
        });
    }

    /** The tab buttons: inside the menu, side by side without overlap, each label (drawn at the tab scale) inside its button. */
    static String tabsFit(Minecraft mc, List<Button> tabs, int menuW) {
        int left = tabs.getFirst().getX(), right = tabs.getLast().getX() + tabs.getLast().getWidth();
        StringBuilder bad = new StringBuilder();
        for (int i = 0; i < tabs.size(); i++) {
            Button b = tabs.get(i);
            float label = mc.font.width(b.getMessage()) * TankScreen.TAB_LABEL_SCALE;
            if (label > b.getWidth() - 2) bad.append(b.getMessage().getString()).append(" too long ");
            if (i > 0 && b.getX() < tabs.get(i - 1).getX() + tabs.get(i - 1).getWidth()) bad.append("overlap ");
        }
        if (right - left > menuW) bad.append("wider than the menu "); // M62: the menu grows with the screen
        return bad.isEmpty() ? "tabs ok (" + tabs.size() + " tabs, " + (right - left) + " px)" : "tabs bad: " + bad;
    }

    // ------------------------------------------------------------------ motion

    private static void startSampling(int frameCount) {
        lastPos.clear();
        firstPos.clear();
        frames = 0;
        jumps = 0;
        maxStep = 0;
        sampleFrames = frameCount;
    }

    /** Every rendered frame: the drawn position of each plant (as the renderer computes it). */
    private static void frame(float partial) {
        if (sampleFrames < 0) return;
        Minecraft mc = Minecraft.getInstance();
        TankData t = clientTank();
        if (t == null || mc.level == null) return;
        double time = mc.level.getGameTime() + partial;
        for (FloatingPlant p : t.plants()) {
            double x = p.renderX(time, TankPlants.SYNC_TICKS), z = p.renderZ(time, TankPlants.SYNC_TICKS);
            double[] last = lastPos.get(p.id);
            if (last != null) {
                double step = Math.hypot(x - last[0], z - last[1]);
                maxStep = Math.max(maxStep, step);
                if (step > 0.05) jumps++;
            }
            lastPos.put(p.id, new double[]{x, z});
            firstPos.putIfAbsent(p.id, new double[]{x, z});
        }
        frames++;
        if (frames >= sampleFrames) {
            sampleFrames = -1;
            int moved = 0;
            double sum = 0;
            for (var e : lastPos.entrySet()) {
                double[] f = firstPos.get(e.getKey());
                double d = Math.hypot(e.getValue()[0] - f[0], e.getValue()[1] - f[1]);
                sum += d;
                if (d > 1e-4) moved++;
            }
            double mean = lastPos.isEmpty() ? 0 : sum / lastPos.size();
            send(String.format(Locale.ROOT, "motion %d %d %d %.5f %.4f %d", frames, moved, lastPos.size(), maxStep, mean, jumps));
        }
    }

    // ------------------------------------------------------------------ ticks

    private static void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    /** M50: how many states of travelling tanks this client got (only with Create). */
    private static int travelReceived() {
        if (!org.overengineer.bettacraft.platform.Platform.get().isModLoaded("create")) return -1;
        try {
            return (int) Class.forName("org.overengineer.bettacraft.compat.create.CreateTravelClient").getField("received").get(null); // M52b: by name (no Create on 1.21.11)
        } catch (ReflectiveOperationException | LinkageError e) {
            return -1;
        }
    }

    /** M50: the packed animals of the tank on this contraption entity as this client's copy of it shows them (reflection on Create). */
    private static int travelPacked(net.minecraft.world.entity.Entity contraption) {
        try {
            Object c = contraption.getClass().getMethod("getContraption").invoke(contraption);
            @SuppressWarnings("unchecked")
            java.util.Map<BlockPos, ?> blocks = (java.util.Map<BlockPos, ?>) c.getClass().getMethod("getBlocks").invoke(c);
            int n = -1;
            for (BlockPos local : blocks.keySet()) {
                Object be = c.getClass().getMethod("getBlockEntityClientSide", BlockPos.class).invoke(c, local);
                if (be instanceof org.overengineer.bettacraft.tank.TankBlockEntity t && t.contraptionView) n = Math.max(n, t.stowed().size());
            }
            return n;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return -2;
        }
    }

    /**
     * M42 part b: runs {@code action} once {@code ready} holds, checked every {@code delay} ticks at most {@code tries} times (then it
     * runs anyway and reports what the client has): with a slow connection (-Pmplag) the data may come a moment later.
     */
    private static void whenReady(int delay, int tries, java.util.function.BooleanSupplier ready, Runnable action) {
        then(delay, () -> {
            if (ready.getAsBoolean() || tries <= 1) action.run();
            else steps.addFirst(new Step(delay, () -> whenReady0(delay, tries - 1, ready, action)));
        });
    }

    private static void whenReady0(int delay, int tries, java.util.function.BooleanSupplier ready, Runnable action) {
        if (ready.getAsBoolean() || tries <= 1) action.run();
        else steps.addFirst(new Step(delay, () -> whenReady0(delay, tries - 1, ready, action)));
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        mc.options.pauseOnLostFocus = false;
        if (retryAt == ticks && mc.gui.screen() instanceof DisconnectedScreen) connect(new TitleScreen());
        if (quitIn > 0 && --quitIn == 0) {
            mc.stop();
            return;
        }
        if (mc.player == null || mc.level == null) return;
        // M22.3: a player that died (here or in an earlier run) respawns at once instead of waiting on the death screen
        if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.DeathScreen) {
            mc.player.respawn();
            mc.gui.setScreen(null);
        }
        if (!sizeSet) {
            sizeSet = true;
            mc.getWindow().setWindowed(1280, 960);
            mc.options.guiScale().set(3);
            mc.resizeGui();
        }
        if (readyIn > 0 && --readyIn == 0) {
            if (clientTank() != null) {
                ready = true;
                send("ready");
            } else {
                readyIn = 40; // not there yet
            }
        }
        Step s = steps.peek();
        if (s == null) return;
        if (wait < s.delay()) {
            wait++;
            return;
        }
        steps.poll();
        wait = 0;
        try {
            s.action().run();
        } catch (RuntimeException e) {
            LOGGER.error("[mp] step failed", e);
            send("error " + e.getClass().getSimpleName() + "_" + String.valueOf(e.getMessage()).replace(' ', '_'));
        }
    }

    private static void shot(String what) {
        Minecraft mc = Minecraft.getInstance();
        mc.gui.toastManager().clear();
        String file = "mp_" + name().toLowerCase(Locale.ROOT) + "_" + what + ".png";
        Screenshot.grab(mc.gameDirectory, file, mc.gameRenderer.mainRenderTarget(), 1, c -> LOGGER.info("[mp] {}", c.getString()));
    }
}
