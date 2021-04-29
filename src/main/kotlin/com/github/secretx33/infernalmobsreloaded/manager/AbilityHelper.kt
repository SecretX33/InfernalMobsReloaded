package com.github.secretx33.infernalmobsreloaded.manager

import com.github.secretx33.infernalmobsreloaded.config.AbilityConfig
import com.github.secretx33.infernalmobsreloaded.config.AbilityConfigKeys
import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.model.Abilities
import com.github.secretx33.infernalmobsreloaded.model.BlockModification
import com.github.secretx33.infernalmobsreloaded.model.InfernalMobType
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.repositories.InfernalMobTypesRepo
import com.github.secretx33.infernalmobsreloaded.repositories.LootItemsRepo
import com.github.secretx33.infernalmobsreloaded.utils.Cuboid
import com.github.secretx33.infernalmobsreloaded.utils.pdc
import com.github.secretx33.infernalmobsreloaded.utils.runSync
import com.github.secretx33.infernalmobsreloaded.utils.toUuid
import com.google.common.collect.Multimap
import com.google.common.collect.Sets
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.Block
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.*
import org.bukkit.entity.EntityType
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
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

    fun addAbilityEffects(entity: LivingEntity, infernalType: InfernalMobType) {
        val abilityList = entity.getAbilities() ?: return
        abilityList.forEach {
            when(it) {
                Abilities.ARMOURED -> entity.addArmouredAbility()
                Abilities.FLYING -> entity.addFlyingAbility()
                Abilities.HEAVY -> entity.addHeavyAbility()
                Abilities.INVISIBLE -> entity.addInvisibleAbility()
                Abilities.MOLTEN -> entity.addMoltenAbility()
                Abilities.MOUNTED -> entity.addMountedAbility()
                Abilities.SPEEDY -> entity.addSpeedyAbility()
                else -> {}
            }
        }
        entity.multiplyMaxHp(infernalType.getHealthMulti())
    }

    private fun LivingEntity.addArmouredAbility() {
        if(equipWithArmor()) return
        // fallback to potion effect if entity cannot wear armor
        addArmouredPotionEffect()
    }

    private fun LivingEntity.addArmouredPotionEffect() {
        addPotionEffect(PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Int.MAX_VALUE, max(0, abilityConfig.get<Int>(AbilityConfigKeys.ARMOURED_POTION_LEVEL) - 1), false, false, false))
    }

    /**
     * Tries to equip default armor on entity, returning false if it cannot wear armor.
     *
     * @receiver LivingEntity the entity to equip armor onto
     * @return Boolean returns true if entity was successfully equipped with armor
     */
    private fun LivingEntity.equipWithArmor(): Boolean {
        val equip = equipment ?: return false
        EquipmentSlot.values().forEach { equip.setDropChance(it, 0f) }
        equip.apply {
            helmet = lootItemsRepo.getLootItem("armoured_helmet").makeItem()
            chestplate = lootItemsRepo.getLootItem("armoured_chestplate").makeItem()
            leggings = lootItemsRepo.getLootItem("armoured_leggings").makeItem()
            boots =  lootItemsRepo.getLootItem("armoured_boots").makeItem()
            when(itemInMainHand.type) {
                Material.BOW -> lootItemsRepo.getLootItem("armoured_bow").makeItem()
                Material.CROSSBOW -> lootItemsRepo.getLootItem("armoured_crossbow").makeItem()
                else -> lootItemsRepo.getLootItem("armoured_sword").makeItem()
            }
        }
        return true
    }

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
        addArmouredPotionEffect()
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

    private fun LivingEntity.addPermanentPotion(effectType: PotionEffectType, ability: Abilities, amplifier: Int = 0, isAmbient: Boolean = abilityConfig.getPotionIsAmbient(ability), emitParticles: Boolean = abilityConfig.getPotionEmitParticles(ability)) {
        addPotionEffect(PotionEffect(effectType, Int.MAX_VALUE, amplifier, isAmbient, emitParticles))
    }

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
        }

        if(evil) particlesHelper.sendParticle(ghost, Particle.SMOKE_LARGE, 2.25, 50)
        else particlesHelper.sendParticle(ghost, Particle.CLOUD, 1.25, 25)

        if (evil) {
            aList.add("necromancer")
            aList.add("withering")
            aList.add("blinding")
        } else {
            aList.add("ghastly")
            aList.add("sapper")
            aList.add("confusing")
        }
        val newMob: InfernalMob
        if (evil) {
            newMob = InfernalMob(g, g.uniqueId, false, aList, 1, "smoke:2:12")
        } else {
            newMob = InfernalMob(g, g.uniqueId, false, aList, 1, "cloud:0:8")
        }
        this.infernalList.add(newMob)
    }


    private fun LivingEntity.triggerKamizake() {
        TODO("Not yet implemented")
    }


    fun startTargetTasks(entity: LivingEntity, target: LivingEntity, multimap: Multimap<UUID, Job>) {
        val abilities = entity.getAbilities() ?: return

        val jobList = ArrayList<Job>()
        abilities.forEach { // TODO("check if I need to pass multimap as parameter to these tasks, cause I'm not sure if the remove will even be called")
            when(it) {
                Abilities.ARCHER -> makeArcherTask(entity, target, multimap)
                Abilities.CALL_THE_GANG -> makeCallTheGangTask(entity, target, multimap)
                Abilities.GHASTLY -> makeGhastlyTask(entity, target, multimap)
                Abilities.MORPH -> makeMorphTask(entity, target, multimap)
                Abilities.NECROMANCER -> makeNecromancerTask(entity, target, multimap)
                Abilities.POTIONS -> TODO()
                Abilities.THIEF -> makeThiefTask(entity, target, multimap)
                Abilities.WEBBER -> makeWebberTask(entity, target, multimap)
                else -> null
            }?.let { job -> jobList.add(job) }
        }
        multimap.putAll(entity.uniqueId, jobList.filter { it.isActive })
    }

    private fun makeMorphTask(entity: LivingEntity, target: LivingEntity, multimap: Multimap<UUID, Job>) = CoroutineScope(Dispatchers.Default).launch {
        val recheckDelay = abilityConfig.getRecheckDelay(Abilities.MORPH, 1.0).toLongDelay()
        val chance = abilityConfig.getAbilityChance(Abilities.MORPH, 0.01)

        while(isActive && !isInvalid(entity, target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance) continue
            val newType = infernalMobTypesRepo.getRandomInfernalType()
            val keepHpPercent = abilityConfig.get<Boolean>(AbilityConfigKeys.MORPH_KEEP_HP_PERCENTAGE)
            entity.remove()
            entity.world.spawn(entity.location, newType.entityClass, SpawnReason.CUSTOM)
        }
        multimap.remove(entity.uniqueId, coroutineContext.job)
    }

    private fun makeArcherTask(entity: LivingEntity, target: LivingEntity, multimap: Multimap<UUID, Job>) = CoroutineScope(Dispatchers.Default).launch {
        val speed = abilityConfig.getProjectileSpeed(Abilities.ARCHER, 2.2)
        val amount = abilityConfig.getIntPair(AbilityConfigKeys.ARCHER_ARROW_AMOUNT, minValue = 1).getRandomBetween()
        val delay = abilityConfig.getDouble(AbilityConfigKeys.ARCHER_ARROW_DELAY, minValue = 0.001).toLongDelay()
        val recheckDelay = abilityConfig.getRecheckDelay(Abilities.ARCHER, 1.0).toLongDelay()
        val chance = abilityConfig.getAbilityChance(Abilities.ARCHER, 0.04)

        while(isActive && !isInvalid(entity, target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance) continue

            for (i in 1..amount) {
                val dir = entity.shootDirection()?.multiply(speed)
                if (!isActive || dir == null || isInvalid(entity, target)) {
                    multimap.remove(entity.uniqueId, coroutineContext.job)
                    return@launch
                }
                entity.shootProjectile(dir, Arrow::class.java)
                delay(delay)
            }
        }
        multimap.remove(entity.uniqueId, coroutineContext.job)
    }

    private fun makeCallTheGangTask(entity: LivingEntity, target: LivingEntity, multimap: Multimap<UUID, Job>) = CoroutineScope(Dispatchers.Default).launch {
        val recheckDelay = abilityConfig.getRecheckDelay(Abilities.CALL_THE_GANG, 2.0).toLongDelay()
        val chance = abilityConfig.getAbilityChance(Abilities.CALL_THE_GANG, 0.025)
        val amount = abilityConfig.getIntAmounts(Abilities.CALL_THE_GANG, 2).getRandomBetween()

        while(isActive && !isInvalid(entity, target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance) continue
            runSync(plugin) {
                repeat(amount) {
                    entity.world.spawnEntity(entity.location, entity.type, SpawnReason.CUSTOM) {
                        (it as? Mob)?.target = target
                        (it as? LivingEntity)?.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 12, 1, false, true, false))
                    }
                }
                particlesHelper.sendParticle(entity, Particle.TOTEM, entity.width + 1, 30)
            }
        }
        multimap.remove(entity.uniqueId, coroutineContext.job)
    }

    private fun makeGhastlyTask(entity: LivingEntity, target: LivingEntity, multimap: Multimap<UUID, Job>) = CoroutineScope(Dispatchers.Default).launch {
        val recheckDelay = abilityConfig.getRecheckDelay(Abilities.GHASTLY, 1.5).toLongDelay()
        val chance = abilityConfig.getAbilityChance(Abilities.GHASTLY, 0.25)
        val speed = abilityConfig.getProjectileSpeed(Abilities.GHASTLY, 1.5)

        while(isActive && !isInvalid(entity, target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance) continue

            val dir = entity.shootDirection()?.multiply(speed) ?: run {
                multimap.remove(entity.uniqueId, coroutineContext.job)
                return@launch
            }
            entity.shootProjectile(dir, Fireball::class.java)
        }
        multimap.remove(entity.uniqueId, coroutineContext.job)
    }

    private fun makeNecromancerTask(entity: LivingEntity, target: LivingEntity, multimap: Multimap<UUID, Job>) = CoroutineScope(Dispatchers.Default).launch {
        val recheckDelay = abilityConfig.getRecheckDelay(Abilities.GHASTLY, 2.5).toLongDelay()
        val chance = abilityConfig.getAbilityChance(Abilities.GHASTLY, 0.25)
        val speed = abilityConfig.getProjectileSpeed(Abilities.GHASTLY, 2.0)

        while(isActive && !isInvalid(entity, target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance) continue

            val dir = entity.shootDirection()?.multiply(speed) ?: run {
                multimap.remove(entity.uniqueId, coroutineContext.job)
                return@launch
            }
            entity.shootProjectile(dir, WitherSkull::class.java)
        }
        multimap.remove(entity.uniqueId, coroutineContext.job)
    }

    private fun makeThiefTask(entity: LivingEntity, target: LivingEntity, multimap: Multimap<UUID, Job>) = CoroutineScope(Dispatchers.Default).launch {
        val recheckDelay = abilityConfig.getRecheckDelay(Abilities.ARCHER, 4.0).toLongDelay()
        val chance = abilityConfig.getAbilityChance(Abilities.ARCHER, 0.05)

        while(isActive && !isInvalid(entity, target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance) continue

            val item = target.equipment?.itemInMainHand?.takeUnless { it.type.isAir } ?: continue
            target.equipment?.setItemInMainHand(ItemStack(Material.AIR))
            entity.world.dropItemNaturally(entity.location, item)
            (target as? Player)?.updateInventory()
        }
        multimap.remove(entity.uniqueId, coroutineContext.job)
    }

    private fun makeWebberTask(entity: LivingEntity, target: LivingEntity, multimap: Multimap<UUID, Job>) = CoroutineScope(Dispatchers.Default).launch {
        val chance = abilityConfig.getAbilityChance(Abilities.WEBBER, 0.05)
        val recheckDelay = abilityConfig.getRecheckDelay(Abilities.WEBBER, 2.0).toLongDelay()

        while(isActive && !isInvalid(entity, target)) {
            delay(recheckDelay)
            if(random.nextDouble() > chance) continue
            launchCobweb(target)
        }
        multimap.remove(entity.uniqueId, coroutineContext.job)
    }

    private fun launchCobweb(target: LivingEntity) {
        val trapDensity = abilityConfig.getDouble(AbilityConfigKeys.WEBBER_TRAP_DENSITY, maxValue = 1.0)
        val duration = abilityConfig.getDuration(Abilities.WEBBER, 5.0, minValue = 0.1).toLongDelay()
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
        val src = eyeLocation.apply { y -= height / 12 }
        val dest = target.location.apply {
            y += target.height * 0.75
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

    private fun isInvalid(entity: LivingEntity, target: LivingEntity) = entity.isDead || !entity.isValid  || target.isDead || !target.isValid || (entity is Mob && entity.target?.uniqueId != target.uniqueId)

    private fun Double.toLongDelay() = (this * 1000.0).toLong()

    private fun Pair<Int, Int>.getRandomBetween(): Int {
        val (minValue, maxValue) = this
        return random.nextInt(maxValue - minValue) + minValue
    }

    private fun Pair<Double, Double>.getRandomBetween(): Double {
        val (minValue, maxValue) = this
        return minValue + (maxValue - minValue) * random.nextDouble()
    }

    private fun LivingEntity.multiplyMaxHp(percentage: Double) {
        val hp = getAttribute(Attribute.GENERIC_MAX_HEALTH) ?: return
        val hpMod = AttributeModifier(healthUID, "health_multi", percentage, AttributeModifier.Operation.ADD_SCALAR)
        val percentHP = health / hp.value
        hp.removeModifier(hpMod)
        hp.addModifier(hpMod)
        health = hp.value * percentHP // Preserve the entity HP percentage when modifying HP
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
