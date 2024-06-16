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
package org.cubeengine.module.mechanism.sign;

import java.util.Arrays;
import java.util.List;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.libcube.util.BlockUtil;
import org.cubeengine.module.mechanism.Mechanism;
import org.cubeengine.module.mechanism.MechanismData;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.server.ServerLocation;

@Singleton
public class HiddenLever extends PermissionContainer implements SignMechanism
{
    public static final String NAME = "hidden-lever";

    @Inject
    public HiddenLever(PermissionManager pm)
    {
        super(pm, Mechanism.class);
    }

    private final Permission hiddenLeverPerm = this.register("sign.hidden-lever.use", "Allows using hidden levers");;


    public ItemStack makeSign(ItemStack signStack)
    {
        signStack.offer(Keys.CUSTOM_NAME, Component.text("[Mechanism]", NamedTextColor.GOLD).append(Component.space()).append(Component.text(HiddenLever.NAME, NamedTextColor.DARK_AQUA)));
        signStack.offer(MechanismData.MECHANISM, HiddenLever.NAME);
        signStack.offer(Keys.LORE, Arrays.asList(Component.text(NAME, NamedTextColor.YELLOW)));
        signStack.offer(Keys.SIGN_LINES, List.of(Component.text("HIDDEN_LEVER")));
        return signStack;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public boolean interact(InteractBlockEvent event, ServerPlayer player, ServerLocation loc, boolean hidden)
    {
        if (!hidden)
        {
            return false;
        }
        if (!this.hiddenLeverPerm.check(player))
        {
            return false;
        }
        boolean click = false;
        for (Direction dir : BlockUtil.BLOCK_FACES)
        {
            final ServerLocation relative = loc.relativeTo(dir);
            @SuppressWarnings("unchecked")
            final var isLever = relative.blockType().isAnyOf(BlockTypes.LEVER);
            if (isLever)
            {
                final BlockState state = relative.block();
                final boolean newPower = !state.get(Keys.IS_POWERED).orElse(false);
                relative.setBlock(state.with(Keys.IS_POWERED, newPower).get());
                loc.world().playSound(Sound.sound(SoundTypes.BLOCK_LEVER_CLICK, Source.BLOCK, 0.1f, newPower ? 0.9f : 0.7f), loc.position());
                click = true;
            }
        }
        if (!click)
        {
            loc.world().playSound(Sound.sound(SoundTypes.BLOCK_NOTE_BLOCK_HAT, Source.BLOCK, 0.2f, 2), loc.position());
        }
        return true;
    }
}
