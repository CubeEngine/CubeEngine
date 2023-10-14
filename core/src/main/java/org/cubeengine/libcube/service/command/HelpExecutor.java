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
package org.cubeengine.libcube.service.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.JoinConfiguration.Builder;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.Command.Parameterized;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.Parameter.Key;
import org.spongepowered.api.command.parameter.Parameter.Subcommand;
import org.spongepowered.api.command.parameter.Parameter.Value;
import org.spongepowered.api.event.EventContextKeys;

public class HelpExecutor implements CommandExecutor
{

    private I18n i18n;
    private Command.Parameterized target;
    private CubeEngineCommand executor;
    private String perm;

    public HelpExecutor(I18n i18n)
    {
        this.i18n = i18n;
    }

    public HelpExecutor(I18n i18n, CubeEngineCommand executor)
    {
        this.i18n = i18n;
        this.executor = executor;
    }

    @Override
    public CommandResult execute(CommandContext context) throws CommandException
    {
        final Optional<String> rawCommand = context.cause().context().get(EventContextKeys.COMMAND);
        final Audience audience = context.cause().audience();
        final Style grayStyle = Style.style(NamedTextColor.GRAY);
        Component descLabel = i18n.translate(audience, grayStyle, "Description:");
        final Component permText = i18n.translate(audience, grayStyle, "Permission: (click to copy) {input}", perm).append(Component.text(".use").color(NamedTextColor.WHITE));
        descLabel = descLabel.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, permText)).clickEvent(ClickEvent.copyToClipboard(perm + ".use"));
        final Component descValue = target.shortDescription(context.cause()).get().color(NamedTextColor.GOLD);
        context.sendMessage(Identity.nil(), Component.empty().append(descLabel).append(Component.space()).append(descValue));

        final String actual = rawCommand.map(r -> r.endsWith("?") ? r.substring(0, r.length() - 1) : r).orElse("missing command context").trim();

        final Component usage = usage(context, Collections.emptyList());
        i18n.send(audience, grayStyle, "Usage: {input}", Component.text(actual).append(Component.space()).append(usage));

