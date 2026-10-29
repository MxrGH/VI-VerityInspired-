package dev.mxr.verityinspired;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class VerityEntity extends Monster {
    private int stareTicks;
    private int teleportCooldown = 100;

    public VerityEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 70.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.31D)
            .add(Attributes.ATTACK_DAMAGE, 8.0D)
            .add(Attributes.FOLLOW_RANGE, 48.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.75D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.18D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) return;

        if (teleportCooldown > 0) teleportCooldown--;
        LivingEntity target = this.getTarget();

        if (target instanceof Player player && player.isAlive()) {
            double distance = this.distanceToSqr(player);
            if (distance < 14.0D * 14.0D) {
                stareTicks++;
                if (stareTicks % 80 == 0) {
                    serverLevel.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 0.9F, 0.65F);
                }
                if (distance > 8.0D * 8.0D && distance < 34.0D * 34.0D && teleportCooldown == 0 && this.random.nextInt(4) == 0) {
                    double angle = this.random.nextDouble() * Math.PI * 2.0D;
                    double x = player.getX() + Math.cos(angle) * (3.0D + this.random.nextDouble() * 3.0D);
                    double z = player.getZ() + Math.sin(angle) * (3.0D + this.random.nextDouble() * 3.0D);
                    if (this.randomTeleport(x, player.getY(), z, true)) {
                        serverLevel.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.9F, 0.7F);
                        teleportCooldown = 180;
                    }
                }
            } else {
                stareTicks = 0;
            }
        } else {
            stareTicks = 0;
        }
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.ENDERMAN_AMBIENT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return super.hurtServer(level, source, amount);
    }
}