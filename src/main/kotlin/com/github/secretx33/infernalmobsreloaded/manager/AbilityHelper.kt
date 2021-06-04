package com.github.secretx33.infernalmobsreloaded.manager

import com.cryptomorin.xseries.XPotion
import com.github.secretx33.infernalmobsreloaded.config.AbilityConfig
import com.github.secretx33.infernalmobsreloaded.config.AbilityConfigKeys
import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.config.MessageKeys
import com.github.secretx33.infernalmobsreloaded.config.Messages
import com.github.secretx33.infernalmobsreloaded.config.replace
import com.github.secretx33.infernalmobsreloaded.events.InfernalDamageDoneEvent
import com.github.secretx33.infernalmobsreloaded.events.InfernalDamageTakenEvent
import com.github.secretx33.infernalmobsreloaded.events.InfernalHealedEvent
import com.github.secretx33.infernalmobsreloaded.events.InfernalLightningStrike
import com.github.secretx33.infernalmobsreloaded.events.InfernalSpawnEvent
import com.github.secretx33.infernalmobsreloaded.model.Ability
import com.github.secretx33.infernalmobsreloaded.model.BlockModification
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.repositories.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.repositories.LootItemsRepo
import com.github.secretx33.infernalmobsreloaded.utils.Cuboid
import com.github.secretx33.infernalmobsreloaded.utils.displayName
import com.github.secretx33.infernalmobsreloaded.utils.futureSync
import com.github.secretx33.infernalmobsreloaded.utils.isAir
import com.github.secretx33.infernalmobsreloaded.utils.pdc
import com.github.secretx33.infernalmobsreloaded.utils.random
import com.github.secretx33.infernalmobsreloaded.utils.runSync
import com.github.secretx33.infernalmobsreloaded.utils.toUuid
import com.google.common.collect.Sets
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.DyeColor
import org.bukkit.FireworkEffect
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.Block
import org.bukkit.entity.Ageable
import org.bukkit.entity.Arrow
import org.bukkit.entity.Bat
import org.bukkit.entity.Bee
import org.bukkit.entity.ComplexLivingEntity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Fireball
import org.bukkit.entity.Firework
import org.bukkit.entity.Horse
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Parrot
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.Tameable
import org.bukkit.entity.ThrownPotion
import org.bukkit.entity.WitherSkull
import org.bukkit.entity.Zombie
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityTargetEvent.TargetReason
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.material.Colorable
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.lang.StrictMath.pow
import java.lang.reflect.Type
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import java.util.logging.Logger
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class AbilityHelper (
    private val plugin: Plugin,
    private val config: Config,
    private val messages: Messages,
    private val keyChain: KeyChain,
    private val abilityConfig: AbilityConfig,
    private val lootItemsRepo: LootItemsRepo,
    private val wgChecker: WorldGuardChecker,
    private val particlesHelper: ParticlesHelper,
    private val infernalMobTypesRepo: InfernalMobTypesRepo,
    private val logger: Logger,
){

    private val blockModifications = Sets.newConcurrentHashSet<BlockModification>()
    private val blocksBlackList = Sets.newConcurrentHashSet<Location>()

    fun removeAbilityEffects(entity: LivingEntity) {
        TODO("revert all attributes and chances and convert it back to a normal entity")
    }

    // abilities that are applied when mob spawns

    fun addAbilityEffects(entity: LivingEntity) {
        val abilitySet = entity.getAbilities() ?: return
        entity.addAbilities(abilitySet)
    }

    private fun LivingEntity.addAbilities(abilitySet: Set<Ability>) {
        abilitySet.forEach {
            when(it) {
                Ability.ARMOURED -> addArmouredAbility()
                Ability.FLYING -> addFlyingAbility()
                Ability.HEAVY -> addHeavyAbility()
                Ability.INVISIBLE -> addInvisibleAbility()
                Ability.MOLTEN -> addMoltenAbility()
                Ability.MOUNTED -> addMountedAbility()
                Ability.SPEEDY -> addSpeedyAbility()
                Ability.THIEF -> addThiefAbility()
                else -> {}
            }
        }
    }

    private fun LivingEntity.addArmouredAbility() {
        if(equipWithArmor()) return
        // fallback to potion effect if entity cannot wear armor
        addArmouredPotionEffect()
    }

    private fun LivingEntity.addArmouredPotionEffect() = addPermanentPotion(PotionEffectType.DAMAGE_RESISTANCE, Ability.ARMOURED, amplifier = max(0, abilityConfig.get<Int>(AbilityConfigKeys.ARMOURED_POTION_LEVEL) - 1))

    /**
     * Tries to equip default armor on entity, returning false if it cannot wear armor.
     *
     * @receiver LivingEntity the entity to equip armor onto
     * @return Boolean returns true if entity was successfully equipped with armor
     */
    private fun LivingEntity.equipWithArmor(): Boolean {
        if(!canWearArmor()) return false
        val equip = equipment ?: return false
        val dropChance = abilityConfig.getDouble(AbilityConfigKeys.ARMOURED_ARMOR_DROP_CHANCE).toFloat()

        equip.apply {
            helmet = lootItemsRepo.getLootItemOrNull("armoured_helmet")?.makeItem()
            chestplate = lootItemsRepo.getLootItemOrNull("armoured_chestplate")?.makeItem()
            leggings = lootItemsRepo.getLootItemOrNull("armoured_leggings")?.makeItem()
            boots =  lootItemsRepo.getLootItemOrNull("armoured_boots")?.makeItem()
            when(itemInMainHand.type) {
                Material.BOW -> lootItemsRepo.getLootItemOrNull("armoured_bow")?.makeItem()
                Material.CROSSBOW -> lootItemsRepo.getLootItemOrNull("armoured_crossbow")?.makeItem()
                else -> lootItemsRepo.getLootItemOrNull("armoured_sword")?.makeItem()
            }
        }
        EquipmentSlot.values().forEach { equip.setDropChance(it, dropChance) }
        return true
    }

    private fun LivingEntity.canWearArmor() = config.getEnumSet(ConfigKeys.MOB_TYPES_THAT_CAN_WEAR_ARMOR, EntityType::class.java).contains(type)

    private fun LivingEntity.addFlyingAbility() {
        val bat = world.spawn(location, Bat::class.java, SpawnReason.CUSTOM) {
            it.turnIntoMount(keyChain.infernalBatMountKey, false)
            it.addPermanentPotion(PotionEffectType.INVISIBILITY, Ability.MOUNTED, isAmbient = false, emitParticles = false)
            // makes the newly spawned bat goes in a random x z direction, upwards
            it.velocity = Vector(random.nextDouble() * 2 - 1.0, 1.0, random.nextDouble() * 2 - 1.0)
            it.removeWhenFarAway = !config.get<Boolean>(ConfigKeys.INFERNALS_ARE_PERSISTENT)
            it.multiplyMaxHp(3.5)
        }
        bat.addPassenger(this)
    }

    private fun LivingEntity.addHeavyAbility() {
        getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.apply {
            val resistAmount = abilityConfig.getDoublePair(AbilityConfigKeys.HEAVY_RESIST_PERCENTAGE).random()
            val mod = AttributeModifier(knockbackResistUID, Ability.HEAVY.name, resistAmount, AttributeModifier.Operation.ADD_NUMBER)
            removeModifier(mod)
            addModifier(mod)
        }
    }

    private fun LivingEntity.addInvisibleAbility() {
        addPermanentPotion(PotionEffectType.INVISIBILITY, Ability.INVISIBLE)
        if(invisibleMakesNoSound) isSilent = true
    }

    private val invisibleMakesNoSound: Boolean
        get() = abilityConfig.get(AbilityConfigKeys.INVISIBLE_DISABLE_ENTITY_SOUNDS)

    private fun LivingEntity.addMoltenAbility() = addPermanentPotion(PotionEffectType.FIRE_RESISTANCE, Ability.MOLTEN)

    private fun LivingEntity.addMountedAbility() {
        vehicle?.apply {
            removePassenger(this)
            if(passengers.isEmpty()) remove()
        }
        val mounts = rideableMounts

        // if player emptied the mount list, do not mount the entity
        if(mounts.isEmpty()) return
        // get a random mount entity class
        val mountClass = mounts.random().entityClass ?: return
        // spawn it, applying the necessary changes
        val mount = world.spawn(location, mountClass, SpawnReason.CUSTOM) { (it as LivingEntity).turnIntoMount(keyChain.infernalMountKey) }
        // and add the entity as its passenger
        mount.addPassenger(this)
    }

    private val rideableMounts
        get() = config.getEnumSet(ConfigKeys.MOBS_THAT_CAN_BE_RIDED_BY_MOUNTED_INFERNALS, EntityType::class.java) { it != null && it.isSpawnable && !ComplexLivingEntity::class.java.isAssignableFrom(it.entityClass as Class<*>) && LivingEntity::class.java.isAssignableFrom(it.entityClass as Class<*>) }

    private fun LivingEntity.turnIntoMount(tag: NamespacedKey, enablePotionParticles: Boolean = true) {
        // if entity that will be turned into a mount spawns mounted on something, remove that mount
        vehicle?.remove()
        // if entity that will be turned into a mount spawns with something mounted on it, remove that sneaky passenger
        passengers.forEach {
            removePassenger(it)
            it.remove()
        }
        (this as? Tameable)?.isTamed = true
        (this as? Colorable)?.color = DyeColor.values().random()

        if(type == EntityType.HORSE) {
            (this as Horse).apply {
                color = Horse.Color.values().random()
                style = Horse.Style.values().random()
            }
            inventory.apply {
                saddle = ItemStack(Material.SADDLE)
                armor = ItemStack(Material.DIAMOND_HORSE_ARMOR)
            }
        }
        // add armoured resistance potion to make the mount more resilient
        addPotionEffect(PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Int.MAX_VALUE, 2, enablePotionParticles, enablePotionParticles, false))
        // give speed potion to make the mount a bit faster
        addPotionEffect(PotionEffect(PotionEffectType.SPEED, Int.MAX_VALUE, 0, enablePotionParticles, enablePotionParticles, false))
        // and mark it as mount in its pdc, so I can check on the spawn event and death event for it
        pdc.set(tag, PersistentDataType.SHORT, 1)
    }

    private fun LivingEntity.addSpeedyAbility() {
        val movSpeed = (if(doesFly()) getAttribute(Attribute.GENERIC_FLYING_SPEED) else getAttribute(Attribute.GENERIC_MOVEMENT_SPEED))
            ?: return
        val speedBonus = abilityConfig.getDoublePair(AbilityConfigKeys.SPEEDY_BONUS).random()
        val mod = AttributeModifier(movSpeedUID, Ability.SPEEDY.name, speedBonus, AttributeModifier.Operation.ADD_SCALAR)
        movSpeed.removeModifier(mod)
        movSpeed.addModifier(mod)
    }

    private fun LivingEntity.addThiefAbility() {
        if(!canWearArmor()) return
        canPickupItems = true
    }

    // periodic tasks that require a target

    fun startTargetAbilityTasks(entity: LivingEntity, target: LivingEntity): List<Job> {
        val abilities = entity.getAbilities() ?: return emptyList()

        val jobList = ArrayList<Job>()
        abilities.forEach {
            when(it) {
                Ability.ARCHER -> makeArcherTask(entity, target)
                Ability.CALL_THE_GANG -> makeCallTheGangTask(entity, target)
                Ability.GHASTLY -> makeGhastlyTask(entity, target) // TODO("Make a damage modifier listener for this task")
                Ability.MORPH -> makeMorphTask(entity, target)
                Ability.MULTI_GHASTLY -> makeMultiGhastlyTask(entity, target)
                Ability.NECROMANCER -> makeNecromancerTask(entity, target) // TODO("Make a damage modifier listener for this task")
                Ability.POTIONS -> makePotionsTasks(entity, target)
                Ability.TELEPORT -> makeTeleportTask(entity, target)
                Ability.TOSSER -> makeTosserTask(entity, target)
                Ability.THIEF -> makeThiefTask(entity, target)
                Ability.WEBBER -> makeWebberTask(entity, target)
                else -> null
            }?.let { job -> jobList.add(job) }
        }
        return jobList
    }

    private fun makeArcherTask(entity: LivingEntity, target: LivingEntity) = CoroutineScope(Dispatchers.Default).launch {
        val nearbyRange = abilityConfig.getNearbyRange(Ability.ARCHER, 4.0)
        val speed = abilityConfig.getProjectileSpeed(Ability.ARCHER, 2.2)
        val delay = abilityConfig.getDouble(AbilityConfigKeys.ARCHER_ARROW_DELAY, minValue = 0.001).toLongDelay()
        val recheckDelay = abilityConfig.getRecheckDelay(Ability.ARCHER, 1.0).toLongDelay()
        val chance = abilityConfig.getAbilityChance(Ability.ARCHER, 0.05)

        while(isActive && !entity.isNotTargeting(target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance) continue
            val amount = abilityConfig.getIntPair(AbilityConfigKeys.ARCHER_ARROW_AMOUNT, minValue = 1).random()
            val victims = target.getValidNearbyTargetsAsync(nearbyRange) - entity

            for (i in 1..amount) {
                victims.forEach {
                    if(!isActive || entity.isNotTargeting(target)) return@launch
                    val dir = entity.shootDirection(it).normalize().add(Vector(0.0, 0.14, 0.0)).multiply(speed)
                    entity.shootProjectile(dir, Arrow::class.java)
                }
                delay(delay)
            }
        }
    }

    private fun makeMultiGhastlyTask(entity: LivingEntity, target: LivingEntity) = CoroutineScope(Dispatchers.Default).launch {
        val nearbyRange = abilityConfig.getNearbyRange(Ability.MULTI_GHASTLY, 4.0)
        val delay = abilityConfig.getDouble(AbilityConfigKeys.MULTI_GHASTLY_FIREBALL_DELAY, minValue = 0.001).toLongDelay()
        val recheckDelay = abilityConfig.getRecheckDelay(Ability.MULTI_GHASTLY, 2.0).toLongDelay()
        val chance = abilityConfig.getAbilityChance(Ability.MULTI_GHASTLY, 0.25)
        val speed = abilityConfig.getProjectileSpeed(Ability.MULTI_GHASTLY, 1.75)

        println("Multi Ghastly, nearbyRange = $nearbyRange, delay = $delay, recheckDelay = $recheckDelay, chance = $chance, speed = $speed")

        while(isActive && !entity.isNotTargeting(target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance) continue
            val amount = abilityConfig.getIntPair(AbilityConfigKeys.MULTI_GHASTLY_FIREBALL_AMOUNT, minValue = 1).random()
            println("Multi Ghastly amount = $amount")
            val victims = target.getValidNearbyTargetsAsync(nearbyRange) - entity

            for (i in 1..amount) {
                victims.forEach {
                    if(!isActive || entity.isNotTargeting(target)) return@launch
                    val dir = entity.shootDirection(it).multiply(speed)
                    entity.shootProjectile(dir, Fireball::class.java)
                }
                delay(delay)
            }
        }
    }

    private fun makeCallTheGangTask(entity: LivingEntity, target: LivingEntity) = CoroutineScope(Dispatchers.Default).launch {
        val recheckDelay = abilityConfig.getRecheckDelay(Ability.CALL_THE_GANG, 2.0).toLongDelay()
        val chance = abilityConfig.getAbilityChance(Ability.CALL_THE_GANG, 0.025)

        while(isActive && !entity.isNotTargeting(target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance) continue
            val amount = abilityConfig.getIntAmounts(Ability.CALL_THE_GANG, 2).random()
            val potency = abilityConfig.getAbilityPotency(Ability.CALL_THE_GANG, 3).random()
            val potionDuration = abilityConfig.getDuration(Ability.CALL_THE_GANG, 8.0).random()

            runSync(plugin) {
                repeat(amount) {
                    entity.world.spawnEntity(entity.location, entity.type, SpawnReason.CUSTOM) {
                        (it as? Mob)?.target = target
                        (it as? Ageable)?.setBaby()
                        (it as? LivingEntity)?.addPotion(PotionEffectType.SPEED, Ability.CALL_THE_GANG, amplifier = potency, duration = potionDuration)
                    }
                }
                particlesHelper.sendParticle(entity, Particle.TOTEM, entity.width + 1, 30)
            }
        }
    }

    private fun makeGhastlyTask(entity: LivingEntity, target: LivingEntity) = CoroutineScope(Dispatchers.Default).launch {
        val nearbyRange = abilityConfig.getNearbyRange(Ability.GHASTLY, 4.0)
        val recheckDelay = abilityConfig.getRecheckDelay(Ability.GHASTLY, 1.5).toLongDelay()
        val chance = abilityConfig.getAbilityChance(Ability.GHASTLY, 0.25)
        val speed = abilityConfig.getProjectileSpeed(Ability.GHASTLY, 1.75)

        while(isActive && !entity.isNotTargeting(target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance) continue

            val victims = target.getValidNearbyTargetsAsync(nearbyRange) - entity
            victims.forEach {
                val dir = entity.shootDirection(it).multiply(speed)
                entity.shootProjectile(dir, Fireball::class.java)
            }
        }
    }

    private fun makeMorphTask(entity: LivingEntity, target: LivingEntity) = CoroutineScope(Dispatchers.Default).launch {
        val recheckDelay = abilityConfig.getRecheckDelay(Ability.MORPH, 1.0).toLongDelay()
        val chance = abilityConfig.getAbilityChance(Ability.MORPH, 0.01)

        while(isActive && !entity.isNotTargeting(target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance) continue
            val newType = infernalMobTypesRepo.getRandomInfernalType(entity)
            val keepHpPercent = abilityConfig.get<Boolean>(AbilityConfigKeys.MORPH_KEEP_HP_PERCENTAGE)

            runSync(plugin) {
                entity.world.spawn(entity.location, newType.entityClass, SpawnReason.CUSTOM) {
                    Bukkit.getPluginManager().callEvent(InfernalSpawnEvent(it as LivingEntity, newType))
                    if (keepHpPercent) it.copyHpPercentage(entity)
                    if (it !is Mob) return@spawn

                    EntityTargetLivingEntityEvent(it, target, TargetReason.REINFORCEMENT_TARGET).let { event ->
                        Bukkit.getPluginManager().callEvent(event)
                        if (!event.isCancelled) it.target = target
                    }
                }
                entity.remove()
            }
            cancel()
        }
    }

    private fun makeNecromancerTask(entity: LivingEntity, target: LivingEntity) = CoroutineScope(Dispatchers.Default).launch {
        val nearbyRange = abilityConfig.getNearbyRange(Ability.NECROMANCER, 4.0)
        val recheckDelay = abilityConfig.getRecheckDelay(Ability.NECROMANCER, 1.25).toLongDelay()
        val chance = abilityConfig.getAbilityChance(Ability.NECROMANCER, 0.25)
        val speed = abilityConfig.getProjectileSpeed(Ability.NECROMANCER, 1.7)

        while(isActive && !entity.isNotTargeting(target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance) continue

            val victims = target.getValidNearbyTargetsAsync(nearbyRange) - entity
            victims.forEach {
                val dir = entity.shootDirection(it).multiply(speed)
                entity.shootProjectile(dir, WitherSkull::class.java)
            }
        }
    }

    private fun makePotionsTasks(entity: LivingEntity, target: LivingEntity) = CoroutineScope(Dispatchers.Default).launch {
        val nearbyRange = abilityConfig.getNearbyRange(Ability.POTIONS, 4.0)
        val chance = abilityConfig.getAbilityChance(Ability.POTIONS, 0.05)
        val recheckDelay = abilityConfig.getRecheckDelay(Ability.POTIONS, 1.0).toLongDelay()

        while(isActive && !entity.isNotTargeting(target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance) continue
            val amount = abilityConfig.getIntAmounts(Ability.POTIONS, 1, minValue = 1).random()
            val victims = target.getValidNearbyTargetsAsync(nearbyRange) - entity

            for (i in 1..amount) {
                victims.forEach { victim ->
                    if (!isActive || entity.isNotTargeting(target)) return@launch
                    // if potion type is invalid or empty, silently fail
                    val type = abilityConfig.get(AbilityConfigKeys.POTIONS_ENABLED_TYPES, emptyList<String>())
                        .randomOrNull()
                        ?.let { name -> XPotion.matchXPotion(name).orElse(null)?.parsePotionEffectType() }
                        ?: return@forEach

                    entity.throwPotion(victim, type)
                }
                val throwDelay = abilityConfig.getDoublePair(AbilityConfigKeys.POTIONS_THROW_DELAY).random().toLongDelay()
                delay(throwDelay)
            }
        }
    }

    private fun LivingEntity.throwPotion(victim: LivingEntity, type: PotionEffectType) {
        val duration = abilityConfig.getDuration(Ability.POTIONS, 2.0).random()
        val potency = abilityConfig.getAbilityPotency(Ability.POTIONS, 2, minValue = 1).random() - 1
        val dir = shootDirection(victim).normalize().apply { y += 0.1 }.multiply(random.nextDouble() * 0.5 + 1)

        val potionItem = ItemStack(randomPotionMaterial).modifyPotion(type, duration, potency)
        runSync(plugin) {
            world.spawn(eyeLocation, ThrownPotion::class.java) { potion ->
                potion.item = potionItem
                potion.velocity = dir
                potion.shooter = this
            }
        }
    }

    private val randomPotionMaterial
        get() = if(random.nextDouble() <= 0.75) Material.LINGERING_POTION else Material.SPLASH_POTION

    private fun ItemStack.modifyPotion(potionType: PotionEffectType, duration: Double, amplifier: Int): ItemStack {
        require(type == Material.LINGERING_POTION || type == Material.SPLASH_POTION) { "ItemStack used as this for function modifyPotion needs to be lingering or splash potion, and $type is not" }
        val meta = itemMeta as PotionMeta
        meta.addCustomEffect(PotionEffect(potionType, (duration * 20).toInt().coerceAtLeast(0), amplifier), true)
        itemMeta = meta
        return this
    }

    private fun makeTeleportTask(entity: LivingEntity, target: LivingEntity) = CoroutineScope(Dispatchers.Default).launch {
        val recheckDelay = abilityConfig.getRecheckDelay(Ability.TELEPORT, 2.5).toLongDelay()
        val chance = abilityConfig.getAbilityChance(Ability.TELEPORT, 0.6)

        while(isActive && !entity.isNotTargeting(target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance) continue

            val world = entity.world
            val dest = target.location.apply {
                x += random.nextDouble() * 2 - 1
                y += 0.2
                z += random.nextDouble() * 2 - 1
            }
            // if the block that the entity will teleport at won't make it suffocate
            if(world.getBlockAt(dest).isPassable && (entity.height <= 1 || world.getBlockAt(dest.clone().apply { y += 1 }).isPassable)) {
                entity.teleportAsync(dest)
                world.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 0.85f, random.nextFloat() * 0.8f + 0.7f)
                continue
            }
            // else just teleport the entity right on the target's feet
            entity.teleportAsync(target.location)
            world.playSound(target.location, Sound.ENTITY_ENDERMAN_TELEPORT, 0.85f, random.nextFloat() * 0.8f + 0.7f)
        }
    }

    private fun makeThiefTask(entity: LivingEntity, target: LivingEntity) = CoroutineScope(Dispatchers.Default).launch {
        // if thief ability should affect only players and target is not one, return
        val affectOnlyPlayers = abilityConfig.getAffectsOnlyPlayers(Ability.THIEF, true)
        if(affectOnlyPlayers && target.type != EntityType.PLAYER || target.equipment == null) return@launch

        val recheckDelay = abilityConfig.getRecheckDelay(Ability.THIEF, 3.0).toLongDelay()
        val chance = abilityConfig.getAbilityChance(Ability.THIEF, 0.05)
        val requireLoS = abilityConfig.doesRequireLineOfSight(Ability.THIEF)
        val sendMessage = abilityConfig.getSendMessage(Ability.THIEF)

        while(isActive && !entity.isNotTargeting(target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance || (requireLoS && !entity.hasLineOfSight(target))) continue

            val equip = target.equipment ?: return@launch
            // slot chosen to have its equipment stolen, skipping this interaction if there's none (and yeah EntityEquipment#getItem returns null even if its annotated with @NonNull)
            val chosenSlot = EquipmentSlot.values().filter { slot -> equip.getItem(slot).let { it != null && !it.isAir() } }.randomOrNull() ?: continue
            val durabilityLoss = abilityConfig.getDurabilityLoss(Ability.THIEF, 0.04).random()
            val item = equip.getItem(chosenSlot)
            val damagedItem = item.damageItemBy(durabilityLoss).markAsStolen()

            // set air in that slot, removing the item from player's inventory
            equip.setItem(chosenSlot, ItemStack(Material.AIR))
            // drop the stolen item in the thief's feet if its not broken
            if(!damagedItem.isAir()) runSync(plugin) { entity.world.dropItem(entity.location, damagedItem) }

            (target as? Player)?.apply {
                updateInventory()
                // send message to player warning the fact, if this option is enabled
                if(!sendMessage) return@apply
                val message = if(!damagedItem.isAir()) messages.get(MessageKeys.THIEF_MESSAGE_TO_TARGET) else messages.get(MessageKeys.THIEF_MESSAGE_TO_TARGET_ITEM_BROKE)
                target.sendMessage(message
                    .replace("<entity>", entity.displayName)
                    .replace("<item>", item.displayName))
            }
        }
    }

    private fun ItemStack.markAsStolen(): ItemStack {
        if(isAir()) return this
        itemMeta?.let { meta ->
            meta.pdc.apply {
                set(keyChain.stolenItemByThiefKey, PersistentDataType.SHORT, 1)
                set(keyChain.thiefItemDurabilityKey, PersistentDataType.INTEGER, (meta as? Damageable)?.damage ?: 0)
            }
            itemMeta = meta
        }
        return this
    }

    private fun makeTosserTask(entity: LivingEntity, target: LivingEntity) = CoroutineScope(Dispatchers.Default).launch {
        val nearbyRange = abilityConfig.getNearbyRange(Ability.TOSSER, 4.0)
        val recheckDelay = abilityConfig.getRecheckDelay(Ability.TOSSER, 1.5).toLongDelay()
        val chance = abilityConfig.getAbilityChance(Ability.TOSSER, 0.5)
        val requireLoS = abilityConfig.doesRequireLineOfSight(Ability.TOSSER)
        val sneakerMultiplier = abilityConfig.getDouble(AbilityConfigKeys.TOSSER_SNEAK_MULTIPLIER_PERCENTAGE)

        while(isActive && !entity.isNotTargeting(target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance || (requireLoS && !entity.hasLineOfSight(target))) continue

            val victims = target.getValidNearbyTargetsAsync(nearbyRange) - entity

            // toss victim and all nearby entities
            victims.forEach {
                val x = 1.3 * abilityConfig.getDistanceMultiplier(Ability.TOSSER) * (random.nextDouble() * 2 - 1)
                val y = 0.75 * abilityConfig.getHeightMultiplier(Ability.TOSSER) + (abilityConfig.getHeightMultiplier(Ability.TOSSER) * random.nextDouble() * 0.5).let { h -> if(h <= 1.0) h else sqrt(h) }
                val z = 1.3 * abilityConfig.getDistanceMultiplier(Ability.TOSSER) * (random.nextDouble() * 2 - 1)

                val victimMulti = if((it as? Player)?.isSneaking == true) sneakerMultiplier else 1.0
                it.velocity = Vector(x, y, z).multiply(victimMulti)
            }
        }
    }

    private fun makeWebberTask(entity: LivingEntity, target: LivingEntity) = CoroutineScope(Dispatchers.Default).launch {
        val chance = abilityConfig.getAbilityChance(Ability.WEBBER, 0.08)
        val recheckDelay = abilityConfig.getRecheckDelay(Ability.WEBBER, 1.5).toLongDelay()

        while(isActive && !entity.isNotTargeting(target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance) continue
            launchCobweb(target)
        }
    }

    private fun launchCobweb(target: LivingEntity) {
        val trapDensity = abilityConfig.getDoublePair(AbilityConfigKeys.WEBBER_TRAP_DENSITY, maxValue = 1.0).random()
        val duration = abilityConfig.getDuration(Ability.WEBBER, 5.0, minValue = 0.1).random().toLongDelay()
        val blocks = target.makeCuboidAround().blockList().filter { random.nextDouble() <= trapDensity && it.canMobGrief() }
        val blockMod = BlockModification(blocks, blockModifications, blocksBlackList) { list -> list.forEach { it.type = Material.COBWEB } }
        blocksBlackList.addAll(blockMod.blockLocations)
        blockModifications.add(blockMod)

        runSync(plugin) { blockMod.make() }
        CoroutineScope(Dispatchers.Default).launch {
            delay(duration)
            runSync(plugin) { blockMod.unmake() }
        }
    }

    private fun Block.canMobGrief(): Boolean = type != Material.BEDROCK && isPassable && !blocksBlackList.contains(location) && wgChecker.canMobGriefBlock(this)

    private fun LivingEntity.makeCuboidAround(): Cuboid {
        val maxRadius = abilityConfig.getIntPair(AbilityConfigKeys.WEBBER_MAX_RADIUS).random()
        val lowerBound = location.apply {
            x -= ceil(width) + 1 + maxRadius
            z -= ceil(width) + 1 + maxRadius
        }
        val upperBound = location.apply {
            x += ceil(width) + 1 + maxRadius
            y += floor(height) + 1 + max(0, maxRadius - 1)
            z += ceil(width) + 1 + maxRadius
        }
        return Cuboid(lowerBound, upperBound)
    }

    private fun <T : Projectile> LivingEntity.shootProjectile(dir: Vector, proj: Class<out T>) {
        val loc = eyeLocation.apply { y -= height / 12 }
        runSync(plugin) {
            world.spawn(loc, proj, SpawnReason.CUSTOM) {
                it.velocity = dir
                (it as Projectile).shooter = this
            }
        }
    }

    private fun LivingEntity.shootDirection(): Vector? {
        if(this !is Mob) return eyeLocation.direction
        val target = target ?: return null
        return shootDirection(target)
    }

    private fun LivingEntity.shootDirection(target: LivingEntity): Vector {
        val src = eyeLocation
        val dest = target.location.apply {
            y += target.height * 0.75
        }
        val difX = dest.x - src.x
        val difZ = dest.z - src.z
        val difXZ = sqrt(pow(difX, 2.0) + pow(difZ, 2.0))
        val difY = (dest.y - src.y) * min(1.5, max(1.0, 0.3 + difXZ / 19))
        // mobs usually don't have rotating heads, so we gotta calculate pitch manually (pitch is the angle)
        val pitch = Math.toDegrees(atan(-difY / difXZ))
        // yaw is because mobs are not very precise with their yaw direction (its like the angle, but rotated 90 degrees sideways)
        val yaw = Math.toDegrees(atan2(difZ, difX))
        src.pitch = pitch.toFloat()
        src.yaw = yaw.toFloat() - 90f
        return src.direction
    }

    private fun LivingEntity.isNotTargeting(target: LivingEntity) = isDead || !isValid  || target.isDead || !target.isValid || (this is Mob && this.target?.uniqueId != target.uniqueId)


    // trigger on death abilities

    fun triggerOnDeathAbilities(entity: LivingEntity) {
        val abilities = entity.getAbilities() ?: return
        abilities.forEach {
            when(it) {
                Ability.GHOST -> entity.triggerGhost()
                Ability.KAMIKAZE -> entity.triggerKamizake()
                else -> {}
            }
        }
    }

    private fun LivingEntity.triggerGhost() {
        val target: LivingEntity? = (this as? Mob)?.target
        val haunted = random.nextDouble() <= abilityConfig.getDouble(AbilityConfigKeys.GHOST_EVIL_CHANCE)
        val hauntedPrefix = if(haunted) "haunted_" else ""
        val itemDropChance = abilityConfig.getDouble(AbilityConfigKeys.GHOST_ITEM_DROP_CHANCE, maxValue = 1.0).toFloat()

        val infernalType = infernalMobTypesRepo.getInfernalTypeOrNull("${hauntedPrefix}ghost") ?: run {
            logger.severe("Oops, seems like you have removed '${hauntedPrefix}ghost' from your mobs.yml configuration file, without it, the Ghost ability don't work, please re-add the missing ghost types and reload. If you prefer, you may also just delete your mobs.yml and let it regenerate automatically using the reload command.")
            return
        }

        val abilitySet = if(haunted) setOf(Ability.BLINDING, Ability.FLYING, Ability.NECROMANCER, Ability.WITHERING) else setOf(Ability.CONFUSION, Ability.FLYING, Ability.GHASTLY, Ability.HUNGER)

        val ghost = world.spawn(location, Zombie::class.java, SpawnReason.CUSTOM) {
            it.setAdult()
            it.addPermanentPotion(PotionEffectType.INVISIBILITY, Ability.GHOST, isAmbient = true, emitParticles = true)
            it.canPickupItems = false

            val equip = it.equipment
            equip?.apply {
                helmet = lootItemsRepo.getLootItemOrNull("${hauntedPrefix}ghost_helmet")?.makeItem()
                chestplate = lootItemsRepo.getLootItemOrNull("${hauntedPrefix}ghost_chestplate")?.makeItem()
                leggings = lootItemsRepo.getLootItemOrNull("${hauntedPrefix}ghost_leggings")?.makeItem()
                boots = lootItemsRepo.getLootItemOrNull("${hauntedPrefix}ghost_boots")?.makeItem()
                setItemInMainHand(lootItemsRepo.getLootItemOrNull("${hauntedPrefix}ghost_weapon")?.makeItem())
            }
            EquipmentSlot.values().forEach { slot -> equip?.setDropChance(slot, itemDropChance) }
            InfernalSpawnEvent(it, infernalType, randomAbilities = false, abilitySet = abilitySet).callEvent()
            it.target = target
        }

        if(haunted) {
            ghost.world.playSound(ghost.eyeLocation, Sound.ENTITY_ENDERMAN_SCREAM, 0.9f, 1f)
            CoroutineScope(Dispatchers.Default).launch {
                delay(300L)
                ghost.world.playSound(ghost.eyeLocation, Sound.ENTITY_ENDERMAN_STARE, 0.6f, 1f)
            }
        }
        else ghost.world.playSound(ghost.location, Sound.AMBIENT_CAVE, 1f, random.nextFloat() + 0.5f)

        CoroutineScope(Dispatchers.Default).launch {
            repeat(6) {
                if(haunted) particlesHelper.sendParticle(ghost, Particle.SMOKE_LARGE, 2.5, 90)
                else particlesHelper.sendParticle(ghost, Particle.CLOUD, 2.0, 35)
                delay(150L)
            }
        }
    }

    private fun LivingEntity.triggerKamizake() {
        val power = abilityConfig.getDouble(AbilityConfigKeys.KAMIZAZE_EXPLOSION_POWER).toFloat()
        val setFire = abilityConfig.get<Boolean>(AbilityConfigKeys.KAMIZAZE_SET_ON_FIRE)
        val breakBlocks = abilityConfig.get<Boolean>(AbilityConfigKeys.KAMIZAZE_BREAK_BLOCKS)

        location.createExplosion(this, power, setFire, breakBlocks)
    }

    // abilities that are triggered when an infernal causes damage

    fun triggerOnDamageDoneAbilities(event: InfernalDamageDoneEvent) {
        val abilities = event.entity.getAbilities() ?: return

        abilities.forEach {
            when(it) {
                Ability.BERSERK -> event.triggerBerserk()
                Ability.BLINDING -> event.triggerBlinding()
                Ability.CONFUSION -> event.triggerConfusion()
                Ability.HUNGER -> event.triggerHunger()
                Ability.LEVITATE -> event.triggerLevitate()
                Ability.LIFESTEAL -> event.triggerLifesteal()
                Ability.LIGHTNING -> event.triggerLightning()
                Ability.MOLTEN -> event.triggerMolten()
                Ability.POISONOUS -> event.triggerPoisonous()
                Ability.SLOWNESS -> event.triggerSlowness()
                Ability.RUST -> event.triggerRust()
                Ability.WEAKNESS -> event.triggerWeakness()
                Ability.WITHERING -> event.triggerWithering()
                else -> {}
            }
        }
    }

    private fun InfernalDamageDoneEvent.triggerBerserk() {
        val bonus = abilityConfig.getDoublePair(AbilityConfigKeys.BERSERK_CAUSED_DAMAGE_BONUS)
        damageMulti = bonus.random()
    }

    private fun InfernalDamageDoneEvent.triggerBlinding() {
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Ability.BLINDING, 0.75)
        if(random.nextDouble() > chance) return

        // blinds the defender for some time
        val duration = abilityConfig.getDuration(Ability.BLINDING, 7.0).random()
        defender.addPotion(PotionEffectType.BLINDNESS, Ability.BLINDING, duration)
    }

    private fun InfernalDamageDoneEvent.triggerConfusion() {
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Ability.CONFUSION, 0.4)
        if(random.nextDouble() > chance) return

        // makes the defender nauseated for some time
        val duration = abilityConfig.getDuration(Ability.CONFUSION, 8.0).random()
        defender.addPotion(PotionEffectType.CONFUSION, Ability.CONFUSION, duration)
    }

    private fun InfernalDamageDoneEvent.triggerHunger() {
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Ability.HUNGER, 0.7)
        if(random.nextDouble() > chance) return

        // makes the defender hunger for some time
        val potency = max(0, abilityConfig.getAbilityPotency(Ability.HUNGER, 8).random() - 1)
        val duration = abilityConfig.getDuration(Ability.HUNGER, 30.0).random()
        defender.addPotion(PotionEffectType.HUNGER, Ability.HUNGER, duration, amplifier = potency)
    }

    private fun InfernalDamageDoneEvent.triggerLevitate() {
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Ability.LEVITATE, 0.4)
        if(random.nextDouble() > chance) return

        // makes the defender levitate for some time
        val potency = max(0, abilityConfig.getAbilityPotency(Ability.LEVITATE, 6).random() - 1)
        val duration = abilityConfig.getDuration(Ability.LEVITATE, 6.0).random()
        defender.addPotion(PotionEffectType.LEVITATION, Ability.LEVITATE, duration, amplifier = potency)
    }

    private fun InfernalDamageDoneEvent.triggerLightning() {
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Ability.LIGHTNING, 0.25)
        if(random.nextDouble() > chance) return

        val strikeLocation = defender.location
        InfernalLightningStrike(entity, infernalType, strikeLocation).callEvent()
        world.strikeLightning(strikeLocation)
    }

    private fun InfernalDamageDoneEvent.triggerLifesteal() {
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Ability.LIFESTEAL, 0.75)
        if(random.nextDouble() > chance) return

        val healingAmount = abilityConfig.getDoublePair(AbilityConfigKeys.LIFESTEAL_HEALING_PERCENTAGE).random()
        val effectiveHealed = entity.heal(damage * healingAmount)
        if(effectiveHealed <= 0.0) return

        // notify that this entity has been healed so its boss bar can be updated
        InfernalHealedEvent(entity, infernalType, effectiveHealed).callEvent()
        // TODO("Add particle effects to visually indicate the healing effect occurring")
    }

    // returns the effective health healed of this living entity
    private fun LivingEntity.heal(healingAmount: Double): Double {
        val maxHp = getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: return 0.0
        val oldHp = health
        health = min(maxHp, health + healingAmount)
        return health - oldHp
    }

    private fun InfernalDamageDoneEvent.triggerMolten() {
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Ability.MOLTEN, 0.6)
        if(random.nextDouble() > chance) return

        // sets the attacker on defender
        val duration = abilityConfig.getDuration(Ability.MOLTEN, 8.0).random()
        defender.fireTicks = (duration * 20.0).toInt()
    }

    private fun InfernalDamageDoneEvent.triggerPoisonous() {
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Ability.POISONOUS, 0.8)
        if(random.nextDouble() > chance) return

        // poisons the defender
        val potency = max(0, abilityConfig.getAbilityPotency(Ability.POISONOUS, 6).random() - 1)
        val duration = abilityConfig.getDuration(Ability.POISONOUS, 8.0).random()
        defender.addPotion(PotionEffectType.POISON, Ability.POISONOUS, duration, amplifier = potency)
    }

    private fun InfernalDamageDoneEvent.triggerRust() {
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Ability.RUST, 0.6)
        if(random.nextDouble() > chance) return
        val mhDurabilityLoss = abilityConfig.getDurabilityLoss(Ability.RUST, 0.15).random()
        val ohDurabilityLoss = abilityConfig.getDurabilityLoss(Ability.RUST, 0.15).random()
        val sendMessage = abilityConfig.getSendMessage(Ability.RUST)

        var corrodedSomething = false  // to prevent message being sent if no tools got corroded
        val randomNum = random.nextInt(3) // 0 = mh, 1 = oh, 2 = both hands
        defender.equipment?.apply {
            // damaged when 0 or 2
            if(randomNum != 1) {
                val item = itemInMainHand.damageItemBy(mhDurabilityLoss)
                if(item.isDamageable()) corrodedSomething = true
                setItemInMainHand(item, true)
            }
            // damaged when 1 or 2
            if(randomNum != 0) {
                val item = itemInOffHand.damageItemBy(ohDurabilityLoss)
                if(item.isDamageable()) corrodedSomething = true
                setItemInOffHand(item, true)
            }
        }
        (defender as? Player)?.apply {
            updateInventory()
            if(sendMessage && corrodedSomething) sendMessage(messages.get(MessageKeys.RUST_CORRODE_TOOLS_MESSAGE))
        }
    }

    private fun ItemStack.damageItemBy(damageAmount: Double): ItemStack {
        val meta = itemMeta
        // return the unmodified item if the item is not damageable (second check for Damageable is for the auto cast)
        if(!isDamageable() || meta !is Damageable) return this
        val damage = (type.maxDurability * damageAmount).toInt()
        meta.damage = min(type.maxDurability.toInt(), meta.damage + damage)
        // if item broke while damaging it, return air
        if(meta.damage >= type.maxDurability) return ItemStack(Material.AIR)
        itemMeta = meta
        return this
    }

    private fun ItemStack.isDamageable() = !isAir() && type.maxDurability > 0.toShort() && itemMeta.let { it != null && it is Damageable }

    private fun InfernalDamageDoneEvent.triggerSlowness() {
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Ability.SLOWNESS, 0.7)
        if(random.nextDouble() > chance) return

        // gives slow effect to the defender
        val potency = max(0, abilityConfig.getAbilityPotency(Ability.SLOWNESS, 3).random() - 1)
        val duration = abilityConfig.getDuration(Ability.SLOWNESS, 6.0).random()
        defender.addPotion(PotionEffectType.SLOW, Ability.SLOWNESS, duration, amplifier = potency)
    }

    private fun InfernalDamageDoneEvent.triggerWeakness() {
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Ability.WEAKNESS, 0.7)
        if(random.nextDouble() > chance) return

        // gives weakness effect to the defender
        val potency = max(0, abilityConfig.getAbilityPotency(Ability.WEAKNESS, 1, minValue = 1).random() - 1)
        val duration = abilityConfig.getDuration(Ability.WEAKNESS, 5.0).random()
        defender.addPotion(PotionEffectType.WEAKNESS, Ability.WEAKNESS, duration, amplifier = potency)
    }

    private fun InfernalDamageDoneEvent.triggerWithering() {
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Ability.WITHERING, 0.65)
        if(random.nextDouble() > chance) return

        // gives wither effect to the defender
        val potency = max(0, abilityConfig.getAbilityPotency(Ability.WITHERING, 1).random() - 1)
        val duration = abilityConfig.getDuration(Ability.WITHERING, 6.0).random()
        defender.addPotion(PotionEffectType.WITHER, Ability.WITHERING, duration, amplifier = potency)
    }

    // abilities that are triggered when an infernal takes damage

    fun triggerOnDamageTakenAbilities(event: InfernalDamageTakenEvent) {
        val abilities = event.entity.getAbilities() ?: return

        abilities.forEach {
            when(it) {
                Ability.BERSERK -> event.triggerBerserk()
                Ability.FIREWORK -> event.triggerFirework()
                Ability.MOLTEN -> event.triggerMolten()
                Ability.POISONOUS -> event.triggerPoisonous()
                Ability.THORNMAIL -> event.triggerThornmail()
                else -> {}
            }
        }
    }

    private fun InfernalDamageTakenEvent.triggerBerserk() {
        val bonus = abilityConfig.getDoublePair(AbilityConfigKeys.BERSERK_RECEIVED_DAMAGE_BONUS)
        damageMulti = bonus.random()
    }

    private fun InfernalDamageTakenEvent.triggerFirework() {
        val chance = abilityConfig.getAbilityChanceOnDamageTaken(Ability.FIREWORK, 0.25)
        if(random.nextDouble() > chance) return

        world.spawn(attacker.location.apply {
            x += random.nextDouble() - 0.5
            y += random.nextDouble() * 0.8 + 0.1
            z += random.nextDouble() - 0.5
        }, Firework::class.java, SpawnReason.CUSTOM) { it.prepareFirework(entity) }.detonate()
    }

    private fun Firework.prepareFirework(owner: LivingEntity) {
        val meta = fireworkMeta
        val effect = FireworkEffect.builder().with(FireworkEffect.Type.values().random())
            .withColor(randomColor)
            .flicker(true)
            .trail(random.nextBoolean())

        // randomly add more colors and fade effects
        if(random.nextBoolean()) effect.withColor(randomColor)
        if(random.nextInt(3) == 0) effect.withColor(randomColor)
        if(random.nextBoolean()) effect.withFade(randomColor)
        if(random.nextBoolean()) effect.withFade(randomColor)
        if(random.nextInt(3) == 0) effect.withFade(randomColor)

        meta.clearEffects()
        meta.addEffect(effect.build())
        fireworkMeta = meta

        shooter = owner
        pdc.set(keyChain.fireworkOwnerUuidKey, PersistentDataType.STRING, owner.uniqueId.toString())
    }

    private val randomColor get() = Color.fromRGB(random.nextInt(255), random.nextInt(255), random.nextInt(255))

    private fun InfernalDamageTakenEvent.triggerMolten() {
        if(cause.isNotMelee()) return
        val chance = abilityConfig.getAbilityChanceOnDamageTaken(Ability.MOLTEN, 0.4)
        if(random.nextDouble() > chance) return

        // sets the attacker on fire
        val duration = abilityConfig.getDuration(Ability.MOLTEN, 8.0).random()
        attacker.fireTicks = (duration * 20.0).toInt()
    }

    private fun InfernalDamageTakenEvent.triggerPoisonous() {
        if(cause.isNotMelee()) return
        val chance = abilityConfig.getAbilityChanceOnDamageTaken(Ability.POISONOUS, 0.2)
        if(random.nextDouble() > chance) return

        // poisons the attacker
        val potency = max(0, abilityConfig.getAbilityPotency(Ability.POISONOUS, 6).random() - 1)
        val duration = abilityConfig.getDuration(Ability.POISONOUS, 8.0).random()
        attacker.addPotion(PotionEffectType.POISON, Ability.POISONOUS, duration, amplifier = potency)
    }

    private fun InfernalDamageTakenEvent.triggerThornmail() {
        val chance = abilityConfig.getAbilityChance(Ability.THORNMAIL, 0.6)
        if(random.nextDouble() > chance) return

        // reflect part of the damage to the attacker
        val reflectPercentage = abilityConfig.getDoublePair(AbilityConfigKeys.THORMAIL_REFLECTED_AMOUNT).random()
        particlesHelper.sendParticle(entity, Ability.THORNMAIL)
        attacker.damage(damage * reflectPercentage, entity)
    }

    private fun EntityDamageEvent.DamageCause.isNotMelee() = this != EntityDamageEvent.DamageCause.ENTITY_ATTACK && this != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK && this != EntityDamageEvent.DamageCause.CONTACT && this != EntityDamageEvent.DamageCause.CRAMMING && this != EntityDamageEvent.DamageCause.THORNS

    // utility functions

    private fun Double.toLongDelay() = (this * 1000.0).toLong()

    private fun LivingEntity.getValidNearbyTargets(range: Double) = location.getNearbyLivingEntities(range) { !it.isDead && it.isValid }.takeUnless { it.isEmpty() } ?: listOf(this)

    private suspend fun LivingEntity.getValidNearbyTargetsAsync(range: Double) = futureSync(plugin) { getValidNearbyTargets(range) }

    private fun LivingEntity.multiplyMaxHp(percentage: Double) {
        val hp = getAttribute(Attribute.GENERIC_MAX_HEALTH) ?: return
        val hpMod = AttributeModifier(healthUID, "health_multi", percentage, AttributeModifier.Operation.ADD_SCALAR)
        val percentHP = health / hp.value
        hp.removeModifier(hpMod)
        hp.addModifier(hpMod)
        health = hp.value * percentHP  // Preserve the entity HP percentage when modifying HP
    }

    private fun LivingEntity.copyHpPercentage(entity: LivingEntity) {
        val oldHp = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH) ?: return
        val oldPercentHp = entity.health / oldHp.value
        val newHp = getAttribute(Attribute.GENERIC_MAX_HEALTH) ?: return
        health = newHp.value * oldPercentHp
    }

    /**
     * Add a potion effect to an entity with given duration, automatically gathering all
     * the necessary information and parameters from the abilities.yml file
     *
     * @receiver [LivingEntity] The entity that will get the potion effect applied
     * @param effectType [PotionEffectType] What type of potion will be applied to it
     * @param ability [Ability] What is the ability behind the potion application
     * @param duration [Double] In seconds, the effect desired duration
     * @param amplifier [Int] The amplifier of the potion effect, starting from 0
     * @param isAmbient [Boolean] If isAmbient attribute should be turned on
     * @param emitParticles [Boolean] If potion should have visible particles
     */
    private fun LivingEntity.addPotion(effectType: PotionEffectType, ability: Ability, duration: Double, amplifier: Int = 0, isAmbient: Boolean = abilityConfig.getPotionIsAmbient(ability), emitParticles: Boolean = abilityConfig.getPotionEmitParticles(ability)) {
        // add a temporary potion effect to the living entity
        addPotionEffect(PotionEffect(effectType, (duration * 20.0).toInt().coerceAtLeast(0), amplifier, isAmbient, emitParticles))
    }

    private fun LivingEntity.addPermanentPotion(effectType: PotionEffectType, ability: Ability, amplifier: Int = 0, isAmbient: Boolean = abilityConfig.getPotionIsAmbient(ability), emitParticles: Boolean = abilityConfig.getPotionEmitParticles(ability)) {
        // add a permanent potion effect to the living entity
        addPotionEffect(PotionEffect(effectType, Int.MAX_VALUE, amplifier, isAmbient, emitParticles))
    }

    private fun LivingEntity.doesFly() = this is Bee || this is Parrot

    private fun LivingEntity.getAbilities(): Set<Ability>? = pdc.get(keyChain.abilityListKey, PersistentDataType.STRING)?.toAbilitySet()

    private fun String.toAbilitySet() = gson.fromJson<Set<Ability>>(this, infernalAbilitySetToken)

    fun revertPendingBlockModifications() {
        blockModifications.forEach { it.unmake() }
        blockModifications.clear()
    }

    private companion object {
        val random: ThreadLocalRandom
            get() = ThreadLocalRandom.current()
        val gson = Gson()
        val infernalAbilitySetToken: Type = object : TypeToken<Set<Ability>>() {}.type
        val movSpeedUID: UUID = "57202f4c-2e52-46cb-ad37-77550e99edb2".toUuid()
        val knockbackResistUID: UUID = "984e7a8c-188f-444b-82ea-5d02197ea8e4".toUuid()
        val healthUID: UUID = "18f1d8fb-6fed-4d47-a69b-df5c76693ad5".toUuid()
    }
}

