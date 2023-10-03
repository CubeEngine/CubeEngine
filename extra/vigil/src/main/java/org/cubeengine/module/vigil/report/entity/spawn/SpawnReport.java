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
package org.cubeengine.module.vigil.report.entity.spawn;

import org.cubeengine.module.vigil.report.BaseReport;
import org.spongepowered.api.event.entity.SpawnEntityEvent;

/* TODO
spawn
-egg
-natural
-other
-spawner
-hanging place

- vehicle place

-item-drop
-drops (attach to death event)

-launch-projectile
--use-firework
--use-potion
--arrow?
 */
public abstract class SpawnReport<T extends SpawnEntityEvent> extends BaseReport<T>
{

}
