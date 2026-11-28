package org.overengineer.bettacraft.datagen;

import java.util.function.BiConsumer;

/**
 * M69 English texts (several aquarists per player: the friend the first one recommends, the tabs Team and Friend, one
 * aquarist per tank). The German ones are in de_de.json; edit both.
 */
final class LangM69 {
    private LangM69() {}

    static void addAll(BiConsumer<String, String> a) {
        // config
        a.accept("bettacraft.configuration.maxAquarists", "Aquarists per player");
        a.accept("bettacraft.configuration.maxAquarists.tooltip", "Aquarists one player can have at most: the first comes by himself, every further one is a friend the first recommends in his menu (1: only one, as before)");
        a.accept("bettacraft.configuration.friendAfterDays", "Days before a friend");
        a.accept("bettacraft.configuration.friendAfterDays.tooltip", "In-game days the aquarist who recommends a friend must have worked without a break (his wage paid every morning), and the days since the last hiring, before he offers the next friend");
        a.accept("bettacraft.configuration.tanksPerAquarist", "Tanks per aquarist");
        a.accept("bettacraft.configuration.tanksPerAquarist.tooltip", "Own tanks per aquarist for a friend (enough work): the second from 2 x this, the third from 3 x this");
        a.accept("bettacraft.configuration.friendCalmDays", "Days without trouble");
        a.accept("bettacraft.configuration.friendCalmDays.tooltip", "In-game days without trouble (no unpaid day, no tank in an aquarist's care with bad water) before a friend is recommended (0: no such condition)");
        a.accept("bettacraft.configuration.friendHomeGraceDays", "Days a friend waits for a home");
        a.accept("bettacraft.configuration.friendHomeGraceDays.tooltip", "In-game days a hired friend waits next to the one who recommended him for a free bed and breeding basin before days without a home count (he does not work while he waits)");
        // advancement and quest
        a.accept("advancements.bettacraft.aquarist_team.title", "A Friend Joins");
        a.accept("advancements.bettacraft.aquarist_team.description", "Hire the friend your aquarist recommends");
        a.accept("quest.bettacraft.aquarist_team.title", "A Friend Joins");
        a.accept("quest.bettacraft.aquarist_team.desc", "Keep your aquarist busy and happy: after a week of work with his wage paid every morning, a few more tanks (three per aquarist) and another breeding, without unpaid days or tanks with bad water, he recommends a friend in his menu (tab Friend) and in the chat. Hire him: he brings his own bed, breeding basin and chests, and every tank goes to the aquarist whose basin is nearest.");
        // the chat
        a.accept("message.bettacraft.aquarist.friend.offer", "My friend %s is looking for work and could work for you too - talk to me.");
        a.accept("message.bettacraft.aquarist.friend.talk_button", "[Talk to him]");
        a.accept("message.bettacraft.aquarist.friend.talk_hover", "Opens his menu at the tab Friend (stand next to him)");
        a.accept("message.bettacraft.aquarist.friend.none", "No friend is on offer.");
        a.accept("message.bettacraft.aquarist.friend.full", "The team is full (%s/%s).");
        a.accept("message.bettacraft.aquarist.friend.hired_home", "Thank you! %s told me a lot about you. I found a free bed and breeding basin and start right away.");
        a.accept("message.bettacraft.aquarist.friend.hired_wait", "Thank you! %s told me a lot about you. I still need a free bed and a breeding basin near here; until then I wait with him.");
        a.accept("message.bettacraft.aquarist.friend.declined", "Pity. I will ask him again in a few days.");
        a.accept("message.bettacraft.aquarist.team.too_far", "This tank is too far from the workplace of %s.");
        a.accept("message.bettacraft.aquarist.team.status_line", "%s: %s, %s tanks, workplace %s");
        a.accept("message.bettacraft.aquarist.team.friend_offered", "A friend is on offer: %s (tab Friend in the menu of the one who recommends him).");
        a.accept("message.bettacraft.aquarist.team.op_only", "Only operators can make the friend come at once.");
        a.accept("message.bettacraft.aquarist.team.no_mediator", "None of your aquarists is hired yet, so nobody can recommend a friend.");
        a.accept("message.bettacraft.aquarist.team.go_to", "%s is at the workplace %s: talk to him there.");
        // the menu
        a.accept("gui.bettacraft.aquarist.tab.team", "Team");
        a.accept("gui.bettacraft.aquarist.tab.friend", "Friend");
        a.accept("gui.bettacraft.aquarist.status.waiting_home", "waits for a bed and a basin");
        a.accept("gui.bettacraft.aquarist.status.unloaded", "far away");
        a.accept("gui.bettacraft.aquarist.tank.other", "%s looks after it");
        a.accept("gui.bettacraft.aquarist.team.title", "Team: %s of %s aquarists");
        a.accept("gui.bettacraft.aquarist.team.cared", "%s tanks");
        a.accept("gui.bettacraft.aquarist.team.no_workplace", "no basin");
        a.accept("gui.bettacraft.aquarist.team.you_recommend", "★ This aquarist recommends friends: see the tab Friend.");
        a.accept("gui.bettacraft.aquarist.team.recommends", "★ %s recommends friends: the tab Friend is in his menu.");
        a.accept("gui.bettacraft.aquarist.team.feature_off", "Several aquarists are switched off (features.aquaristNpc.team).");
        a.accept("gui.bettacraft.aquarist.team.shared", "Every tank is looked after by one of them: the one with the nearest basin, or the one chosen on the tab Tanks.");
        a.accept("gui.bettacraft.aquarist.team.who.tooltip", "Who of the team looks after this tank. Click: the next one; after the last, back to the one with the nearest basin");
        a.accept("gui.bettacraft.aquarist.team.who_nobody", "Nobody of the team");
        a.accept("gui.bettacraft.aquarist.team.who_given", "By %s (chosen)");
        a.accept("gui.bettacraft.aquarist.team.who_auto", "By %s (nearest)");
        a.accept("gui.bettacraft.aquarist.friend.hire", "Hire him");
        a.accept("gui.bettacraft.aquarist.friend.decline", "No, thanks");
        a.accept("gui.bettacraft.aquarist.friend.full", "Team full (%s/%s)");
        a.accept("gui.bettacraft.aquarist.friend.full_text", "Every place in the team is taken (hiredAquarist.maxAquarists in the server config).");
        a.accept("gui.bettacraft.aquarist.friend.offer_text", "My friend %s is looking for work and would like to work for you too. He brings his own bed, breeding basin and chests and takes his own wage; every tank goes to the one of us whose basin is nearest.");
        a.accept("gui.bettacraft.aquarist.friend.home_text", "He needs a free bed and a free breeding basin near here. Until you have built them he waits next to me and does not work yet.");
        a.accept("gui.bettacraft.aquarist.friend.team_after", "The team after hiring him: %s/%s");
        a.accept("gui.bettacraft.aquarist.friend.intro", "A friend of mine could work for you too, as soon as:");
        a.accept("gui.bettacraft.aquarist.friend.cond.limit", "Room in the team: %s of %s aquarists");
        a.accept("gui.bettacraft.aquarist.friend.cond.days", "Worked without a break (wage every morning): %s of %s days");
        a.accept("gui.bettacraft.aquarist.friend.cond.tanks", "Enough work: %s of %s own tanks");
        a.accept("gui.bettacraft.aquarist.friend.cond.breeding", "Another breeding since the last hiring: %s of %s");
        a.accept("gui.bettacraft.aquarist.friend.cond.calm", "No trouble (unpaid days, bad water): %s of %s days");
        a.accept("gui.bettacraft.aquarist.friend.cond.wait", "After the last refusal: %s more days");
        a.accept("gui.bettacraft.aquarist.friend.in_days", "A friend in %s days at the earliest");
    }
}
