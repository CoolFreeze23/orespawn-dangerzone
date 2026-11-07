package danger.orespawn.dangerzone.world;

import danger.orespawn.dangerzone.DzConfig;
import danger.orespawn.dangerzone.ModContent;
import danger.orespawn.dangerzone.OreSpawnDangerZone;
import danger.orespawn.dangerzone.entity.ElementDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * What DangerZone's castle builders run against. It takes their block writes, chests and creatures, then places them
 * a slice per tick so that carving millions of blocks doesn't stop the server. Creatures come last, on finished ground.
 */
@EventBusSubscriber(modid = OreSpawnDangerZone.MOD_ID)
public final class DzBuilder {
    private static final List<String> IDS = new ArrayList<>(List.of("air"));
    private static final List<DzBuilder> ACTIVE = new ArrayList<>();
    private static final int BLOCK_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    /** Something a builder asked to hold; the castle's captive has a tiara and a staff this mod doesn't have yet. */
    public record Held(String name, int count) {
    }

    /** A creature a builder asked for, in DangerZone's terms. */
    public static final class Creature {
        final String name;
        final double x;
        final double y;
        final double z;
        int variant;

        Creature(String name, double x, double y, double z) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public void init() {
        }

        public void setCanDespawn(boolean can) {
        }

        public void setIID(int id) {
            this.variant = id;
        }

        public void setVarInt(int slot, int value) {
        }

        public void setArmor(int slot, Held held) {
        }

        public void setHotbar(int slot, Held held) {
        }
    }

    private record Chest(BlockPos pos, String list) {
    }

    private record Spawner(BlockPos pos, int id) {
    }

    private final ServerLevel level;
    private final RandomSource random;
    private long[] positions = new long[4096];
    private short[] blocks = new short[4096];
    private int size;
    private int placed;
    private final List<Chest> chests = new ArrayList<>();
    private final List<Spawner> spawners = new ArrayList<>();
    private final List<Creature> creatures = new ArrayList<>();
    private BlockState[] resolved;
    private Runnable whenDone = () -> {
    };

    public DzBuilder(ServerLevel level) {
        this.level = level;
        this.random = level.random;
    }

    static synchronized int id(String name) {
        int at = IDS.indexOf(name);
        if (at < 0) {
            IDS.add(name);
            at = IDS.size() - 1;
        }
        return at;
    }

    // ---- what the translated builders call

    public void set(int x, int y, int z, int id) {
        if (this.size == this.positions.length) {
            this.positions = Arrays.copyOf(this.positions, this.size * 2);
            this.blocks = Arrays.copyOf(this.blocks, this.size * 2);
        }
        this.positions[this.size] = BlockPos.asLong(x, y, z);
        this.blocks[this.size++] = (short) id;
    }

    public void spawner(int x, int y, int z, int id) {
        this.spawners.add(new Spawner(new BlockPos(x, y, z), id));
    }

    public int get(int x, int y, int z) {
        return this.level.getBlockState(new BlockPos(x, y, z)).isAir() ? 0 : 1;
    }

    public void chest(int x, int y, int z, String list) {
        this.chests.add(new Chest(new BlockPos(x, y, z), list));
    }

    public Creature creature(String name, double x, double y, double z) {
        Creature creature = new Creature(name, x, y, z);
        this.creatures.add(creature);
        return creature;
    }

    public Creature create(String name, double x, double y, double z) {
        return new Creature(name, x, y, z);
    }

    public void add(Creature creature) {
        this.creatures.add(creature);
    }

    public int rand(int bound) {
        return this.random.nextInt(bound);
    }

    // ---- placing it

    public int pending() {
        return this.size - this.placed;
    }

    /** Queues the work to run a slice per tick. */
    public void start(Runnable whenDone) {
        this.whenDone = whenDone;
        ACTIVE.add(this);
    }

