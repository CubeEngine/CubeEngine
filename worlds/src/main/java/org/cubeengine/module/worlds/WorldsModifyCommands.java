/*
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cubeengine.module.worlds;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.world.SerializationBehavior;
import org.spongepowered.api.world.difficulty.Difficulty;
import org.spongepowered.api.world.generation.config.WorldGenerationConfig;
import org.spongepowered.api.world.server.storage.ServerWorldProperties;
import org.spongepowered.math.vector.Vector3i;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Singleton
@Command(name = "modify", desc = "Worlds modify commands")
public class WorldsModifyCommands extends DispatcherCommand
{
    private I18n i18n;

    @Inject
    public WorldsModifyCommands(I18n i18n)
    {
        this.i18n = i18n;
    }

    @Command(desc = "Sets the autoload behaviour")
    public void autoload(CommandCause context, ServerWorldProperties world, @Option Boolean set)
    {
        if (set == null)
        {
            set = !world.loadOnStartup();
        }
        world.setLoadOnStartup(set);
        if (set)
        {
            i18n.send(context, POSITIVE, "{world} will now autoload.", world);
            return;
        }
        i18n.send(context, POSITIVE, "{world} will no longer autoload.", world);
    }

    @Command(desc = "Sets whether features generate")
    public void generateFeatures(CommandCause context, ServerWorldProperties world, @Option Boolean set)
    {
        if (set == null)
        {
            set = !world.worldGenerationConfig().generateStructures();
        }
        world.offer(Keys.WORLD_GEN_CONFIG, WorldGenerationConfig.builder().from(world.worldGenerationConfig()).generateStructures(set).build());
        if (set)
        {
            i18n.send(context, POSITIVE, "{world} will now generate structures", world);
            return;
        }
        i18n.send(context, POSITIVE, "{world} will no longer generate structures", world);
    }

    @Command(desc = "Sets world spawn")
    public void spawn(CommandCause context, ServerWorldProperties world, Vector3i spawnPoint)
    {
        world.offer(Keys.SPAWN_POSITION, spawnPoint);
        i18n.send(context, POSITIVE, "{world} world spawn changed to {vector}", world, spawnPoint);
    }

    @Command(desc = "Sets view distance")
    public void viewDistance(CommandCause context, ServerWorldProperties world, int viewDistance)
    {
        world.offer(Keys.VIEW_DISTANCE, viewDistance);
        i18n.send(context, POSITIVE, "{world} view distance changed to {number}", world, viewDistance);
    }

    @Command(desc = "Sets spawnchunks loaded")
    public void spawnChunks(CommandCause context, ServerWorldProperties world, boolean loaded)
    {
        world.offer(Keys.PERFORM_SPAWN_LOGIC, loaded);
        
        i18n.send(context, POSITIVE, "{world} spawn-chunks loaded changed to {name}", world, String.valueOf(loaded));
    }

    @Command(desc = "Sets hardcore mode")
    public void hardcore(CommandCause context, ServerWorldProperties world, boolean hardcore)
    {
        world.offer(Keys.HARDCORE, hardcore);
        i18n.send(context, POSITIVE, "{world} hardcore mode changed to {name}", world, String.valueOf(hardcore));
    }

    @Command(desc = "Sets command usage")
    public void commands(CommandCause context, ServerWorldProperties world, boolean allowed)
    {
        world.offer(Keys.COMMANDS, allowed);
        i18n.send(context, POSITIVE, "{world} command usage changed to {name}", world, String.valueOf(allowed));
    }

    @Command(desc = "Sets pvp")
    public void pvp(CommandCause context, ServerWorldProperties world, boolean pvp)
    {
        world.offer(Keys.PVP, pvp);
        i18n.send(context, POSITIVE, "{world} pvp changed to {name}", world, String.valueOf(pvp));
    }

    @Command(desc = "Sets the seed")
    public void seed(CommandCause context, ServerWorldProperties world, String seed)
    {
        world.offer(Keys.SEED, (long) seed.hashCode());
        i18n.send(context, POSITIVE, "{world} seed changed to {name}", world, seed);
    }

    @Command(desc = "Sets the serialization behavior")
    public void serialize(CommandCause context, ServerWorldProperties world, SerializationBehavior serializationBehavior)
    {
        world.offer(Keys.SERIALIZATION_BEHAVIOR, serializationBehavior);
        i18n.send(context, POSITIVE, "{world} serialization behavior changed to {name}", world, serializationBehavior.name());
    }

    @Command(desc = "Sets the difficulty")
    public void difficulty(CommandCause context, ServerWorldProperties world, Difficulty difficulty)
    {
        world.offer(Keys.WORLD_DIFFICULTY, difficulty);
        i18n.send(context, POSITIVE, "{world} difficulty changed to {name}", world, difficulty.key(RegistryTypes.DIFFICULTY).asString());
    }

    @Command(desc = "Sets the default gamemode")
    public void gamemode(CommandCause context, ServerWorldProperties world, GameMode gameMode)
    {
        world.offer(Keys.GAME_MODE, gameMode);
        i18n.send(context, POSITIVE, "{world} gamemode changed to {name}", world, gameMode.key(RegistryTypes.GAME_MODE).asString());
    }

    // TODO changing world type?
    // TODO custom chunk-generators?

}
