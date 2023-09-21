package com.spellbladenext;

import com.google.common.collect.ImmutableMultimap;
import com.spellbladenext.effect.RunicAbsorption;
import com.spellbladenext.items.*;
import com.spellbladenext.items.armor.Armors;
import com.spellbladenext.items.attacks.AttackAll;
import com.spellbladenext.items.interfaces.PlayerDamageInterface;
import com.spellbladenext.items.loot.Default;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.item.v1.FabricItemStack;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.spell_engine.api.item.ItemConfig;
import net.spell_engine.api.item.trinket.SpellBooks;
import net.spell_engine.api.item.weapon.Weapon;
import net.spell_engine.api.loot.LootConfig;
import net.spell_engine.api.loot.LootHelper;
import net.spell_engine.api.render.CustomModels;
import net.spell_engine.api.spell.CustomSpellHandler;
import net.spell_engine.client.render.SpellProjectileRenderer;
import net.spell_engine.entity.SpellProjectile;
import net.spell_engine.internals.SpellCasterEntity;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.SpellRegistry;
import net.spell_engine.particle.ParticleHelper;
import net.spell_engine.utils.TargetHelper;
import net.spell_power.api.MagicSchool;
import net.spell_power.api.SpellDamageSource;
import net.spell_power.api.SpellPower;
import net.tinyconfig.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.GeoAnimatable;

import java.util.*;

import static net.spell_power.api.SpellPower.getCriticalChance;

