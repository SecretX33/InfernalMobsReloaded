package com.github.secretx33.infernalmobsreloaded.manager

import com.github.secretx33.infernalmobsreloaded.config.*
import com.github.secretx33.infernalmobsreloaded.events.InfernalDamageDoneEvent
import com.github.secretx33.infernalmobsreloaded.events.InfernalDamageTakenEvent
import com.github.secretx33.infernalmobsreloaded.events.InfernalSpawnEvent
import com.github.secretx33.infernalmobsreloaded.model.Abilities
import com.github.secretx33.infernalmobsreloaded.model.BlockModification
import com.github.secretx33.infernalmobsreloaded.model.InfernalMobType
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.repositories.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.repositories.LootItemsRepo
import com.github.secretx33.infernalmobsreloaded.utils.*
import com.google.common.collect.Sets
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.Block
import org.bukkit.entity.*
import org.bukkit.entity.EntityType
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityTargetEvent.TargetReason
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.material.Colorable
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import org.koin.core.component.KoinApiExtension
import java.lang.StrictMath.pow
import java.lang.reflect.Type
import java.util.*
import kotlin.math.*

@KoinApiExtension
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
){

    private val blockModifications = Sets.newConcurrentHashSet<BlockModification>()
    private val blocksBlackList = Sets.newConcurrentHashSet<Location>()

    fun removeAbilityEffects(entity: LivingEntity) {
        TODO("revert all attributes and chances and convert it back to a normal entity")
    }

    // abilities that are applied when mob spawns

    fun addAbilityEffects(entity: LivingEntity, infernalType: InfernalMobType) {
        val abilitySet = entity.getAbilities() ?: return
        entity.addAbilities(abilitySet)
        entity.multiplyMaxHp(infernalType.getHealthMulti())
    }

    private fun LivingEntity.addAbilities(abilitySet: Set<Abilities>) {
        abilitySet.forEach {
            when(it) {
                Abilities.ARMOURED -> addArmouredAbility()
                Abilities.FLYING -> addFlyingAbility()
                Abilities.HEAVY -> addHeavyAbility()
                Abilities.INVISIBLE -> addInvisibleAbility()
                Abilities.MOLTEN -> addMoltenAbility()
                Abilities.MOUNTED -> addMountedAbility()
                Abilities.SPEEDY -> addSpeedyAbility()
                else -> {}
            }
        }
    }

    private fun LivingEntity.addArmouredAbility() {
        if(equipWithArmor()) return
        // fallback to potion effect if entity cannot wear armor
        addArmouredPotionEffect()
    }

    private fun LivingEntity.addArmouredPotionEffect() = addPermanentPotion(PotionEffectType.DAMAGE_RESISTANCE, Abilities.ARMOURED, amplifier = max(0, abilityConfig.get<Int>(AbilityConfigKeys.ARMOURED_POTION_LEVEL) - 1))

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
        EquipmentSlot.values().forEach { equip.setDropChance(it, dropChance) }

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
        return true
    }

    private fun LivingEntity.canWearArmor() = config.getEnumSet(ConfigKeys.MOB_TYPES_THAT_CAN_WEAR_ARMOR, EntityType::class.java).contains(type)

    private fun LivingEntity.addFlyingAbility() {
        val bat = world.spawn(location, Bat::class.java, SpawnReason.CUSTOM) {
            it.turnIntoMount(keyChain.infernalBatMountKey, false)
            it.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, Int.MAX_VALUE, 0,false, false, false))
            // makes the newly spawned bat goes in a random x z direction, upwards
            it.velocity = Vector(random.nextDouble() * 2 - 1, 1.0, random.nextDouble() * 2 - 1)
            it.isPersistent = true
            it.multiplyMaxHp(3.5)
        }
        bat.addPassenger(this)
        // TODO("Remove bat entity when its 'master' dies")
    }

    private fun LivingEntity.addHeavyAbility() {
        getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.apply {
            val resistAmount = abilityConfig.getDoublePair(AbilityConfigKeys.HEAVY_RESIST_PERCENTAGE).getRandomBetween()
            val mod = AttributeModifier(knockbackResistUID, Abilities.HEAVY.name, resistAmount, AttributeModifier.Operation.ADD_SCALAR)
            removeModifier(mod)
            addModifier(mod)
        }
    }

    private fun LivingEntity.addInvisibleAbility() = addPermanentPotion(PotionEffectType.INVISIBILITY, Abilities.INVISIBLE)

    private fun LivingEntity.addMoltenAbility() = addPermanentPotion(PotionEffectType.FIRE_RESISTANCE, Abilities.MOLTEN)

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
        get() = config.getEnumSet(ConfigKeys.INFERNAL_MOBS_THAT_CAN_BE_RIDED_BY_ANOTHER, EntityType::class.java) { it != null && it.isSpawnable && it.entityClass is LivingEntity && it.entityClass !is ComplexLivingEntity }

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
        addPotionEffect(PotionEffect(PotionEffectType.SPEED, Int.MAX_VALUE, 2, enablePotionParticles, enablePotionParticles, false))
        // give speed potion to make the mount a bit faster
        addPotionEffect(PotionEffect(PotionEffectType.SPEED, Int.MAX_VALUE, 0, enablePotionParticles, enablePotionParticles, false))
        // and mark it as mount in its pdc, so I can check on the spawn event and death event for it
        pdc.set(tag, PersistentDataType.SHORT, 1)
    }

    private fun LivingEntity.addSpeedyAbility() {
        val movSpeed = (if(doesFly()) getAttribute(Attribute.GENERIC_FLYING_SPEED) else getAttribute(Attribute.GENERIC_MOVEMENT_SPEED))
            ?: return
        val speedBonus = abilityConfig.getDoublePair(AbilityConfigKeys.SPEEDY_BONUS).getRandomBetween()
        val mod = AttributeModifier(movSpeedUID, Abilities.SPEEDY.name, speedBonus, AttributeModifier.Operation.ADD_SCALAR)
        movSpeed.removeModifier(mod)
        movSpeed.addModifier(mod)
    }

    // periodic tasks that require a target

    fun startTargetAbilityTasks(entity: LivingEntity, target: LivingEntity): List<Job> {
        val abilities = entity.getAbilities() ?: return emptyList()

        val jobList = ArrayList<Job>()
        abilities.forEach { // TODO("check if I need to pass multimap as parameter to these tasks, cause I'm not sure if the remove will even be called")
            when(it) {
                Abilities.ARCHER -> makeArcherTask(entity, target)
                Abilities.CALL_THE_GANG -> makeCallTheGangTask(entity, target)
                Abilities.GHASTLY -> makeGhastlyTask(entity, target)
                Abilities.MORPH -> makeMorphTask(entity, target)
                Abilities.NECROMANCER -> makeNecromancerTask(entity, target)
                Abilities.POTIONS -> null // TODO("Make a potion throw task")
                Abilities.TELEPORT -> makeTeleportTask(entity, target)
                Abilities.THIEF -> makeThiefTask(entity, target)
                Abilities.WEBBER -> makeWebberTask(entity, target)
                else -> null
            }?.let { job -> jobList.add(job) }
        }
        return jobList
    }

    private fun makeArcherTask(entity: LivingEntity, target: LivingEntity) = CoroutineScope(Dispatchers.Default).launch {
        val nearbyRange = abilityConfig.getNearbyRange(Abilities.ARCHER, 4.0)
        val speed = abilityConfig.getProjectileSpeed(Abilities.ARCHER, 2.2)
        val delay = abilityConfig.getDouble(AbilityConfigKeys.ARCHER_ARROW_DELAY, minValue = 0.001).toLongDelay()
        val recheckDelay = abilityConfig.getRecheckDelay(Abilities.ARCHER, 1.0).toLongDelay()
        val chance = abilityConfig.getAbilityChance(Abilities.ARCHER, 0.05)

        while(isActive && !entity.isNotTargeting(target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance) continue
            val amount = abilityConfig.getIntPair(AbilityConfigKeys.ARCHER_ARROW_AMOUNT, minValue = 1).getRandomBetween()
            val victims = target.getValidNearbyTargetsAsync(nearbyRange)

            for (i in 1..amount) {
                victims.forEach {
                    if(!isActive || entity.isNotTargeting(target)) return@launch
                    val dir = entity.shootDirection(it).multiply(speed)
                    entity.shootProjectile(dir, Arrow::class.java)
                }
                delay(delay)
            }
        }
    }

    private fun makeCallTheGangTask(entity: LivingEntity, target: LivingEntity) = CoroutineScope(Dispatchers.Default).launch {
        val recheckDelay = abilityConfig.getRecheckDelay(Abilities.CALL_THE_GANG, 2.0).toLongDelay()
        val chance = abilityConfig.getAbilityChance(Abilities.CALL_THE_GANG, 0.025)

        while(isActive && !entity.isNotTargeting(target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance) continue
            val amount = abilityConfig.getIntAmounts(Abilities.CALL_THE_GANG, 2).getRandomBetween()
            val potency = abilityConfig.getAbilityPotency(Abilities.CALL_THE_GANG, 3).getRandomBetween()
            val potionDuration = abilityConfig.getDuration(Abilities.CALL_THE_GANG, 8.0).getRandomBetween()

            runSync(plugin) {
                repeat(amount) {
                    entity.world.spawnEntity(entity.location, entity.type, SpawnReason.CUSTOM) {
                        (it as? Mob)?.target = target
                        (it as? Ageable)?.setBaby()
                        (it as? LivingEntity)?.addPotion(PotionEffectType.SPEED, Abilities.CALL_THE_GANG, amplifier = potency, duration = potionDuration)
                    }
                }
                particlesHelper.sendParticle(entity, Particle.TOTEM, entity.width + 1, 30)
            }
        }
    }

    private fun makeGhastlyTask(entity: LivingEntity, target: LivingEntity) = CoroutineScope(Dispatchers.Default).launch {
        val nearbyRange = abilityConfig.getNearbyRange(Abilities.ARCHER, 4.0)
        val recheckDelay = abilityConfig.getRecheckDelay(Abilities.GHASTLY, 1.5).toLongDelay()
        val chance = abilityConfig.getAbilityChance(Abilities.GHASTLY, 0.25)
        val speed = abilityConfig.getProjectileSpeed(Abilities.GHASTLY, 1.5)

        while(isActive && !entity.isNotTargeting(target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance) continue

            val victims = target.getValidNearbyTargetsAsync(nearbyRange)
            victims.forEach {
                val dir = entity.shootDirection(it).multiply(speed)
                entity.shootProjectile(dir, Fireball::class.java)
            }
        }
    }

    private fun makeMorphTask(entity: LivingEntity, target: LivingEntity) = CoroutineScope(Dispatchers.Default).launch {
        val recheckDelay = abilityConfig.getRecheckDelay(Abilities.MORPH, 1.0).toLongDelay()
        val chance = abilityConfig.getAbilityChance(Abilities.MORPH, 0.01)

        while(isActive && !entity.isNotTargeting(target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance) continue
            val newType = infernalMobTypesRepo.getRandomInfernalType()
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
        val nearbyRange = abilityConfig.getNearbyRange(Abilities.GHASTLY, 4.0)
        val recheckDelay = abilityConfig.getRecheckDelay(Abilities.GHASTLY, 2.5).toLongDelay()
        val chance = abilityConfig.getAbilityChance(Abilities.GHASTLY, 0.25)
        val speed = abilityConfig.getProjectileSpeed(Abilities.GHASTLY, 2.0)

        while(isActive && !entity.isNotTargeting(target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance) continue

            val victims = target.getValidNearbyTargetsAsync(nearbyRange)
            victims.forEach {
                val dir = entity.shootDirection(it).multiply(speed)
                entity.shootProjectile(dir, WitherSkull::class.java)
            }
        }
    }

    private fun makeTeleportTask(entity: LivingEntity, target: LivingEntity) = CoroutineScope(Dispatchers.Default).launch {
        val recheckDelay = abilityConfig.getRecheckDelay(Abilities.TELEPORT, 2.5).toLongDelay()
        val chance = abilityConfig.getAbilityChance(Abilities.TELEPORT, 0.3)

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
                continue
            }
            // else just teleport the entity right on the target's feet
            entity.teleportAsync(target.location)
        }
    }

    private fun makeThiefTask(entity: LivingEntity, target: LivingEntity) = CoroutineScope(Dispatchers.Default).launch {
        // if thief ability should affect only players and target is not one, return
        val affectOnlyPlayers = abilityConfig.getAffectsOnlyPlayers(Abilities.THIEF, true)
        if(affectOnlyPlayers && target.type != EntityType.PLAYER || target.equipment == null) return@launch

        val recheckDelay = abilityConfig.getRecheckDelay(Abilities.THIEF, 3.0).toLongDelay()
        val chance = abilityConfig.getAbilityChance(Abilities.THIEF, 0.05)
        val requireLoS = abilityConfig.doesRequireLineOfSight(Abilities.THIEF)
        val sendMessage = abilityConfig.getSendMessage(Abilities.THIEF)

        while(isActive && !entity.isNotTargeting(target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance || (requireLoS && !entity.hasLineOfSight(target))) continue

            val equip = target.equipment ?: return@launch
            // slot chosen to have its equipment stolen, skipping this interaction if there's none (and yeah EntityEquipment#getItem returns null even if its annotated with @NonNull)
            val chosenSlot = EquipmentSlot.values().filter { slot -> equip.getItem(slot).let { it != null && !it.isAir() } }.randomOrNull() ?: continue
            val durabilityLoss = abilityConfig.getDurabilityLoss(Abilities.THIEF, 0.04).getRandomBetween()
            val item = equip.getItem(chosenSlot)
            val damagedItem = item.damageItemBy(durabilityLoss)

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

    private fun makeWebberTask(entity: LivingEntity, target: LivingEntity) = CoroutineScope(Dispatchers.Default).launch {
        val chance = abilityConfig.getAbilityChance(Abilities.WEBBER, 0.05)
        val recheckDelay = abilityConfig.getRecheckDelay(Abilities.WEBBER, 2.0).toLongDelay()

        while(isActive && !entity.isNotTargeting(target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance) continue
            launchCobweb(target)
        }
    }

    private fun launchCobweb(target: LivingEntity) {
        val trapDensity = abilityConfig.getDouble(AbilityConfigKeys.WEBBER_TRAP_DENSITY, maxValue = 1.0)
        val duration = abilityConfig.getDuration(Abilities.WEBBER, 5.0, minValue = 0.1).getRandomBetween().toLongDelay()
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

    private fun Block.canMobGrief(): Boolean = type != Material.BEDROCK && !blocksBlackList.contains(location) && wgChecker.canMobGriefBlock(this)

    private fun LivingEntity.makeCuboidAround(): Cuboid {
        val lowerBound = location.apply {
            x -= ceil(width)
            y -= 1
            z -= ceil(width)
        }
        val upperBound = location.apply {
            x += ceil(width) + 2
            y += ceil(height) + 1
            z += ceil(width) + 2
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
            y += target.height + 0.1
        }
        val difX = dest.x - src.x
        val difZ = dest.z - src.z
        val difY = dest.y - src.y
        val difXZ = sqrt(pow(difX, 2.0) + pow(difZ, 2.0))
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
                Abilities.GHOST -> entity.triggerGhost()
                Abilities.KAMIKAZE -> entity.triggerKamizake()
                else -> {}
            }
        }
    }

    private fun LivingEntity.triggerGhost() {
        val evil = random.nextDouble() <= abilityConfig.getDouble(AbilityConfigKeys.GHOST_EVIL_CHANCE)
        val evilPrefix = if(evil) "evil_" else ""
        val itemDropChance = abilityConfig.getDouble(AbilityConfigKeys.GHOST_ITEM_DROP_CHANCE, maxValue = 1.0).toFloat()

        val abilitySet = if(evil) setOf(Abilities.BLINDING, Abilities.NECROMANCER, Abilities.WITHERING) else setOf(Abilities.CONFUSION, Abilities.GHASTLY, Abilities.HUNGER)

        val ghost = world.spawn(location, Zombie::class.java, SpawnReason.CUSTOM) {
            it.addPermanentPotion(PotionEffectType.INVISIBILITY, Abilities.GHOST, isAmbient = true, emitParticles = true)
            it.canPickupItems = false

            val equip = it.equipment
            EquipmentSlot.values().forEach { slot -> equip?.setDropChance(slot, itemDropChance) }

            equip?.apply {
                helmet = lootItemsRepo.getLootItemOrNull("${evilPrefix}ghost_helmet")?.makeItem()
                chestplate = lootItemsRepo.getLootItemOrNull("${evilPrefix}ghost_chestplate")?.makeItem()
                leggings = lootItemsRepo.getLootItemOrNull("${evilPrefix}ghost_leggings")?.makeItem()
                boots = lootItemsRepo.getLootItemOrNull("${evilPrefix}ghost_boots")?.makeItem()
                setItemInMainHand(lootItemsRepo.getLootItemOrNull("${evilPrefix}ghost_weapon")?.makeItem())
            }
            it.addAbilities(abilitySet)
        }

        if(evil) particlesHelper.sendParticle(ghost, Particle.SMOKE_LARGE, 3.5, 60)
        else particlesHelper.sendParticle(ghost, Particle.CLOUD, 2.0, 25)
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
                Abilities.BERSERK -> event.triggerBerserk()
                Abilities.BLINDING -> event.triggerBlinding()
                Abilities.CONFUSION -> event.triggerConfusion()
                Abilities.HUNGER -> event.triggerHunger()
                Abilities.LEVITATE -> event.triggerLevitate()
                Abilities.LIFESTEAL -> event.triggerLifesteal()
                Abilities.LIGHTNING -> event.triggerLightning()
                Abilities.MOLTEN -> event.triggerMolten()
                Abilities.POISONOUS -> event.triggerPoisonous()
                Abilities.SLOWNESS -> event.triggerSlowness()
                Abilities.RUST -> event.triggerRust()
                Abilities.TOSSER -> event.triggerTosser()
                Abilities.WEAKNESS -> event.triggerWeakness()
                Abilities.WITHERING -> event.triggerWithering()
                else -> {}
            }
        }
    }

    private fun InfernalDamageDoneEvent.triggerBerserk() {
        val bonus = abilityConfig.getDoublePair(AbilityConfigKeys.BERSERK_CAUSED_DAMAGE_BONUS)
        damageMulti = bonus.getRandomBetween()
    }

    private fun InfernalDamageDoneEvent.triggerBlinding() {
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Abilities.BLINDING, 0.75)
        if(random.nextDouble() > chance) return

        // blinds the defender for some time
        val duration = abilityConfig.getDuration(Abilities.BLINDING, 7.0).getRandomBetween()
        defender.addPotion(PotionEffectType.BLINDNESS, Abilities.BLINDING, duration)
    }

    private fun InfernalDamageDoneEvent.triggerConfusion() {
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Abilities.CONFUSION, 0.4)
        if(random.nextDouble() > chance) return

        // makes the defender nauseated for some time
        val duration = abilityConfig.getDuration(Abilities.CONFUSION, 8.0).getRandomBetween()
        defender.addPotion(PotionEffectType.CONFUSION, Abilities.CONFUSION, duration)
    }

    private fun InfernalDamageDoneEvent.triggerHunger() {
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Abilities.HUNGER, 0.7)
        if(random.nextDouble() > chance) return

        // makes the defender hunger for some time
        val potency = max(0, abilityConfig.getAbilityPotency(Abilities.HUNGER, 8).getRandomBetween() - 1)
        val duration = abilityConfig.getDuration(Abilities.HUNGER, 30.0).getRandomBetween()
        defender.addPotion(PotionEffectType.HUNGER, Abilities.HUNGER, duration, amplifier = potency)
    }

    private fun InfernalDamageDoneEvent.triggerLevitate() {
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Abilities.LEVITATE, 0.4)
        if(random.nextDouble() > chance) return

        // makes the defender levitate for some time
        val potency = max(0, abilityConfig.getAbilityPotency(Abilities.LEVITATE, 6).getRandomBetween() - 1)
        val duration = abilityConfig.getDuration(Abilities.LEVITATE, 6.0).getRandomBetween()
        defender.addPotion(PotionEffectType.LEVITATION, Abilities.LEVITATE, duration, amplifier = potency)
    }

    private fun InfernalDamageDoneEvent.triggerLightning() {
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Abilities.LIGHTNING, 0.25)
        if(random.nextDouble() > chance) return

        val lightning = world.strikeLightning(defender.location)
        lightning.pdc.set(keyChain.lightningOwnerUuidKey, PersistentDataType.STRING, entity.uniqueId.toString())
    }

    private fun InfernalDamageDoneEvent.triggerLifesteal() {
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Abilities.LIFESTEAL, 0.75)
        if(random.nextDouble() > chance) return

        val healingAmount = abilityConfig.getDoublePair(AbilityConfigKeys.LIFESTEAL_HEALING_PERCENTAGE).getRandomBetween()
        entity.heal(damage * healingAmount)
        // TODO("Add particle effects to visually indicate the healing effect occurring")
    }

    private fun LivingEntity.heal(healingAmount: Double) {
        val maxHp = getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: return
        health = min(maxHp, health + healingAmount)
    }

    private fun InfernalDamageDoneEvent.triggerMolten() {
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Abilities.MOLTEN, 0.6)
        if(random.nextDouble() > chance) return

        // sets the attacker on defender
        val duration = abilityConfig.getDuration(Abilities.MOLTEN, 8.0).getRandomBetween()
        defender.fireTicks = (duration * 20.0).toInt()
    }

    private fun InfernalDamageDoneEvent.triggerPoisonous() {
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Abilities.POISONOUS, 0.8)
        if(random.nextDouble() > chance) return

        // poisons the defender
        val potency = max(0, abilityConfig.getAbilityPotency(Abilities.POISONOUS, 6).getRandomBetween() - 1)
        val duration = abilityConfig.getDuration(Abilities.POISONOUS, 8.0).getRandomBetween()
        defender.addPotion(PotionEffectType.POISON, Abilities.POISONOUS, duration, amplifier = potency)
    }

    private fun InfernalDamageDoneEvent.triggerRust() {
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Abilities.RUST, 0.6)
        if(random.nextDouble() > chance) return
        val mhDurabilityLoss = abilityConfig.getDurabilityLoss(Abilities.RUST, 0.15).getRandomBetween()
        val ohDurabilityLoss = abilityConfig.getDurabilityLoss(Abilities.RUST, 0.15).getRandomBetween()
        val sendMessage = abilityConfig.getSendMessage(Abilities.RUST)

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
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Abilities.SLOWNESS, 0.7)
        if(random.nextDouble() > chance) return

        // gives slow effect to the defender
        val potency = max(0, abilityConfig.getAbilityPotency(Abilities.SLOWNESS, 3).getRandomBetween() - 1)
        val duration = abilityConfig.getDuration(Abilities.SLOWNESS, 6.0).getRandomBetween()
        defender.addPotion(PotionEffectType.WEAKNESS, Abilities.WEAKNESS, duration, amplifier = potency)
    }

    private fun InfernalDamageDoneEvent.triggerTosser() {
        val nearbyRange = abilityConfig.getNearbyRange(Abilities.TOSSER, 4.0)
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Abilities.TOSSER, 0.4)
        if(random.nextDouble() > chance) return

        val victims = entity.getValidNearbyTargets(nearbyRange)

        // toss victim and all nearby entities
        victims.forEach {
            val x = abilityConfig.getDistanceMultiplier(Abilities.TOSSER) * (random.nextDouble() * 2 - 1)
            val y = abilityConfig.getHeightMultiplier(Abilities.TOSSER) * random.nextDouble()
            val z = abilityConfig.getDistanceMultiplier(Abilities.TOSSER) * (random.nextDouble() * 2 - 1)
            it.velocity = Vector(x, y, z)
        }
    }

    private fun InfernalDamageDoneEvent.triggerWeakness() {
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Abilities.WEAKNESS, 0.5)
        if(random.nextDouble() > chance) return

        // gives weakness effect to the defender
        val potency = max(0, abilityConfig.getAbilityPotency(Abilities.WEAKNESS, 1, minValue = 1).getRandomBetween() - 1)
        val duration = abilityConfig.getDuration(Abilities.WEAKNESS, 6.0).getRandomBetween()
        defender.addPotion(PotionEffectType.WEAKNESS, Abilities.WEAKNESS, duration, amplifier = potency)
    }

    private fun InfernalDamageDoneEvent.triggerWithering() {
        val chance = abilityConfig.getAbilityChanceOnDamageDone(Abilities.WITHERING, 0.7)
        if(random.nextDouble() > chance) return

        // gives wither effect to the defender
        val potency = max(0, abilityConfig.getAbilityPotency(Abilities.WITHERING, 3).getRandomBetween() - 1)
        val duration = abilityConfig.getDuration(Abilities.WITHERING, 6.0).getRandomBetween()
        defender.addPotion(PotionEffectType.WITHER, Abilities.WITHERING, duration, amplifier = potency)
    }

    // abilities that are triggered when an infernal takes damage

    fun triggerOnDamageTakenAbilities(event: InfernalDamageTakenEvent) {
        val abilities = event.entity.getAbilities() ?: return

        abilities.forEach {
            when(it) {
                Abilities.BERSERK -> event.triggerBerserk()
                Abilities.FIREWORK -> event.triggerFirework()
                Abilities.MOLTEN -> event.triggerMolten()
                Abilities.POISONOUS -> event.triggerPoisonous()
                Abilities.THORNMAIL -> event.triggerThornmail()
                else -> {}
            }
        }
    }

    private fun InfernalDamageTakenEvent.triggerBerserk() {
        val bonus = abilityConfig.getDoublePair(AbilityConfigKeys.BERSERK_RECEIVED_DAMAGE_BONUS)
        damageMulti = bonus.getRandomBetween()
    }

    private fun InfernalDamageTakenEvent.triggerFirework() {
        val chance = abilityConfig.getAbilityChanceOnDamageTaken(Abilities.FIREWORK, 0.25)
        println("Firework chance = $chance")
        if(random.nextDouble() > chance) return

        world.spawn(attacker.location.apply {
            x += random.nextDouble() - 0.5
            y += random.nextDouble()
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
        val chance = abilityConfig.getAbilityChanceOnDamageTaken(Abilities.MOLTEN, 0.4)
        if(random.nextDouble() > chance) return

        // sets the attacker on fire
        val duration = abilityConfig.getDuration(Abilities.MOLTEN, 8.0).getRandomBetween()
        attacker.fireTicks = (duration * 20.0).toInt()
    }

    private fun InfernalDamageTakenEvent.triggerPoisonous() {
        if(cause.isNotMelee()) return
        val chance = abilityConfig.getAbilityChanceOnDamageTaken(Abilities.POISONOUS, 0.2)
        if(random.nextDouble() > chance) return

        // poisons the attacker
        val potency = max(0, abilityConfig.getAbilityPotency(Abilities.POISONOUS, 6).getRandomBetween() - 1)
        val duration = abilityConfig.getDuration(Abilities.POISONOUS, 8.0).getRandomBetween()
        attacker.addPotion(PotionEffectType.POISON, Abilities.POISONOUS, duration, amplifier = potency)
    }

    private fun InfernalDamageTakenEvent.triggerThornmail() {
        val chance = abilityConfig.getAbilityChance(Abilities.THORNMAIL, 0.6)
        if(random.nextDouble() > chance) return

        // reflect part of the damage to the attacker
        val reflectPercentage = abilityConfig.getDoublePair(AbilityConfigKeys.THORMAIL_REFLECTED_AMOUNT).getRandomBetween()
        particlesHelper.sendParticle(entity, Abilities.THORNMAIL)
        attacker.damage(damage * reflectPercentage, entity)
    }

    private fun EntityDamageEvent.DamageCause.isNotMelee() = this != EntityDamageEvent.DamageCause.ENTITY_ATTACK && this != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK && this != EntityDamageEvent.DamageCause.CONTACT && this != EntityDamageEvent.DamageCause.CRAMMING && this != EntityDamageEvent.DamageCause.THORNS

    // utility functions

    private fun Double.toLongDelay() = (this * 1000.0).toLong()

    private fun Pair<Int, Int>.getRandomBetween(): Int {
        val (minValue, maxValue) = this
        return random.nextInt(maxValue - minValue + 1) + minValue
    }

    private fun Pair<Double, Double>.getRandomBetween(): Double {
        val (minValue, maxValue) = this
        return minValue + (maxValue - minValue) * random.nextDouble()
    }

    private fun LivingEntity.getValidNearbyTargets(range: Double) = location.getNearbyLivingEntities(range) { !it.isDead && it.isValid }

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
     * @param ability [Abilities] What is the ability behind the potion application
     * @param duration [Double] In seconds, the effect desired duration
     * @param amplifier [Int] The amplifier of the potion effect, starting from 0
     * @param isAmbient [Boolean] If isAmbient attribute should be turned on
     * @param emitParticles [Boolean] If potion should have visible particles
     */
    private fun LivingEntity.addPotion(effectType: PotionEffectType, ability: Abilities, duration: Double, amplifier: Int = 0, isAmbient: Boolean = abilityConfig.getPotionIsAmbient(ability), emitParticles: Boolean = abilityConfig.getPotionEmitParticles(ability)) {
        // add a temporary potion effect to the living entity
        addPotionEffect(PotionEffect(effectType, (duration * 20.0).toInt(), amplifier, isAmbient, emitParticles))
    }

    private fun LivingEntity.addPermanentPotion(effectType: PotionEffectType, ability: Abilities, amplifier: Int = 0, isAmbient: Boolean = abilityConfig.getPotionIsAmbient(ability), emitParticles: Boolean = abilityConfig.getPotionEmitParticles(ability)) {
        // add a permanent potion effect to the living entity
        addPotionEffect(PotionEffect(effectType, Int.MAX_VALUE, amplifier, isAmbient, emitParticles))
    }

    private fun LivingEntity.doesFly() = this is Bee || this is Parrot

    private fun LivingEntity.getAbilities(): Set<Abilities>? = pdc.get(keyChain.abilityListKey, PersistentDataType.STRING)?.toAbilitySet()

    private fun String.toAbilitySet() = gson.fromJson<Set<Abilities>>(this, infernalAbilitySetToken)

    fun revertPendingBlockModifications() {
        blockModifications.forEach { it.unmake() }
        blockModifications.clear()
    }

    private companion object {
        val random = Random()
        val gson = Gson()
        val infernalAbilitySetToken: Type = object : TypeToken<Set<Abilities>>() {}.type
        val movSpeedUID: UUID = "57202f4c-2e52-46cb-ad37-77550e99edb2".toUuid()
        val knockbackResistUID: UUID = "984e7a8c-188f-444b-82ea-5d02197ea8e4".toUuid()
        val healthUID: UUID = "18f1d8fb-6fed-4d47-a69b-df5c76693ad5".toUuid()
    }
}