    private BlockState state(int id) {
        if (this.resolved == null) {
            this.resolved = new BlockState[IDS.size()];
        }
        if (id >= this.resolved.length) {
            this.resolved = Arrays.copyOf(this.resolved, IDS.size());
        }
        BlockState state = this.resolved[id];
        if (state == null) {
            String name = IDS.get(id);
            state = name.equals("air") ? Blocks.AIR.defaultBlockState()
                    : BuiltInRegistries.BLOCK.get(ResourceLocation.parse(DzDungeons.PALETTE.get(name))).defaultBlockState();
            this.resolved[id] = state;
        }
        return state;
    }

    /** One slice. Returns true when everything is finished. Public for the tests. */
    public boolean work(int budget) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int end = Math.min(this.size, this.placed + budget);
        for (; this.placed < end; this.placed++) {
            pos.set(this.positions[this.placed]);
            this.level.setBlock(pos, state(this.blocks[this.placed]), BLOCK_FLAGS);
        }
        if (this.placed < this.size) {
            return false;
        }
        this.chests.forEach(this::placeChest);
        this.spawners.forEach(this::placeSpawner);
        this.creatures.forEach(this::placeCreature);
        this.chests.clear();
        this.spawners.clear();
        this.creatures.clear();
        return true;
    }

    private void placeChest(Chest chest) {
        this.level.setBlock(chest.pos(), Blocks.CHEST.defaultBlockState(), BLOCK_FLAGS);
        if (this.level.getBlockEntity(chest.pos()) instanceof RandomizableContainerBlockEntity container) {
            ResourceKey<LootTable> table = ResourceKey.create(Registries.LOOT_TABLE, OreSpawnDangerZone.id("chests/" + chest.list().toLowerCase()));
            container.setLootTable(table, this.random.nextLong());
        }
    }

    private void placeSpawner(Spawner spawner) {
        this.level.setBlock(spawner.pos(), Blocks.SPAWNER.defaultBlockState(), BLOCK_FLAGS);
        BlockEntity entity = this.level.getBlockEntity(spawner.pos());
        if (entity instanceof SpawnerBlockEntity block) {
            String target = DzDungeons.PALETTE.get(IDS.get(spawner.id()));
            block.setEntityId(BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(target)), this.random);
        }
    }

    private static final Map<String, String> CREATURES = Map.of(
            "OreSpawn:Kraken", "orespawn:kraken",
            "OreSpawn:Squid", "orespawn:attack_squid",
            "OreSpawn:Girlfriend", "orespawn:girlfriend",
            "OreSpawn:SquidGuard", "orespawn_dangerzone:squid_guard",
            "OreSpawn:ChaosDemon", "orespawn_dangerzone:chaos_demon",
            "OreSpawn:BabySitter", "orespawn_dangerzone:baby_sitter",
            "OreSpawn:Element Dragon", "orespawn_dangerzone:element_dragon",
            "OreSpawn:Dargon", "orespawn_dangerzone:dargon");

    private void placeCreature(Creature creature) {
        String id = CREATURES.get(creature.name);
        if (id == null) {
            return;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(id));
        Entity entity = type.create(this.level);
        if (entity instanceof Mob mob) {
            mob.moveTo(creature.x, creature.y, creature.z, this.random.nextFloat() * 360.0f, 0.0f);
            mob.finalizeSpawn(this.level, this.level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.STRUCTURE, null);
            mob.setPersistenceRequired();
            if (mob instanceof ElementDragon dragon) {
                dragon.setElement(creature.variant);
                dragon.guardHome(mob.blockPosition());
            }
            this.level.addFreshEntity(mob);
        }
    }

    /** A build never outlives its world: in singleplayer the same game can go on to open another. */
    @SubscribeEvent
    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        ACTIVE.clear();
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        int budget = DzConfig.BUILDER_BLOCKS_PER_TICK.get();
        Iterator<DzBuilder> it = ACTIVE.iterator();
        while (it.hasNext() && budget > 0) {
            DzBuilder builder = it.next();
            int before = builder.pending();
            if (builder.work(budget)) {
                it.remove();
                builder.whenDone.run();
            }
            budget -= before;
        }
    }

    /** How many builders are still placing. Public for the tests. */
    public static int activeCount() {
        return ACTIVE.size();
    }
}
