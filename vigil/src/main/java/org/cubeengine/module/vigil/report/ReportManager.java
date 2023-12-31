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
package org.cubeengine.module.vigil.report;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.module.vigil.Vigil;
import org.cubeengine.module.vigil.report.block.BlockReport;
import org.cubeengine.module.vigil.report.entity.DestructReport;
import org.cubeengine.module.vigil.report.entity.player.ChatReport;
import org.cubeengine.module.vigil.report.entity.player.JoinReport;
import org.cubeengine.module.vigil.report.entity.player.QuitReport;
import org.cubeengine.module.vigil.report.inventory.ChangeInventoryReport;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;

@Singleton
public class ReportManager
{
    private Vigil module;
    private EventManager em;
    private I18n i18n;

    private Map<String, Report> reports = new HashMap<>();

    @Inject
    public ReportManager(Vigil module, EventManager em, I18n i18n)
    {
        this.module = module;
        this.em = em;
        this.i18n = i18n;
        // TODO ReportManager to be able to call showReport(Action) on it
        // TODO also allow classloading additional Reports from module folder
        register(BlockReport.class);
        // TODO register(ExplosionReport.class);
        register(ChangeInventoryReport.class);
        register(ChatReport.class);
        register(JoinReport.class);
        register(QuitReport.class);
        register(DestructReport.class);
    }

    public void register(Class<? extends Report> report)
    {
        try
        {
            Report instance = report.getDeclaredConstructor().newInstance();
            if (instance instanceof BaseReport)
            {
                ((BaseReport) instance).init(module);
            }
            reports.put(report.getName(), instance);
            em.registerListener(instance);
        }
        catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }

    public Report reportOf(Action action)
    {
        String type = Action.TYPE.get(action.getDocument());
        return reports.getOrDefault(type, new UnkownReport(i18n, type));
    }

    public Map<String, Report> getReports()
    {
        return reports;
    }
}
