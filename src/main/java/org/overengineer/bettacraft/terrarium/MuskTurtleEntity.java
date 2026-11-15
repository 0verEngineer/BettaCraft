package org.overengineer.bettacraft.terrarium;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.food.BettaFood;
import org.overengineer.bettacraft.registry.ModItems;

/**
 * M31: common musk turtle (<i>Sternotherus odoratus</i>, the "stinkpot"), about 10 cm: a small turtle that walks on the
 * bottom of the water more than it swims and only now and then climbs onto land or a root to bask. It lives in a
 * <b>paludarium</b> with a small land part (5-60 % land), in water of 20-28 °C and air of 18-32 °C; it needs no UV lamp
 * (it basks seldom, a basking spot is welcome). Eats pellets, frozen and live food. Caught in a bucket of water like a
 * fish. The first species of the terrarium care hook ({@link TerrariumNeeds}); breeding and the other turtles follow in
 * part b.
 */
public class MuskTurtleEntity extends TerrariumAnimalEntity implements Bucketable {
    public static final TerrariumNeeds NEEDS = TerrariumNeeds.builder(TerrariumNeeds.Habitat.PALUDARIUM)
            .air(18, 32).water(20, 28).land(0.05, 0.6).build();
    private static final LandWalker.Gait GAIT = new LandWalker.Gait(0.02, 0.03, true, 0.25, 0.6, 0.08, 0.2, 0.22);
    private boolean fromBucket;

    public MuskTurtleEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.12);
    }

    @Override
    public TerrariumNeeds terrariumNeeds() {
        return NEEDS;
    }

    @Override
    public LandWalker.Gait gait() {
        return GAIT;
    }

    @Override
    public boolean eatsFood(BettaFood.Kind kind) {
        return kind == BettaFood.Kind.NORMAL || kind == BettaFood.Kind.LIVE;
    }

    /** A musk turtle eats and dirties like a few bettas. */
    @Override
    public double stockWeight() {
        return 3.0;
    }

    @Override
    public double starveDays() {
        return 7.0;
    }

    // ------------------------------------------------------------------ bucket

    @Override
    public boolean fromBucket() {
        return fromBucket;
    }

    @Override
    public void setFromBucket(boolean b) {
        fromBucket = b;
    }

    @Override
    public void saveToBucketTag(ItemStack stack) {
        Bucketable.saveDefaultDataToBucketTag(this, stack);
        CompoundTag t = saveTerrarium();
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, stack, c -> c.put("terrarium", t));
    }

    @Override
    public void loadFromBucketTag(CompoundTag tag) {
        Bucketable.loadDefaultDataFromBucketTag(this, tag);
        wild = false;
        if (tag.contains("terrarium")) loadTerrarium(tag.getCompound("terrarium"));
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(ModItems.MUSK_TURTLE_BUCKET.get());
    }

    @Override
    public SoundEvent getPickupSound() {
        return SoundEvents.BUCKET_FILL_FISH;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return Bucketable.bucketMobPickup(player, hand, this).orElse(super.mobInteract(player, hand));
    }

    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || fromBucket;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("FromBucket", fromBucket);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        fromBucket = tag.getBoolean("FromBucket");
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.TURTLE_HURT_BABY;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.TURTLE_DEATH_BABY;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }
}
