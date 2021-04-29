package com.github.secretx33.infernalmobsreloaded.manager

import com.github.secretx33.infernalmobsreloaded.config.AbilityConfig
import com.github.secretx33.infernalmobsreloaded.config.AbilityConfigKeys
import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.model.Abilities
import com.github.secretx33.infernalmobsreloaded.model.BlockModification
import com.github.secretx33.infernalmobsreloaded.model.InfernalMobType
import com.github.secretx33.infernalmobsreloaded.model.KeyChain
import com.github.secretx33.infernalmobsreloaded.repositories.LootItemsRepo
import com.github.secretx33.infernalmobsreloaded.utils.*
import com.google.common.collect.Multimap
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
import org.bukkit.event.entity.CreatureSpawnEvent.*
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
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
                Abilities.ARMOURED -> addArmouredAbility(entity)
                Abilities.FLYING -> addFlyingAbility(entity)
                Abilities.HEAVY -> addHeavyAbility(entity)
                Abilities.INVISIBLE -> addInvisibleAbility(entity)
                Abilities.MOLTEN -> addMoltenAbility(entity)
                Abilities.MOUNTED -> addMountedAbility(entity)
                Abilities.SPEEDY -> addSpeedyAbility(entity)
                else -> {}
            }
        }
        entity.multiplyMaxHp(infernalType.getHealthMulti())
    }

    private fun addArmouredAbility(entity: LivingEntity) {
        if(entity.equipWithArmor()) return
        // fallback to potion effect if entity cannot wear armor
        addArmouredPotionEffect(entity)
    }

    private fun addArmouredPotionEffect(entity: LivingEntity) {
        entity.addPotionEffect(PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Int.MAX_VALUE, max(0, abilityConfig.get<Int>(AbilityConfigKeys.ARMOURED_POTION_LEVEL) - 1), false, false, false))
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

    private fun addFlyingAbility(entity: LivingEntity) {
        val bat = entity.world.spawn(entity.location, Bat::class.java, SpawnReason.CUSTOM) {
            turnIntoMount(it, keyChain.infernalBatMountKey, false)
            it.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, Int.MAX_VALUE, 0,false, false, false))
            it.velocity = Vector(0, 1, 0)
            it.isPersistent = true
            it.multiplyMaxHp(3.5)
        }
        bat.addPassenger(entity)
        // TODO("Remove bat entity when its 'master' dies")
    }

    private fun addHeavyAbility(entity: LivingEntity) {
        entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.apply {
            val resistAmount = abilityConfig.getDoublePair(AbilityConfigKeys.HEAVY_RESIST_PERCENTAGE).getRandomBetween()
            val mod = AttributeModifier(knockbackResistUID, Abilities.HEAVY.name, resistAmount, AttributeModifier.Operation.ADD_SCALAR)
            removeModifier(mod)
            addModifier(mod)
        }
    }

    private fun addInvisibleAbility(entity: LivingEntity) = entity.addPermanentPotion(PotionEffectType.INVISIBILITY, Abilities.INVISIBLE)

    private fun addMoltenAbility(entity: LivingEntity) = entity.addPermanentPotion(PotionEffectType.FIRE_RESISTANCE, Abilities.MOLTEN)

    private fun addMountedAbility(entity: LivingEntity) {
        entity.vehicle?.apply {
            removePassenger(entity)
            if(passengers.isEmpty()) remove()
        }
        val mounts = rideableMounts

        // if player emptied the mount list, do not mount the entity
        if(mounts.isEmpty()) return
        // get the a random mount entity class
        val mountClass = mounts.random().entityClass ?: return
        // spawn it, applying the necessary changes
        val mount = entity.world.spawn(entity.location, mountClass, SpawnReason.CUSTOM) { turnIntoMount(it as LivingEntity, keyChain.infernalMountKey) }
        // and add the entity as its passenger
        mount.addPassenger(entity)
    }

    private val rideableMounts
        get() = config.getEnumSet(ConfigKeys.INFERNAL_MOBS_THAT_CAN_BE_RIDED_BY_ANOTHER, EntityType::class.java) { it != null && it.isSpawnable && it.entityClass is LivingEntity && it.entityClass !is ComplexLivingEntity }

    private fun turnIntoMount(entity: LivingEntity, tag: NamespacedKey, enablePotionParticles: Boolean = true) {
        // if entity that will be turned into a mount spawns mounted on something, remove that mount
        entity.vehicle?.remove()
        // if entity that will be turned into a mount spawns with something mounted on it, remove that sneaky passenger
        entity.passengers.forEach {
            entity.removePassenger(it)
            it.remove()
        }
        (entity as? Tameable)?.isTamed = true
        (entity as? Colorable)?.color = DyeColor.values().apply { shuffle() }.first()

        if(entity.type == EntityType.HORSE) {
            (entity as Horse).apply {
                color = Horse.Color.values().random()
                style = Horse.Style.values().random()
            }
            entity.inventory.apply {
                saddle = ItemStack(Material.SADDLE)
                armor = ItemStack(Material.DIAMOND_HORSE_ARMOR)
            }
        }
        addArmouredPotionEffect(entity)
        entity.apply {
            addPotionEffect(PotionEffect(PotionEffectType.SPEED, Int.MAX_VALUE, 0, enablePotionParticles, enablePotionParticles, false))
            pdc.set(tag, PersistentDataType.SHORT, 1)
        }
    }

    private fun addSpeedyAbility(entity: LivingEntity) {
        val movSpeed = (if(entity.doesFly()) entity.getAttribute(Attribute.GENERIC_FLYING_SPEED) else entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED))
            ?: return
        val speedBonus = abilityConfig.getDoublePair(AbilityConfigKeys.SPEEDY_BONUS).getRandomBetween()
        val mod = AttributeModifier(movSpeedUID, Abilities.SPEEDY.name, speedBonus, AttributeModifier.Operation.ADD_SCALAR)
        movSpeed.removeModifier(mod)
        movSpeed.addModifier(mod)
    }

    private fun LivingEntity.addPermanentPotion(effectType: PotionEffectType, ability: Abilities, amplifier: Int = 0) {
        addPotionEffect(PotionEffect(effectType, Int.MAX_VALUE, amplifier, abilityConfig.getPotionIsAmbient(ability), abilityConfig.getPotionEmitParticles(ability), false))
    }

    fun startTargetTasks(entity: LivingEntity, target: LivingEntity, multimap: Multimap<UUID, Job>) {
        val abilities = entity.getAbilities() ?: return

        val jobList = ArrayList<Job>()
        abilities.forEach {
            when(it) {
                Abilities.ARCHER -> makeArcherTask(entity, target, multimap)
                Abilities.CALL_THE_GANG -> makeCallTheGangTask(entity, target, multimap)
                Abilities.GHASTLY -> makeGhastlyTask(entity, target, multimap)
                Abilities.MORPH -> TODO()
                Abilities.NECROMANCER -> makeNecromancerTask(entity, target, multimap)
                Abilities.POTIONS -> TODO()
                Abilities.THIEF -> makeThiefTask(entity, target, multimap)
                Abilities.WEBBER -> makeWebberTask(entity, target, multimap)
                else -> null
            }?.let { job -> jobList.add(job) }
        }
        multimap.putAll(entity.uniqueId, jobList.filter { it.isActive })
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

    private fun makeArcherTask(entity: LivingEntity, target: LivingEntity, multimap: Multimap<UUID, Job>) = CoroutineScope(Dispatchers.Default).launch {
        val speed = abilityConfig.getProjectileSpeed(Abilities.ARCHER, 2.2)
        val amount = abilityConfig.getIntPair(AbilityConfigKeys.ARCHER_ARROW_AMOUNT, minValue = 1).getRandomBetween()
        val delay = abilityConfig.get<Double>(AbilityConfigKeys.ARCHER_ARROW_DELAY).toLongDelay()
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
        val duration = max(100, abilityConfig.getDuration(Abilities.WEBBER, 5.0).toLongDelay())
        val blocks = target.makeCuboidAround().blockList().filter { random.nextDouble() <= 0.6 && it.canMobGrief() }
        val blockMod = BlockModification(blocks, blocksBlackList) { list -> list.forEach { it.type = Material.COBWEB } }
        blockModifications.add(blockMod)

        runSync(plugin) { blockMod.make() }
        CoroutineScope(Dispatchers.Default).launch {
            delay(duration)
            runSync(plugin) { blockMod.unmake() }
        }
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

    private fun String.toAbilitySet() = gson.fromJson<Set<Abilities>>(this, infernalAbilityListToken)

    fun revertPendingBlockModifications() {
        blockModifications.forEach { it.unmake() }
        blockModifications.clear()
    }

    private companion object {
        val random = Random()
        val gson = Gson()
        val infernalAbilityListToken: Type = object : TypeToken<Set<Abilities>>() {}.type
        val movSpeedUID: UUID = "57202f4c-2e52-46cb-ad37-77550e99edb2".toUuid()
        val knockbackResistUID: UUID = "984e7a8c-188f-444b-82ea-5d02197ea8e4".toUuid()
        val healthUID: UUID = "18f1d8fb-6fed-4d47-a69b-df5c76693ad5".toUuid()
    }
}