public class Spellblades implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("spellbladenext");
	public static ItemGroup SPELLBLADES;
	public static String MOD_ID = "spellbladenext";

	public static RegistryKey<ItemGroup> KEY = RegistryKey.of(Registries.ITEM_GROUP.getKey(),new Identifier(Spellblades.MOD_ID,"generic"));
	public static Item spellOil = new SpellOil(new FabricItemSettings().maxCount(1));
	public static Item whirlwindOil = new WhirlwindOil(new FabricItemSettings().maxCount(1));
	public static Item smiteOil = new SmiteOil(new FabricItemSettings().maxCount(1));
	public static Item RUNEBLAZE = new Item(new FabricItemSettings().maxCount(64));
	public static Item RUNEFROST = new Item(new FabricItemSettings().maxCount(64));
	public static Item RUNEGLEAM = new Item(new FabricItemSettings().maxCount(64));
	public static StatusEffect RunicAbsorption = new RunicAbsorption(StatusEffectCategory.BENEFICIAL, 0xff4bdd);
	public static ConfigManager<ItemConfig> itemConfig = new ConfigManager<ItemConfig>
			("items_v4", Default.itemConfig)
			.builder()
			.setDirectory(MOD_ID)
			.sanitize(true)
			.build();
	public static ConfigManager<LootConfig> lootConfig = new ConfigManager<LootConfig>
			("loot_v4", Default.lootConfig)
			.builder()
			.setDirectory(MOD_ID)
			.sanitize(true)
			.constrain(LootConfig::constrainValues)
			.build();
	@Override
	public void onInitialize() {
		SPELLBLADES = FabricItemGroup.builder()
				.icon(() -> new ItemStack(Items.arcane_blade.item()))
				.displayName(Text.translatable("itemGroup.spellbladenext.general"))
				.build();
		Registry.register(Registries.ITEM,new Identifier(MOD_ID,"spelloil"),spellOil);
		Registry.register(Registries.ITEM,new Identifier(MOD_ID,"whirlwindoil"),whirlwindOil);
		Registry.register(Registries.ITEM,new Identifier(MOD_ID,"smiteoil"),smiteOil);
		Registry.register(Registries.ITEM,new Identifier(MOD_ID,"runeblaze_ingot"),RUNEBLAZE);
		Registry.register(Registries.ITEM,new Identifier(MOD_ID,"runefrost_ingot"),RUNEFROST);
		Registry.register(Registries.ITEM,new Identifier(MOD_ID,"runegleam_ingot"),RUNEGLEAM);
		Registry.register(Registries.STATUS_EFFECT,new Identifier(MOD_ID,"runicabsorption"),RunicAbsorption);

		Items.register(itemConfig.value.weapons);
		Armors.register(itemConfig.value.armor_sets);
		lootConfig.refresh();
		itemConfig.refresh();
		CustomModels.registerModelIds(List.of(
				new Identifier(MOD_ID, "projectile/flamewaveprojectile")
		));
		CustomModels.registerModelIds(List.of(
				new Identifier(MOD_ID, "projectile/amethyst")
		));
		CustomModels.registerModelIds(List.of(
				new Identifier(MOD_ID, "projectile/gladius")
		));
		Registry.register(Registries.ITEM_GROUP, KEY, SPELLBLADES);
		ItemGroupEvents.modifyEntriesEvent(KEY).register((content) -> {
			content.add(spellOil);
			content.add(whirlwindOil);
			content.add(smiteOil);
			content.add(RUNEBLAZE);
			content.add(RUNEGLEAM);
			content.add(RUNEFROST);

		});
		SpellBooks.createAndRegister(new Identifier(MOD_ID,"frost_battlemage"),KEY);
		SpellBooks.createAndRegister(new Identifier(MOD_ID,"fire_battlemage"),KEY);
		SpellBooks.createAndRegister(new Identifier(MOD_ID,"arcane_battlemage"),KEY);

		CustomSpellHandler.register(new Identifier(MOD_ID,"whirlingblades"),(data) -> {
			MagicSchool actualSchool = MagicSchool.FIRE;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"frostvert")).impact[0].action.damage.spell_power_coefficient;
			data1.caster().velocityDirty = true;
			data1.caster().velocityModified = true;
				float f = data1.caster().getYaw();
				float g = data1.caster().getPitch();
				float h = -MathHelper.sin(f * 0.017453292F) * MathHelper.cos(g * 0.017453292F);
				float k = -MathHelper.sin(g * 0.017453292F);
				float l = MathHelper.cos(f * 0.017453292F) * MathHelper.cos(g * 0.017453292F);
				float m = MathHelper.sqrt(h * h + k * k + l * l);
				float n = 3.0F * ((1.0F + (float)3) / 4.0F);
				h *= n / m;
				k *= n / m;
				l *= n / m;
			data1.caster().addVelocity((double)h, (double)k, (double)l);
			data1.caster().useRiptide(20);
				if (data1.caster().isOnGround()) {
					float o = 1.1999999F;
					data1.caster().move(MovementType.SELF, new Vec3d(0.0D, 1.1999999284744263D, 0.0D));
				}

				SoundEvent soundEvent;
					soundEvent = SoundEvents.ITEM_TRIDENT_RIPTIDE_3;


			data1.caster().getWorld().playSoundFromEntity((PlayerEntity)null, data1.caster(), soundEvent, SoundCategory.PLAYERS, 1.0F, 1.0F);

			return true;
	});
		CustomSpellHandler.register(new Identifier(MOD_ID,"frostvert"),(data) -> {
			MagicSchool actualSchool = MagicSchool.FIRE;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"frostvert")).impact[0].action.damage.spell_power_coefficient;
			data1.caster().velocityDirty = true;
			data1.caster().velocityModified = true;

			data1.caster().addVelocity(0,1,0);
			AttackAll.attackAll(data1.caster(),data1.targets(),(float)modifier);
			for(Entity entity: data1.targets()){
				SpellPower.Result power = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
				SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
				if(entity instanceof LivingEntity living) {
					vulnerability = SpellPower.getVulnerability(living, actualSchool);
				}
				double amount = modifier *  power.randomValue(vulnerability);
				entity.timeUntilRegen = 0;

				entity.damage(SpellDamageSource.player(actualSchool,data1.caster()), (float) amount);
				ParticleHelper.sendBatches(entity,SpellRegistry.getSpell(new Identifier(MOD_ID,"frostvert")).impact[0].particles);

			}
			return true;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"finalstrike"),(data) -> {
			MagicSchool actualSchool = MagicSchool.ARCANE;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"finalstrike")).impact[0].action.damage.spell_power_coefficient;
			List<Entity> list = TargetHelper.targetsFromRaycast(data1.caster(),SpellRegistry.getSpell(new Identifier(MOD_ID,"finalstrike")).range, Objects::nonNull);
			System.out.println(list);
			if(!data1.targets().isEmpty()) {
				AttackAll.attackAll(data1.caster(), data1.targets(), (float) modifier);
				for (Entity entity : data1.targets()) {
					SpellPower.Result power = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
					SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
					if (entity instanceof LivingEntity living) {
						vulnerability = SpellPower.getVulnerability(living, actualSchool);
					}
					double amount = modifier * power.randomValue(vulnerability);
					entity.timeUntilRegen = 0;

					entity.damage(SpellDamageSource.player(actualSchool, data1.caster()), (float) amount);
					ParticleHelper.sendBatches(entity,SpellRegistry.getSpell(new Identifier(MOD_ID,"finalstrike")).impact[0].particles);

					if(entity instanceof LivingEntity living){
						Vec3d vec3 = entity.getPos().add(data1.caster().getRotationVec(1F).subtract(0,data1.caster().getRotationVec(1F).getY(),0).normalize().multiply(1+0.5+(entity.getBoundingBox().getXLength() / 2)));
						data1.caster().requestTeleport(vec3.getX(),vec3.getY(),vec3.getZ());
					}
				}
			}
			else {
				BlockHitResult result = data1.caster().getWorld().raycast(new RaycastContext(data1.caster().getEyePos(),data1.caster().getEyePos().add(data1.caster().getRotationVector().multiply(SpellRegistry.getSpell(new Identifier(MOD_ID,"finalstrike")).range)), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE,data1.caster()));
				if (!list.isEmpty()) {
					AttackAll.attackAll(data1.caster(), list, (float) modifier);
					for (Entity entity : list) {
						SpellPower.Result power = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
						SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
						if (entity instanceof LivingEntity living) {
							vulnerability = SpellPower.getVulnerability(living, actualSchool);
						}
						double amount = modifier * power.randomValue(vulnerability);
						entity.timeUntilRegen = 0;

						entity.damage(SpellDamageSource.player(actualSchool, data1.caster()), (float) amount);
						ParticleHelper.sendBatches(entity,SpellRegistry.getSpell(new Identifier(MOD_ID,"finalstrike")).impact[0].particles);
					}
				}
				if(result.getPos() != null) {
					data1.caster().requestTeleport(result.getPos().getX(),result.getPos().getY(),result.getPos().getZ());
				}
			}
			return true;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"frostblink"),(data) -> {
			MagicSchool actualSchool = MagicSchool.ARCANE;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"frostblink")).impact[0].action.damage.spell_power_coefficient;
			List<Entity> list = TargetHelper.targetsFromRaycast(data1.caster(),SpellRegistry.getSpell(new Identifier(MOD_ID,"frostblink")).range, Objects::nonNull);
			System.out.println(list);
			if(!data1.targets().isEmpty()) {
				AttackAll.attackAll(data1.caster(), data1.targets(), (float) modifier);
				for (Entity entity : data1.targets()) {
					SpellPower.Result power = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
					SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
					if (entity instanceof LivingEntity living) {
						vulnerability = SpellPower.getVulnerability(living, actualSchool);
					}
					double amount = modifier * power.randomValue(vulnerability);
					entity.timeUntilRegen = 0;

					entity.damage(SpellDamageSource.player(actualSchool, data1.caster()), (float) amount);
					ParticleHelper.sendBatches(entity,SpellRegistry.getSpell(new Identifier(MOD_ID,"frostblink")).impact[0].particles);

					if(entity instanceof LivingEntity living){
						Vec3d vec3 = entity.getPos().add(data1.caster().getRotationVec(1F).subtract(0,data1.caster().getRotationVec(1F).getY(),0).normalize().multiply(1+0.5+(entity.getBoundingBox().getXLength() / 2)));
						data1.caster().requestTeleport(vec3.getX(),vec3.getY(),vec3.getZ());
					}
				}
			}
			else {
				BlockHitResult result = data1.caster().getWorld().raycast(new RaycastContext(data1.caster().getEyePos(),data1.caster().getEyePos().add(data1.caster().getRotationVector().multiply(SpellRegistry.getSpell(new Identifier(MOD_ID,"frostblink")).range)), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE,data1.caster()));
				if (!list.isEmpty()) {
					AttackAll.attackAll(data1.caster(), list, (float) modifier);
					for (Entity entity : list) {
						SpellPower.Result power = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
						SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
						if (entity instanceof LivingEntity living) {
							vulnerability = SpellPower.getVulnerability(living, actualSchool);
						}
						double amount = modifier * power.randomValue(vulnerability);
						entity.timeUntilRegen = 0;

						entity.damage(SpellDamageSource.player(actualSchool, data1.caster()), (float) amount);
						ParticleHelper.sendBatches(entity,SpellRegistry.getSpell(new Identifier(MOD_ID,"frostblink")).impact[0].particles);
					}
				}
				if(result.getPos() != null) {
					data1.caster().requestTeleport(result.getPos().getX(),result.getPos().getY(),result.getPos().getZ());
				}
			}
			return true;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"flicker_strike"),(data) -> {

			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"flicker_strike")).impact[0].action.damage.spell_power_coefficient;
			modifier *= 0.2;
			modifier *= data1.caster().getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED);
			if(data1.caster() instanceof PlayerDamageInterface player) {
				List<LivingEntity> list = new ArrayList<>();
				for(Entity entity: data1.targets()){
					if(entity instanceof LivingEntity living && (!player.getList().contains(living) || (data1.targets().size() == 1 && data1.targets().contains(data1.caster().getAttacking())))){
						list.add(living);
					}
				}
				if(list.isEmpty()){
					player.listRefresh();
					return false;

				}
				LivingEntity closest = data1.caster().getWorld().getClosestEntity(list,TargetPredicate.DEFAULT, data1.caster(),data1.caster().getX(),data1.caster().getY(),data1.caster().getZ());

				if(closest!= null) {
					data1.caster().requestTeleport(closest.getX()-((closest.getWidth()+1)*data1.caster().getRotationVec(1.0F).subtract(0,data1.caster().getRotationVec(1.0F).getY(),0).normalize().getX()),closest.getY(),closest.getZ()-((closest.getWidth()+1)*data1.caster().getRotationVec(1.0F).subtract(0,data1.caster().getRotationVec(1.0F).getY(),0).normalize().getZ()));

					AttackAll.attackAll(data1.caster(), List.of(closest), modifier);
					SpellPower.Result power = SpellPower.getSpellPower(MagicSchool.FIRE, (LivingEntity) data1.caster());
					SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
						vulnerability = SpellPower.getVulnerability(closest, MagicSchool.FIRE);

					double amount = modifier * power.randomValue(vulnerability);
					closest.timeUntilRegen = 0;

					closest.damage(SpellDamageSource.player(MagicSchool.FIRE, data1.caster()), (float) amount);

					player.listAdd(closest);
					return false;
				}
				else{
					player.listRefresh();
					return true;
				}

			}
			return false;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"eviscerate"),(data) -> {
			MagicSchool actualSchool = MagicSchool.FROST;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			data1.targets().remove(data1.caster());
			if(data1.targets().isEmpty()){
				if(data1.caster() instanceof SpellCasterEntity entity){
					entity.clearCasting();
				}
				return true;
			}
			if(data1.caster() instanceof PlayerDamageInterface playerDamageInterface && playerDamageInterface.getLastAttacked() != null && playerDamageInterface.getLastAttacked() instanceof LivingEntity living && living.isDead()){
				playerDamageInterface.resetRepeats();
				playerDamageInterface.setLastAttacked(null);
			}
			if(data1.caster() instanceof PlayerDamageInterface playerDamageInterface && playerDamageInterface.getRepeats() >= 4){
				playerDamageInterface.resetRepeats();
				playerDamageInterface.setLastAttacked(null);

				if(data1.caster() instanceof SpellCasterEntity entity){
					entity.clearCasting();
				}
				return true;
			}
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"eviscerate")).impact[0].action.damage.spell_power_coefficient;
			modifier *= 0.2;
			modifier *= data1.caster().getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED);

			if(data1.caster() instanceof PlayerDamageInterface playerDamageInterface && playerDamageInterface.getLastAttacked() != null && data1.targets().contains(playerDamageInterface.getLastAttacked())) {
				EntityAttributeModifier modifier1 = new EntityAttributeModifier(UUID.randomUUID(),"knockbackresist",1, EntityAttributeModifier.Operation.ADDITION);
				ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
				builder.put(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, modifier1);

				((LivingEntity)playerDamageInterface.getLastAttacked()).getAttributes().addTemporaryModifiers(builder.build());

				AttackAll.attackAll(data1.caster(), List.of(playerDamageInterface.getLastAttacked()), (float) modifier);
				playerDamageInterface.repeat();
				SpellPower.Result power = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
				SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
				if (playerDamageInterface.getLastAttacked() instanceof LivingEntity living) {
					vulnerability = SpellPower.getVulnerability(living, actualSchool);
				}
				double amount = modifier * power.randomValue(vulnerability);
				playerDamageInterface.getLastAttacked().timeUntilRegen = 0;

				playerDamageInterface.getLastAttacked().damage(SpellDamageSource.player(actualSchool, data1.caster()), (float) amount);
				if(playerDamageInterface.getLastAttacked() instanceof LivingEntity living)
					living.getAttributes().removeModifiers(builder.build());
				ParticleHelper.sendBatches(playerDamageInterface.getLastAttacked(),SpellRegistry.getSpell(new Identifier(MOD_ID,"eviscerate")).impact[0].particles);

				return false;
			}
			if(data1.caster() instanceof PlayerDamageInterface playerDamageInterface && !data1.targets().isEmpty()) {
				Entity entity = playerDamageInterface.getLastAttacked();
				List<LivingEntity> list = new ArrayList<>();
				for(Entity entity1 : data1.targets()){
					if(entity1 instanceof LivingEntity living){
						list.add(living);
					}
				}
				if(entity == null || !data1.targets().contains(entity)) {
					entity = data1.caster().getWorld().getClosestEntity(list, TargetPredicate.DEFAULT,data1.caster(),data1.caster().getX(),data1.caster().getY(),data1.caster().getZ());
				}
				else{
					playerDamageInterface.setLastAttacked(null);
					playerDamageInterface.resetRepeats();
					if(data1.caster() instanceof SpellCasterEntity antity){
						antity.clearCasting();
					}
					return true;
				}

				if(entity != null) {
					EntityAttributeModifier modifier1 = new EntityAttributeModifier(UUID.randomUUID(),"knockbackresist",1, EntityAttributeModifier.Operation.ADDITION);
					ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
					builder.put(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, modifier1);

					((LivingEntity)entity).getAttributes().addTemporaryModifiers(builder.build());

					AttackAll.attackAll(data1.caster(), List.of(entity), (float) modifier);
					playerDamageInterface.setLastAttacked(entity);
					SpellPower.Result power = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
					SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
					if (entity instanceof LivingEntity living) {
						vulnerability = SpellPower.getVulnerability(living, actualSchool);
					}
					double amount = modifier * power.randomValue(vulnerability);
					entity.timeUntilRegen = 0;

					entity.damage(SpellDamageSource.player(actualSchool, data1.caster()), (float) amount);
					if(entity instanceof LivingEntity living)
						living.getAttributes().removeModifiers(builder.build());
					ParticleHelper.sendBatches(entity,SpellRegistry.getSpell(new Identifier(MOD_ID,"eviscerate")).impact[0].particles);


				}
			}
			return false;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"frostflourish"),(data) -> {
			MagicSchool actualSchool = MagicSchool.FROST;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"frostflourish")).impact[0].action.damage.spell_power_coefficient;

			AttackAll.attackAll(data1.caster(),data1.targets(),(float)modifier);
			for(Entity entity: data1.targets()){
				SpellPower.Result power = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
				SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
				if(entity instanceof LivingEntity living) {
					vulnerability = SpellPower.getVulnerability(living, actualSchool);
				}
				double amount = modifier *  power.randomValue(vulnerability);
				entity.timeUntilRegen = 0;

				entity.damage(SpellDamageSource.player(actualSchool,data1.caster()), (float) amount);
			}
			return true;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"fireflourish"),(data) -> {
			MagicSchool actualSchool = MagicSchool.FIRE;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"fireflourish")).impact[0].action.damage.spell_power_coefficient;

			AttackAll.attackAll(data1.caster(),data1.targets(),(float)modifier);
			for(Entity entity: data1.targets()){
				SpellPower.Result power = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
				SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
				if(entity instanceof LivingEntity living) {
					vulnerability = SpellPower.getVulnerability(living, actualSchool);
				}
				double amount = modifier *  power.randomValue(vulnerability);
				entity.timeUntilRegen = 0;

				entity.damage(SpellDamageSource.player(actualSchool,data1.caster()), (float) amount);
			}
			return true;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"arcaneflourish"),(data) -> {
			MagicSchool actualSchool = MagicSchool.ARCANE;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"arcaneflourish")).impact[0].action.damage.spell_power_coefficient;

			AttackAll.attackAll(data1.caster(),data1.targets(),(float)modifier);
			for(Entity entity: data1.targets()){
				SpellPower.Result power = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
				SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
				if(entity instanceof LivingEntity living) {
					vulnerability = SpellPower.getVulnerability(living, actualSchool);
				}
				double amount = modifier *  power.randomValue(vulnerability);
				entity.timeUntilRegen = 0;

				entity.damage(SpellDamageSource.player(actualSchool,data1.caster()), (float) amount);
			}
			return true;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"tempest"),(data) -> {
			MagicSchool actualSchool = MagicSchool.FROST;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			double modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"tempest")).impact[0].action.damage.spell_power_coefficient;
			 modifier *= 0.4F+0.6F/(float)data1.targets().size()+(0.6F-0.6F/(float)data1.targets().size())*Math.min(3, EnchantmentHelper.getEquipmentLevel(Enchantments.SWEEPING,data1.caster()))/3;
			modifier *= 0.2;
			modifier *= data1.caster().getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED);

			AttackAll.attackAll(data1.caster(),data1.targets(),(float)modifier);
			for(Entity entity: data1.targets()){
				SpellPower.Result power = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
				SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
				if(entity instanceof LivingEntity living) {
					vulnerability = SpellPower.getVulnerability(living, actualSchool);
				}
				double amount = modifier *  power.randomValue(vulnerability);
				entity.timeUntilRegen = 0;

				entity.damage(SpellDamageSource.player(actualSchool,data1.caster()), (float) amount);
			}
			return false;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"whirlwind"),(data) -> {
			MagicSchool actualSchool = MagicSchool.FROST;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"whirlwind")).impact[0].action.damage.spell_power_coefficient;
			modifier *= 0.4F+0.6F/(float)data1.targets().size()+(0.6F-0.6F/(float)data1.targets().size())*Math.min(3, EnchantmentHelper.getEquipmentLevel(Enchantments.SWEEPING,data1.caster()))/3;
			modifier *= 0.2;
			modifier *= data1.caster().getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED);

			AttackAll.attackAll(data1.caster(),data1.targets(),(float)modifier);

			return false;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"maelstrom"),(data) -> {
			MagicSchool actualSchool = MagicSchool.ARCANE;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"maelstrom")).impact[0].action.damage.spell_power_coefficient;
			modifier *= 0.4F+0.6F/(float)data1.targets().size()+(0.6F-0.6F/(float)data1.targets().size())*Math.min(3, EnchantmentHelper.getEquipmentLevel(Enchantments.SWEEPING,data1.caster()))/3;
			modifier *= 0.2;
			modifier *= data1.caster().getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED);


			AttackAll.attackAll(data1.caster(),data1.targets(),(float)modifier);
			for(Entity entity: data1.targets()){
				SpellPower.Result power = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
				SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
				if(entity instanceof LivingEntity living) {
					vulnerability = SpellPower.getVulnerability(living, actualSchool);
				}
				double amount = modifier *  power.randomValue(vulnerability);
				entity.timeUntilRegen = 0;

				entity.damage(SpellDamageSource.player(actualSchool,data1.caster()), (float) amount);
			}
			return false;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"inferno"),(data) -> {
			MagicSchool actualSchool = MagicSchool.FIRE;
			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"inferno")).impact[0].action.damage.spell_power_coefficient;
			modifier *= 0.4F+0.6F/(float)data1.targets().size()+(0.6F-0.6F/(float)data1.targets().size())*Math.min(3, EnchantmentHelper.getEquipmentLevel(Enchantments.SWEEPING,data1.caster()))/3;
			modifier *= 0.2;
			modifier *= data1.caster().getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED);

			AttackAll.attackAll(data1.caster(),data1.targets(),(float)modifier);
			for(Entity entity: data1.targets()){
				SpellPower.Result power = SpellPower.getSpellPower(actualSchool, (LivingEntity) data1.caster());
				SpellPower.Vulnerability vulnerability = SpellPower.Vulnerability.none;
				if(entity instanceof LivingEntity living) {
					vulnerability = SpellPower.getVulnerability(living, actualSchool);
				}
				double amount = modifier *  power.randomValue(vulnerability);
				entity.timeUntilRegen = 0;

				entity.damage(SpellDamageSource.player(actualSchool,data1.caster()), (float) amount);
			}
			return false;
		});
		CustomSpellHandler.register(new Identifier(MOD_ID,"smite"),(data) -> {

			CustomSpellHandler.Data data1 = (CustomSpellHandler.Data) data;
			float modifier = SpellRegistry.getSpell(new Identifier(MOD_ID,"smite")).impact[0].action.damage.spell_power_coefficient;

			AttackAll.attackAll(data1.caster(),data1.targets(),(float)modifier);
			for(Entity target : data1.targets()){
				if(target instanceof LivingEntity living && living.isUndead()){
					modifier = 3f;
				}
				if(target instanceof LivingEntity living && data1.caster() instanceof SpellCasterEntity caster && SpellRegistry.getSpell(new Identifier(MOD_ID,"fervoussmite")) != null){
					SpellPower.Result result = new SpellPower.Result(MagicSchool.HEALING, modifier * SpellPower.getSpellPower(MagicSchool.HEALING,data1.caster()).baseValue(), getCriticalChance(data1.caster(), data1.caster().getMainHandStack()), SpellPower.getCriticalMultiplier(data1.caster())+SpellPower.getVulnerability(living,MagicSchool.HEALING).criticalDamageBonus());
					SpellHelper.performImpacts(data1.caster().getWorld(), data1.caster(), target, SpellRegistry.getSpell(new Identifier(MOD_ID, "fervoussmite")),
							new SpellHelper.ImpactContext(1, 1, null, result, TargetHelper.TargetingMode.DIRECT));

				}
			}
			return true;
		});
		LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
			LootHelper.configure(id, tableBuilder, Spellblades.lootConfig.value, SpellbladeItems.entries);
		});
		LOGGER.info("Hello Fabric world!");
	}
}