/**
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
package de.cubeisland.engine.module.selector;

import org.cubeengine.module.core.util.math.Vector3;
import org.cubeengine.module.core.util.math.shape.Cuboid;
import org.cubeengine.module.core.util.math.shape.Shape;
import org.cubeengine.service.user.UserAttachment;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class SelectorAttachment extends UserAttachment
{
    private enum Mode
    {
        CUBOID(2);
        private int initialSize;

        Mode(int initialSize)
        {
            this.initialSize = initialSize;
        }

        public int initialSize()
        {
            return this.initialSize;
        }
    }

    private Mode mode = Mode.CUBOID;

    private World lastPointWorld;

    private Location[] points = new Location[mode.initialSize];

    public void setPoint(int index, Location location)
    {
        this.points[index] = location;
        this.lastPointWorld = ((World)location.getExtent());
    }

    public int addPoint(Location location)
    {
        /*
        this.points.add(location);
        this.lastPointWorld = location.getWorld();
        return this.points.size();
        */
        throw new UnsupportedOperationException("Not supported yet!");
    }

    public Location getPoint(int index)
    {
        return this.points[index];
    }

    public Shape getSelection()
    {
        for (Location point : this.points)
        {
            if (point == null) // missing point
            {
                return null;
            }
            if (lastPointWorld != ((World)point.getExtent())) // points are in different worlds
            {
                return null;
            }
        }
        if (this.getPoint(0) == null || this.getPoint(1) == null)
        {
            return null;
        }
        return this.getSelection0();
    }

    private Shape getSelection0()
    {
        Vector3 v1 = new Vector3(this.getPoint(0).getX(), this.getPoint(0).getY(), this.getPoint(0).getZ());
        Vector3 v2 = new Vector3(this.getPoint(1).getX(), this.getPoint(1).getY(), this.getPoint(1).getZ());
        return new Cuboid(v1.midpoint(v2), v1.distanceVector(v2));
    }
}
