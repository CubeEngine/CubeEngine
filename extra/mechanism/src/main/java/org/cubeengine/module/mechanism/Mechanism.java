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
package org.cubeengine.module.mechanism;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.util.EventUtil;
import org.cubeengine.processor.Module;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.block.transaction.Operations;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.entity.ChangeSignEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.event.lifecycle.RegisterDataPackValueEvent;
import org.spongepowered.api.item.recipe.RecipeRegistration;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.server.ServerLocation;

/**
 * A module to edit signs and signed books
 */
// TODO missing permission for Gate
@Singleton
@Module
public class Mechanism
{
    @Inject
    private MechanismManager manager;

    @Listener
    public void onRegisterData(RegisterDataEvent event)
    {
        MechanismData.register(event);
    }

    @Listener
    public void onRecipeRegister(RegisterDataPackValueEvent<RecipeRegistration>event)
    {
        this.manager.init();
        MechanismItems.registerRecipes(event, manager);
    }

    @Listener
    public void onRightClickBlock(InteractBlockEvent.Secondary event, @First ServerPlayer player)
    {
        if (!EventUtil.isMainHand(event.context()))
        {
            return;
        }
        final ServerLocation thisLoc = event.block().location().get();
        final boolean triggered = thisLoc.blockEntity().flatMap(be -> be.get(MechanismData.MECHANISM)).map(
            mechanism -> manager.trigger(mechanism, event, player, thisLoc, false)).orElse(false);
        if (!triggered)
        {
            final Direction oppositeDirection = event.targetSide().opposite();
            final ServerLocation oppositeLoc = thisLoc.relativeTo(oppositeDirection);
            oppositeLoc.blockEntity().filter(be -> be.supports(Keys.SIGN_LINES))
                       .filter(be -> be.get(Keys.DIRECTION).map(facing -> facing.equals(oppositeDirection)).orElse(false))
                       .flatMap(be -> be.get(MechanismData.MECHANISM)).ifPresent(mechanism -> {
                manager.trigger(mechanism, event, player, oppositeLoc, true);
            });
        }
    }

    @Listener
    public void onPlaceBlock(ChangeBlockEvent.All event, @First ServerPlayer player)
    {
        event.context().get(EventContextKeys.USED_ITEM).flatMap(item -> item.get(MechanismData.MECHANISM)).ifPresent(mechanism ->
            event.transactions(Operations.PLACE.get()).forEach(trans -> {
                final BlockEntity sign = trans.finalReplacement().location().get().blockEntity().get();
                manager.initializeSign(mechanism, sign);

            }));
    }

    @Listener
    public void onChangeSign(ChangeSignEvent event)
    {
        if (event.sign().get(MechanismData.MECHANISM).isPresent())
        {
            event.setCancelled(true);
        }
    }

}
