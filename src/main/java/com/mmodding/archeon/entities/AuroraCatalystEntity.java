package com.mmodding.archeon.entities;

import com.mmodding.archeon.init.ArcheonEntities;
import com.mmodding.archeon.init.ArcheonSoundEvents;
import com.mmodding.mmodding_lib.library.MModdingDamageSources;
import com.mmodding.mmodding_lib.library.entities.goals.FlyingAroundFarGoal;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class AuroraCatalystEntity extends HostileEntity {

	private static final TrackedData<Optional<UUID>> MASTER = DataTracker.registerData(AuroraCatalystEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);

	private final Type type;

	public AuroraCatalystEntity(EntityType<? extends AuroraCatalystEntity> entityType, World world) {
		super(entityType, world);
		this.moveControl = new FlightMoveControl(this, 10, false);
		this.type = entityType == ArcheonEntities.AURORA_CATALYST ? Type.NORMAL : (entityType == ArcheonEntities.POISONOUS_AURORA_CATALYST ? Type.POISONOUS : Type.EXPLOSIVE);
	}

	public void onInvokedByMaster(UUID master) {
		this.dataTracker.set(AuroraCatalystEntity.MASTER, Optional.of(master));
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(0, new MeleeAttackGoal(this, 2.0f, false));
		this.goalSelector.add(1, new AuroraCatalystFlyingGoal(this, 1.0));
		this.targetSelector.add(0, new TargetGoal<>(this, PlayerEntity.class, true));
	}

	@Override
	protected void initDataTracker() {
		super.initDataTracker();
		this.dataTracker.startTracking(AuroraCatalystEntity.MASTER, Optional.empty());
	}

	public boolean hasMaster() {
		return this.dataTracker.get(AuroraCatalystEntity.MASTER).isPresent();
	}

	public HeartOfNatureEntity getMaster(ServerWorld world) {
		return (HeartOfNatureEntity) world.getEntity(this.dataTracker.get(AuroraCatalystEntity.MASTER).orElseThrow());
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		if (this.hasMaster()) {
			nbt.putUuid("Master", this.dataTracker.get(AuroraCatalystEntity.MASTER).orElseThrow());
		}
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.contains("Master")) {
			this.onInvokedByMaster(nbt.getUuid("Master"));
		}
	}

	public @Nullable Box getAreaIfHavingMaster(ServerWorld world) {
		if (this.hasMaster()) {
			HeartOfNatureEntity heartOfNature = this.getMaster(world);
			if (heartOfNature == null) { // if the master was discarded
				this.dataTracker.set(AuroraCatalystEntity.MASTER, Optional.empty());
				return null;
			}
			return new Box(
				heartOfNature.getOriginalPos().add(8, 6, 8),
				heartOfNature.getOriginalPos().add(-8, -3, -8)
			);
		}
		return null;
	}

	@Override
	public void tickMovement() {
		super.tickMovement();

		if (!this.isOnGround() && this.getVelocity().y < 0.0) {
			this.setVelocity(this.getVelocity().multiply(1.0, 0.6, 1.0));
		}

		if (this.hasMaster()) {
			this.world.addParticle(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 1.0f, this.getZ(), 0.0f, 0.0f, 0.0f);
		}

		DefaultParticleType particleType = switch (this.type) {
			case NORMAL -> ParticleTypes.ENCHANT;
			case POISONOUS -> ParticleTypes.FALLING_SPORE_BLOSSOM;
			case EXPLOSIVE -> this.random.nextFloat() > 0.01f ? ParticleTypes.SMALL_FLAME : ParticleTypes.LAVA;
		};

		float velocityMultiplier = !this.type.equals(Type.EXPLOSIVE) ? 0.5f : 0.1f;

		for (int i = 0; i < 2; i++) {
			this.world.addParticle(
				particleType,
				this.getX() + this.random.nextFloat() - 0.5f,
				this.getY() + this.random.nextFloat() - 0.0f,
				this.getZ() + this.random.nextFloat() - 0.5f,
				(this.random.nextFloat() - 0.5f) * velocityMultiplier,
				(this.random.nextFloat() - 0.5f) * velocityMultiplier,
				(this.random.nextFloat() - 0.5f) * velocityMultiplier
			);
		}
	}

	@Override
	public void setTarget(@Nullable LivingEntity target) {
		if (target instanceof PlayerEntity) {
			super.setTarget(target);
		}
	}

	public static DefaultAttributeContainer.Builder createAuroraCatalystAttributes() {
		return MobEntity.createMobAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 9.0f)
			.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0f)
			.add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.1f)
			.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.5f)
			.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3f);
	}

	@Override
	protected EntityNavigation createNavigation(World world) {
		BirdNavigation birdNavigation = new BirdNavigation(this, world);
		birdNavigation.setCanPathThroughDoors(true);
		birdNavigation.setCanSwim(true);
		birdNavigation.setCanEnterOpenDoors(true);
		return birdNavigation;
	}

	@Override
	public boolean tryAttack(Entity target) {
		if (super.tryAttack(target)) {

			if (target instanceof LivingEntity entity) {

				Difficulty difficulty = entity.world.getDifficulty();

				int i = switch (difficulty) {
					case PEACEFUL -> 0;
					case EASY -> 1;
					case NORMAL -> 2;
					case HARD -> 3;
				};

				if (i != 0) {

					switch (this.type) {
						case EXPLOSIVE -> {
							Explosion explosion = new Explosion(this.world, this, MModdingDamageSources.PUSH, null, this.getPos().x, this.getPos().y, this.getPos().z, i, true, Explosion.DestructionType.NONE);
							explosion.collectBlocksAndDamageEntities();
							explosion.affectWorld(true);
							entity.setOnFireFor(3);
						}
						case POISONOUS -> {
							int amplifier = i - 1;
							entity.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 60, amplifier), this);
						}
					}

					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void remove(RemovalReason reason) {
		super.remove(reason);
		if (this.hasMaster() && this.getWorld() instanceof ServerWorld serverWorld) {
			Entity entity = this.getMaster(serverWorld);
			if (entity instanceof HeartOfNatureEntity heartOfNatureEntity) {
				heartOfNatureEntity.removeSoldier(this.getUuid());
				if (heartOfNatureEntity.getSoldiers().isEmpty()) {
					heartOfNatureEntity.openProtections();
				}
			}
		}
	}

	@Override
	public boolean isImmuneToExplosion() {
		return true;
	}

	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
		return false;
	}

	@Override
	public void fall(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition) {
	}

	@Nullable
	@Override
	protected SoundEvent getAmbientSound() {
		return ArcheonSoundEvents.ENTITY_AURORA_CATALYST_AMBIENT;
	}

	@Nullable
	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ArcheonSoundEvents.ENTITY_AURORA_CATALYST_HIT;
	}

	@Nullable
	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK;
	}

	public static class AuroraCatalystFlyingGoal extends FlyingAroundFarGoal {

		public AuroraCatalystFlyingGoal(AuroraCatalystEntity auroraCatalyst, double speed) {
			super(auroraCatalyst, speed);
		}

		@Override
		@Nullable
		protected Vec3d getWanderTarget() {
			Vec3d pos = super.getWanderTarget();
			if (pos == null || !(this.mob.getWorld() instanceof ServerWorld)) {
				return null;
			}
			Box box = ((AuroraCatalystEntity) this.mob).getAreaIfHavingMaster((ServerWorld) this.mob.getWorld());
			if (box != null) {
				boolean xCheck = pos.x <= box.minX || pos.x >= box.maxX;
				boolean yCheck = pos.y <= box.minY || pos.y >= box.maxY;
				boolean zCheck = pos.z <= box.minZ || pos.z >= box.maxZ;
				if (xCheck || yCheck || zCheck) {
					return ((AuroraCatalystEntity) this.mob).getMaster((ServerWorld) this.mob.getWorld()).getOriginalPos();
				}
			}
			return pos;
		}
	}

	public enum Type {
		NORMAL,
		POISONOUS,
		EXPLOSIVE
	}
}