//            context.sendMessage(target.getUsage(context.getCause()).style(grayStyle));
//            context.sendMessage(Component.text(actual.orElse("no cmd?")));

        final List<Parameter.Subcommand> subcommands = target.subcommands().stream().filter(sc -> !sc.aliases().iterator().next().equals("?")).toList();
        if (!subcommands.isEmpty())
        {
            context.sendMessage(Identity.nil(), Component.empty());
            i18n.send(audience, MessageType.NEUTRAL, "The following sub-commands are available:");
            context.sendMessage(Identity.nil(), Component.empty());
            for (Parameter.Subcommand subcommand : subcommands)
            {
                final String firstAlias = subcommand.aliases().iterator().next();
                final Command.Parameterized subCmd = subcommand.command();
                TextComponent textPart1 = Component.text(firstAlias, NamedTextColor.YELLOW);
                final Component subPermText = i18n.translate(audience, grayStyle, "Permission: (click to copy) {input}", perm + "." + firstAlias).append(
                    Component.text(".use").color(NamedTextColor.WHITE));
                textPart1 = textPart1.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, subPermText)).clickEvent(
                    ClickEvent.copyToClipboard(perm + "." + firstAlias + ".use"));
                final String newHelpCmd = actual + " " + firstAlias + " ?";
                final TextComponent text = Component.empty().append(textPart1).append(Component.text(": ")).append(subCmd.shortDescription(context.cause()).get().style(
                    grayStyle).hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("click to show usage"))).clickEvent(
                    ClickEvent.runCommand(newHelpCmd))
                                                                                                                   // TODO missing command context
                                                                                                                  );

                context.sendMessage(Identity.nil(), text);
            }
        }
        else
        {
            if (this.executor == null)
            {
                i18n.send(audience, MessageType.NEGATIVE, "No actions are available");
            }
        }
        context.sendMessage(Identity.nil(), Component.empty());
        return CommandResult.success();
    }

    public Component usage(final CommandContext context, final List<Parameter.Key<?>> errors)
    {
        List<Component> usages = new ArrayList<>();
        for (Parameter param : target.parameters())
        {
            collectUsage(context, usages, param, errors);
        }

        return usages.isEmpty() && executor == null ? Component.text("<command>") : Component.join(JoinConfiguration.separator(Component.space()), usages);
    }

    public void errorUsage(final CommandContext context, final List<Parameter.Key<?>> errors) throws CommandException
    {
        // TODO handle other errors
        final var usage = this.usage(context, errors);

        final String rawCmd = context.cause().context().get(EventContextKeys.COMMAND).orElse("???");
        // TODO get base cmd when this is a command with parameters
        final Component fullUsage = Component.text(rawCmd).append(Component.space()).append(usage).color(NamedTextColor.GOLD);
        final Component append = Component.text("Too few arguments:").append(Component.newline()).append(fullUsage);
        throw new ArgumentParseException(append, rawCmd, rawCmd.length() + 2);
    }

    private void collectUsage(CommandContext context, List<Component> usages, Parameter param, List<Key<?>> errors)
    {
        if (param instanceof Parameter.Value)
        {
            final boolean hasError = errors.contains(((Value<?>)param).key());
            var usage = Component.text(((Parameter.Value<?>)param).usage(context.cause()));
            if (!param.isOptional())
            {
                if (hasError)
                {
                    usage = Component.text("<", NamedTextColor.RED)
                                     .append(usage.color(NamedTextColor.GOLD))
                                     .append(Component.text(">", NamedTextColor.RED));
                }
                else
                {
                    usage = Component.text("<").append(usage).append(Component.text(">"));
                }
            }
            usages.add(usage);
        }
        else if (param instanceof Parameter.Multi)
        {
            final List<Component> childUsages = new ArrayList<>();
            for (Parameter childParam : ((Parameter.Multi)param).childParameters())
            {
                this.collectUsage(context, childUsages, childParam, errors);
            }

            // Fixing multiple named parameters usage:
            // As SubCommands cannot be optional they are nested in multiple FirstOf-SubCommand combos
            // Essentially [p1 <value>] [p2 <value>] is actually registered as
            // [p1 <value> [p2 <value>]] or [p2 <value>]
            // For this we can go through usages in reverse and remove all previous occurences of a named parameter
            final List<String> repeats = new ArrayList<>();
            Collections.reverse(childUsages);
            for (Component childUsage : childUsages)
            {
                String rawChildUsage = PlainTextComponentSerializer.plainText().serialize(childUsage);
                for (String repeat : repeats)
                {
                    String optRepeat = " [" + repeat + "]";
                    if (rawChildUsage.endsWith(optRepeat))
                    {
                        rawChildUsage = rawChildUsage.substring(0, rawChildUsage.indexOf(optRepeat));
                    }
                }
                repeats.add(rawChildUsage);
            }
            Collections.reverse(repeats);
            childUsages.clear();
            childUsages.addAll(repeats.stream().map(Component::text).toList());

            // TODO different usage for sequence/firstof
            final Builder joinCfgBuilder = JoinConfiguration.builder();

            if (param.getClass().getName().contains("FirstOf"))
            {
                joinCfgBuilder.separator(Component.text(" | "));
            }
            else
            {
                joinCfgBuilder.separator(Component.text(" "));
            }
            if (param.isOptional())
            {
                joinCfgBuilder.prefix(Component.text("["));
                joinCfgBuilder.suffix(Component.text("]"));
            }
            else
            {
                joinCfgBuilder.prefix(Component.text("<"));
                joinCfgBuilder.suffix(Component.text(">"));
            }
            usages.add(Component.join(joinCfgBuilder.build(), childUsages));
        }
        else if (param instanceof Subcommand)
        {
            final Set<String> aliases = ((Subcommand)param).aliases();
            final ArrayList<Component> subUsage = new ArrayList<>();
            for (Parameter subCmdParam : ((Subcommand)param).command().parameters())
            {
                this.collectUsage(context, subUsage, subCmdParam, errors);
            }

            final var alias = Component.join(JoinConfiguration.separator(Component.text("|", NamedTextColor.GRAY)),
                                                      aliases.stream().map(Component::text).toList());

            usages.add(alias.append(Component.space()).append(Component.join(JoinConfiguration.separator(Component.space()), subUsage)));

        }
        else
        {
            usages.add(Component.text("param(" + param.getClass().getSimpleName() + ")"));
        }
    }

    public void init(Parameterized target, String perm)
    {
        this.target = target;
        this.perm = perm;
    }
}
