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
package org.cubeengine.module.vigil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cubeengine.module.vigil.data.LookupSettings;
import org.cubeengine.module.vigil.report.Report;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.math.vector.Vector3i;



// locatable: chat, command, join, quit, teleport, death, inventory_open, craft/enchant
// entity:  spawn destruct/kill, interact
// block: change explosion inventory_change


// Scenarios
//
// BLOCK STUFF ONLY
// find griefing around location
// SELECT //
// - count block breaks
// - min date
// - max date
// where around xyz
// where timelimit  (24h?)
// group by player, timerange (1min)
// ----
// then filter by player+timelimit
// SELECT
// -- count block breaks
// -- min date max date
// where around xyz
// where timelimit
// where player
// group by player, timerange (1min), blocktype
// ----
// expand search zone by 5 blocks? show delta


// BLOCK STUFF only?
// check one specific block
// where xyz
// where timelimit
// group by player, timerange same as above...
// check for inventory?

// GENERAL PLAYER SEARCH
// on specific player
// group by blocks in area?
// group by chat

// inventory specific search


// TODO teleport report


public class Lookup
{

    private Map<LookupTiming, Long> timingStart = new HashMap<>();
    private Map<LookupTiming, Long> timingTime = new HashMap<>();

    private LookupSettings settings;

    private ResourceKey world;
    private transient Vector3i position;
    private AABB boundingBox;


    public String player;

    public Lookup(LookupSettings settings)
    {
        this.settings = settings;
    }

    public Lookup with(ServerLocation loc)
    {
        this.world = loc.world().key();
        this.position = loc.blockPosition();
        return this;
    }


    public AABB boundingBox() {
        switch (this.settings.areaMode) {
            case SINGLE -> {
                return null;
            }
            case RADIUS -> {
                if (boundingBox == null) {
                    final int radius = this.settings.radius;
                    this.boundingBox = AABB.of(Vector3i.from(position.x() - radius, position.y() - radius, position.z() - radius),
                            Vector3i.from(position.x() + radius, position.y() + radius, position.z() + radius));
                }
                return boundingBox;
            }
            case AREA -> {
                // TODO implement on LookupData
                return AABB.of(position, position);
            }
        }
        return boundingBox;
    }


    public Lookup copy()
    {
        Lookup lookup = new Lookup(settings);
        lookup.world = this.world;
        lookup.position = this.position;
        lookup.settings = this.settings.copy();
        return lookup;
    }

    public ResourceKey world()
    {
        return world;
    }

    public Vector3i position()
    {
        return position;
    }

    public LookupSettings settings()
    {
        return settings;
    }


    public void time(LookupTiming timing)
    {
        Long start = timingStart.remove(timing);
        if (start == null)
        {
            timingStart.put(timing, System.currentTimeMillis());
        }
        else
        {
            Long time = timingTime.getOrDefault(timing, 0L);
            time += System.currentTimeMillis() - start;
            timingTime.put(timing, time);
        }
    }

    public long timing(LookupTiming timing)
    {
        return timingTime.get(timing);
    }

    public enum LookupTiming
    {
        LOOKUP,
        REPORT,
        PREPARE
    }

}
