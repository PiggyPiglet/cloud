//
// MIT License
//
// Copyright (c) 2020 Alexander Söderberg & Contributors
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
package cloud.commandframework.bukkit.parsers;

import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;

/**
 * cloud argument type that parses Bukkit {@link Enchantment enchantments}
 *
 * @param <C> Command sender type
 */
public class EnchantmentArgument<C> extends CommandArgument<C, Enchantment> {

    protected EnchantmentArgument(
            final boolean required,
            final @NonNull String name,
            final @NonNull String defaultValue,
            final @Nullable BiFunction<@NonNull CommandContext<C>, @NonNull String,
                    @NonNull List<@NonNull String>> suggestionsProvider
    ) {
        super(required, name, new EnchantmentParser<>(), defaultValue, Enchantment.class, suggestionsProvider);
    }

    /**
     * Create a new builder
     *
     * @param name Name of the argument
     * @param <C>  Command sender type
     * @return Created builder
     */
    public static <C> EnchantmentArgument.@NonNull Builder<C> newBuilder(final @NonNull String name) {
        return new EnchantmentArgument.Builder<>(name);
    }

    /**
     * Create a new required argument
     *
     * @param name Argument name
     * @param <C>  Command sender type
     * @return Created argument
     */
    public static <C> @NonNull CommandArgument<C, Enchantment> of(final @NonNull String name) {
        return EnchantmentArgument.<C>newBuilder(name).asRequired().build();
    }

    /**
     * Create a new optional argument
     *
     * @param name Argument name
     * @param <C>  Command sender type
     * @return Created argument
     */
    public static <C> @NonNull CommandArgument<C, Enchantment> optional(final @NonNull String name) {
        return EnchantmentArgument.<C>newBuilder(name).asOptional().build();
    }

    /**
     * Create a new optional argument with a default value
     *
     * @param name        Argument name
     * @param enchantment Default value
     * @param <C>         Command sender type
     * @return Created argument
     */
    public static <C> @NonNull CommandArgument<C, Enchantment> optional(
            final @NonNull String name,
            final @NonNull Enchantment enchantment
    ) {
        return EnchantmentArgument.<C>newBuilder(name).asOptionalWithDefault(enchantment.getKey().toString()).build();
    }

    public static final class Builder<C> extends CommandArgument.Builder<C, Enchantment> {

        protected Builder(final @NonNull String name) {
            super(Enchantment.class, name);
        }

    }

    public static final class EnchantmentParser<C> implements ArgumentParser<C, Enchantment> {

        @Override
        public @NonNull ArgumentParseResult<Enchantment> parse(
                final @NonNull CommandContext<C> commandContext,
                final @NonNull Queue<@NonNull String> inputQueue
        ) {
            final String input = inputQueue.peek();
            if (input == null) {
                return ArgumentParseResult.failure(new NullPointerException("No input was provided"));
            }

            final NamespacedKey key;
            if (input.contains(":")) {
                final String[] splitInput = input.split(":");
                //noinspection deprecation
                key = new NamespacedKey(splitInput[0], splitInput[1]);
            } else {
                key = NamespacedKey.minecraft(input);
            }

            final Enchantment enchantment = Enchantment.getByKey(key);
            if (enchantment == null) {
                return ArgumentParseResult.failure(new EnchantmentParseException(input));
            }
            inputQueue.remove();
            return ArgumentParseResult.success(enchantment);
        }

        @Override
        public @NonNull List<@NonNull String> suggestions(
                final @NonNull CommandContext<C> commandContext,
                final @NonNull String input
        ) {
            final List<String> completions = new ArrayList<>();
            for (Enchantment value : Enchantment.values()) {
                if (value.getKey().getNamespace().equals(NamespacedKey.MINECRAFT)) {
                    completions.add(value.getKey().getKey());
                } else {
                    completions.add(value.getKey().toString());
                }
            }
            return completions;
        }

    }


    public static final class EnchantmentParseException extends IllegalArgumentException {

        private final String input;

        /**
         * Construct a new EnchantmentParseException
         *
         * @param input Input
         */
        public EnchantmentParseException(final @NonNull String input) {
            this.input = input;
        }

        /**
         * Get the input
         *
         * @return Input
         */
        public @NonNull String getInput() {
            return this.input;
        }

        @Override
        public String getMessage() {
            return String.format("'%s' is not a valid enchantment", this.input);
        }

    }

}
