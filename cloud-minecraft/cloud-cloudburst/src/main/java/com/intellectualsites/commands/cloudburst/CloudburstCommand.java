//
// MIT License
//
// Copyright (c) 2020 Alexander Söderberg
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
package com.intellectualsites.commands.cloudburst;

import com.intellectualsites.commands.arguments.CommandArgument;
import com.intellectualsites.commands.exceptions.ArgumentParseException;
import com.intellectualsites.commands.exceptions.InvalidCommandSenderException;
import com.intellectualsites.commands.exceptions.InvalidSyntaxException;
import com.intellectualsites.commands.exceptions.NoPermissionException;
import com.intellectualsites.commands.exceptions.NoSuchCommandException;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.PluginCommand;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.plugin.Plugin;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletionException;

final class CloudburstCommand<C> extends PluginCommand<Plugin> {

    private static final String MESSAGE_NO_PERMS =
            "I'm sorry, but you do not have permission to perform this command. "
                    + "Please contact the server administrators if you believe that this is in error.";
    private static final String MESSAGE_UNKNOWN_COMMAND = "Unknown command. Type \"/help\" for help.";

    private final CommandArgument<C, ?> command;
    private final CloudburstCommandManager<C> manager;
    private final com.intellectualsites.commands.Command<C> cloudCommand;

    CloudburstCommand(@Nonnull final String label,
                      @Nonnull final List<String> aliases,
                      @Nonnull final com.intellectualsites.commands.Command<C> cloudCommand,
                      @Nonnull final CommandArgument<C, ?> command,
                      @Nonnull final CloudburstCommandManager<C> manager) {
        super(manager.getOwningPlugin(), CommandData.builder(label)
                                .addAliases(aliases.toArray(new String[0]))
                                .addPermission(cloudCommand.getCommandPermission().toString())
                                .setDescription(cloudCommand.getCommandMeta().getOrDefault("description", ""))
                                .build());
        this.command = command;
        this.manager = manager;
        this.cloudCommand = cloudCommand;
    }

    @Override
    public boolean execute(final CommandSender commandSender,
                           final String commandLabel,
                           final String[] strings) {
        /* Join input */
        final StringBuilder builder = new StringBuilder(this.command.getName());
        for (final String string : strings) {
            builder.append(" ").append(string);
        }
        final C sender = this.manager.getCommandSenderMapper().apply(commandSender);
        this.manager.executeCommand(sender,
                                    builder.toString())
                    .whenComplete(((commandResult, throwable) -> {
                        if (throwable != null) {
                            if (throwable instanceof CompletionException) {
                                throwable = throwable.getCause();
                            }
                            final Throwable finalThrowable = throwable;
                            if (throwable instanceof InvalidSyntaxException) {
                                this.manager.handleException(sender,
                                                             InvalidSyntaxException.class,
                                                             (InvalidSyntaxException) throwable, (c, e) ->
                                                                     commandSender.sendMessage(
                                                                             "Invalid Command Syntax. "
                                                                                     + "Correct command syntax is: "
                                                                                     + "/"
                                                                                     + ((InvalidSyntaxException) finalThrowable)
                                                                                     .getCorrectSyntax())
                                );
                            } else if (throwable instanceof InvalidCommandSenderException) {
                                this.manager.handleException(sender,
                                                             InvalidCommandSenderException.class,
                                                             (InvalidCommandSenderException) throwable, (c, e) ->
                                                                     commandSender.sendMessage(finalThrowable.getMessage())
                                );
                            } else if (throwable instanceof NoPermissionException) {
                                this.manager.handleException(sender,
                                                             NoPermissionException.class,
                                                             (NoPermissionException) throwable, (c, e) ->
                                                                     commandSender.sendMessage(MESSAGE_NO_PERMS)
                                );
                            } else if (throwable instanceof NoSuchCommandException) {
                                this.manager.handleException(sender,
                                                             NoSuchCommandException.class,
                                                             (NoSuchCommandException) throwable, (c, e) ->
                                                                     commandSender.sendMessage(MESSAGE_UNKNOWN_COMMAND)
                                );
                            } else if (throwable instanceof ArgumentParseException) {
                                this.manager.handleException(sender,
                                                             ArgumentParseException.class,
                                                             (ArgumentParseException) throwable, (c, e) ->
                                                                     commandSender.sendMessage(
                                                                             "Invalid Command Argument: "
                                                                                     + finalThrowable.getCause().getMessage())
                                );
                            } else {
                                commandSender.sendMessage(throwable.getMessage());
                                throwable.printStackTrace();
                            }
                        }
                    }));
        return true;
    }

}
