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
/**
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 * <p>
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cubeengine.module.vigil.report;

import java.time.Duration;
import java.util.Optional;

import net.kyori.adventure.audience.Audience;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.vigil.reporting.Receiver;
import org.cubeengine.module.vigil.action.Action;
import org.cubeengine.module.vigil.reporting.PreparedReport;
import org.spongepowered.api.item.inventory.ItemStack;

public interface Report
{
    default boolean filterable() {
        return true;
    }
    ItemStack getIcon(final I18n i18n, final Audience audience);

    static Optional<? extends Class<? extends Report>> getReport(String name)
    {
        Class<? extends Report> clazz = null;
        try
        {
            try
            {
                clazz = (Class<? extends Report>) Class.forName(name);
            }
            catch (ClassNotFoundException e)
            {
                try
                {
                    clazz = (Class<? extends Report>) Class.forName("org.cubeengine.module.vigil.report." + name);
                }
                catch (ClassNotFoundException e1)
                {
                    System.err.println("Cannot find Report for: " + name);
                }
            }
        }
        catch (ClassCastException e)
        {
            System.err.println("Class of " + name + " is not a Report class.");
        }
        return Optional.ofNullable(clazz);
    }

    /**
     * Shows the action to given CommandSource
     *
     * @param receiver   the CommandSource
     * @param reportLine
     */
    void showReportLine(Receiver receiver, final PreparedReport.ReportLine reportLine);



    /**
     * Applies the action to the world
     *
     * @param action   the action to apply
     * @param noOp true if permanent or false transient
     */
    void apply(Action action, boolean noOp);

    /**
     * Applies the reverse action to the world
     *
     * @param action the action to unapply
     * @param noOp true if permanent or false if transient
     */
    void unapply(Action action, boolean noOp);

     Duration maxDiff();


    interface Readonly extends Report
    {
        default void apply(Action action, boolean noOp) {}
        default void unapply(Action action, boolean noOp) {}
    }

}
