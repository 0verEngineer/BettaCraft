package org.overengineer.bettacraft.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.tank.TankInhabitants.Acceptance;

/** M16: the acceptance rules for animals of all mods (order of the checks, config patterns). */
class InhabitantRulesTest {
    private static InhabitantRules.Facts fish(String id, float w, float h) {
        return new InhabitantRules.Facts(id, w, h, false, false, false, false, true);
    }

    private static Acceptance decide(InhabitantRules.Facts f) {
        return InhabitantRules.decide(f, InhabitantRules.Settings.DEFAULT);
    }

    @Test
    void sizeLimitIsInclusive() {
        assertEquals(Acceptance.OK, decide(fish("minecraft:squid", 0.8F, 0.8F)), "a squid (0.8) fits");
        assertEquals(Acceptance.TOO_BIG, decide(fish("minecraft:dolphin", 0.9F, 0.6F)));
        assertEquals(Acceptance.TOO_BIG, decide(fish("livingthings:seahorse_tall", 0.2F, 0.81F)), "height counts too");
    }

    @Test
    void orderOfTheChecks() {
        var boss = new InhabitantRules.Facts("cataclysm:the_leviathan", 4.5F, 3F, false, false, true, true, true);
        assertEquals(Acceptance.BOSS, decide(boss), "bosses before size");
        var lionfish = new InhabitantRules.Facts("cataclysm:lionfish", 0.6F, 0.55F, false, false, false, true, true);
        assertEquals(Acceptance.HOSTILE, decide(lionfish));
        assertEquals(Acceptance.OK, InhabitantRules.decide(lionfish, new InhabitantRules.Settings(List.of(), List.of(), 0.8, false)),
                "refuseHostile = false lets it in");
        var bigGuardian = new InhabitantRules.Facts("minecraft:guardian", 0.85F, 0.85F, false, false, false, true, true);
        assertEquals(Acceptance.TOO_BIG, decide(bigGuardian), "size before hostile");
        var pleco = new InhabitantRules.Facts("fintastic:pleco", 1.0F, 0.5F, false, true, false, false, true);
        assertEquals(Acceptance.OK, decide(pleco), "the tag wins over the size");
        var tagged = new InhabitantRules.Facts("x:y", 0.3F, 0.3F, true, true, false, false, true);
        assertEquals(Acceptance.BLACKLISTED, decide(tagged), "the blacklist tag wins over the whitelist tag");
        var butterfly = new InhabitantRules.Facts("naturalist:butterfly", 0.7F, 0.6F, false, false, false, false, false);
        assertEquals(Acceptance.NOT_AQUATIC, decide(butterfly));
        var unknown = new InhabitantRules.Facts("naturalist:butterfly", 0.7F, 0.6F, false, false, false, false, null);
        assertEquals(Acceptance.OK, decide(unknown), "without an instance the water check waits");
    }

    @Test
    void configListsWinOverEverything() {
        var s = new InhabitantRules.Settings(List.of("naturalist:*", " Minecraft:Dolphin "), List.of("naturalist:piranha"), 0.8, true);
        assertEquals(Acceptance.OK, InhabitantRules.decide(new InhabitantRules.Facts("naturalist:whale", 3F, 2.5F, true, false, true, false, true), s));
        assertEquals(Acceptance.OK, InhabitantRules.decide(fish("minecraft:dolphin", 0.9F, 0.6F), s), "case and spaces do not matter");
        assertEquals(Acceptance.BLACKLISTED, InhabitantRules.decide(fish("naturalist:piranha", 0.5F, 0.6F), s), "deny wins over allow");
        var big = new InhabitantRules.Settings(List.of(), List.of(), 1.5, true);
        assertEquals(Acceptance.OK, InhabitantRules.decide(fish("minecraft:turtle", 1.2F, 0.4F), big), "maxHitbox 1.5");
    }

    @Test
    void patterns() {
        assertTrue(InhabitantRules.matches(List.of("cod"), "minecraft:cod"), "no namespace = minecraft");
        assertTrue(InhabitantRules.matches(List.of("*"), "anything:at_all"));
        assertFalse(InhabitantRules.matches(List.of("fintastic:*"), "fishofthieves:pondie"));
        assertFalse(InhabitantRules.matches(List.of("minecraft:co"), "minecraft:cod"), "no prefix matches");
        assertTrue(InhabitantRules.validPattern("modid:name") && InhabitantRules.validPattern("modid:*") && InhabitantRules.validPattern("*")
                && InhabitantRules.validPattern("cod"));
        assertFalse(InhabitantRules.validPattern("Not An Id") || InhabitantRules.validPattern(3) || InhabitantRules.validPattern("a:b:c"));
    }
}
