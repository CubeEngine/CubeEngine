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
package org.cubeengine.service.paginate;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.service.command.CommandContext;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;

public class PaginationCommands
{
    private SpongePaginationManager paginationManager;

    public PaginationCommands(SpongePaginationManager paginationManager)
    {
        this.paginationManager = paginationManager;
    }

    @Command(desc = "Display the next page of your previous command.")
    public void next(CommandContext context)
    {
        if (paginationManager.hasResult(context.getSource()))
        {
            paginationManager.getResult(context.getSource()).nextPage();
            return;
        }
        context.sendTranslated(NEGATIVE, "You don't have any results to show!");
    }

    @Command(desc = "Display the previous page of your previous command.")
    public void prev(CommandContext context)
    {
        if (paginationManager.hasResult(context.getSource()))
        {
            paginationManager.getResult(context.getSource()).prevPage();
            return;
        }
        context.sendTranslated(NEGATIVE, "You don't have any results to show!");
    }

    @Command(desc = "Display the given page of your previous command.")
    public void showpage(CommandContext context, @Label("page-number") Integer page)
    {
        if (paginationManager.hasResult(context.getSource()))
        {
            paginationManager.getResult(context.getSource()).showPage(page - 1);
            return;
        }
        context.sendTranslated(NEGATIVE, "You don't have any results to show!");
    }
}