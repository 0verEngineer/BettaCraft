package org.overengineer.bettacraft.datagen;

import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import org.overengineer.bettacraft.containers.AnimalIcons;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M55: the item models of the containers with an animal's own sprite ({@link AnimalIcons},
 * {@code tools/assetgen/animal_icons.py}): the tankmate buckets, and per animal a model of the critter bucket, the fish bag
 * and the transport tub chosen by the item property {@code bettacraft:animal} (overrides in the order of the numbers: the
 * last one that matches wins).
 */
final class DataGenM55 {
    /** The fish bag with opaque water under an animal (the layers keep their order on 1.21.4+, see animal_icons.py). */
    static final String BAG = "item/animal/bag";

    private DataGenM55() {}

    /** A bucket of water with the sprite (layer 1) and the glass in front (layer 2). */
    static void bucket(ItemModelProvider p, String name, String sprite) {
        p.withExistingParent(name, p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/betta_bucket"))
                .texture("layer1", p.modLoc(sprite)).texture("layer2", p.modLoc("item/betta_bucket_glass"));
    }

    /** The critter bucket of each small animal: body (layer 1, tinted for the gene-coloured ones), details, glass. */
    static void critterBucketOverrides(ItemModelProvider p, ItemModelBuilder bucket) {
        for (CritterSpecies s : CritterSpecies.values()) {
            var m = p.withExistingParent("critter_bucket_" + s.id(), p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/betta_bucket"))
                    .texture("layer1", p.modLoc(AnimalIcons.body(s))).texture("layer2", p.modLoc(AnimalIcons.sprite(s)))
                    .texture("layer3", p.modLoc("item/betta_bucket_glass"));
            bucket.override().predicate(AnimalIcons.PROPERTY, AnimalIcons.threshold(AnimalIcons.index(s))).model(m).end();
        }
    }

    /** The fish bag of each small tankmate (its sprite) and each small animal (body and details). */
    static void bagOverrides(ItemModelProvider p, ItemModelBuilder bag) {
        for (TankmateSpecies s : TankmateSpecies.values()) {
            if (s.isLarge()) continue;
            var m = p.withExistingParent("fish_bag_" + s.id(), p.mcLoc("item/generated")).texture("layer0", p.modLoc(BAG))
                    .texture("layer1", p.modLoc(AnimalIcons.sprite(s)));
            bag.override().predicate(AnimalIcons.PROPERTY, AnimalIcons.threshold(AnimalIcons.index(s))).model(m).end();
        }
        for (CritterSpecies s : CritterSpecies.values()) {
            var m = p.withExistingParent("fish_bag_" + s.id(), p.mcLoc("item/generated")).texture("layer0", p.modLoc(BAG))
                    .texture("layer1", p.modLoc(AnimalIcons.body(s))).texture("layer2", p.modLoc(AnimalIcons.sprite(s)));
            bag.override().predicate(AnimalIcons.PROPERTY, AnimalIcons.threshold(AnimalIcons.index(s))).model(m).end();
        }
    }

    /** The transport tub of each big animal: the tub, the animal lying in it, the lower front wall over its belly. */
    static void tubOverrides(ItemModelProvider p, ItemModelBuilder tub) {
        for (TankmateSpecies s : TankmateSpecies.values()) {
            if (!s.isLarge()) continue;
            var m = p.withExistingParent("transport_tub_" + s.id(), p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/transport_tub"))
                    .texture("layer1", p.modLoc(AnimalIcons.sprite(s))).texture("layer2", p.modLoc("item/animal/tub_front"));
            tub.override().predicate(AnimalIcons.PROPERTY, AnimalIcons.threshold(AnimalIcons.index(s))).model(m).end();
        }
    }
}
