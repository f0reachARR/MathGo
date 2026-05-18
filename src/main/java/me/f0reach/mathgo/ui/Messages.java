package me.f0reach.mathgo.ui;

import me.f0reach.mathgo.MathGoPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * MiniMessage-based message helper backed by {@code messages.yml}. All user-facing text lives in the
 * YAML file so it can be edited or localized without rebuilding. See {@code docs/adventure.md}.
 *
 * <p>Lookup pattern: {@code Messages.get("game.question.chat", u("expr", "1+2"))} reads
 * {@code game.question.chat} (a MiniMessage template) and applies the named placeholders.
 *
 * <p>Initialization is one-shot per plugin enable / reload via {@link #load(MathGoPlugin)}.
 */
public final class Messages {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    @Nullable private static FileConfiguration config;
    @Nullable private static Logger logger;

    private Messages() {}

    /**
     * Loads (or reloads) {@code messages.yml} from the plugin data folder. Saves the bundled
     * resource as the default file if missing, and fills in any missing keys from the bundled
     * resource so upgrades automatically pick up new templates.
     */
    public static void load(MathGoPlugin plugin) {
        plugin.saveResource("messages.yml", false);
        File file = new File(plugin.getDataFolder(), "messages.yml");
        FileConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        // Fallback defaults from the bundled jar resource so newly-added keys resolve even when
        // the user's on-disk file predates the version change.
        try (InputStream in = plugin.getResource("messages.yml")) {
            if (in != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(in, StandardCharsets.UTF_8));
                loaded.setDefaults(defaults);
                loaded.options().copyDefaults(false);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Messages: failed to load bundled defaults: " + e.getMessage());
        }
        config = loaded;
        logger = plugin.getLogger();
    }

    /** Returns the rendered Component for {@code key} with the given resolvers. */
    public static Component get(String key, TagResolver... resolvers) {
        String template = (config != null) ? config.getString(key) : null;
        if (template == null) {
            if (logger != null) logger.warning("Messages: missing key '" + key + "'");
            return Component.text("[missing:" + key + "]");
        }
        return resolvers.length == 0
                ? MM.deserialize(template)
                : MM.deserialize(template, resolvers);
    }

    /**
     * Reads a raw string from messages.yml (no MiniMessage parsing). Used for inline values like
     * rule labels that need to be embedded into a parent template via {@link #u}.
     */
    public static String getRaw(String key, String fallback) {
        String s = (config != null) ? config.getString(key) : null;
        return s != null ? s : fallback;
    }

    /**
     * Inline MiniMessage parse, e.g. for log lines or one-off cases that don't belong in YAML.
     * Prefer {@link #get(String, TagResolver...)} for anything user-facing.
     */
    public static Component mm(String template, TagResolver... resolvers) {
        return resolvers.length == 0
                ? MM.deserialize(template)
                : MM.deserialize(template, resolvers);
    }

    /** Resolver for a plain string value (rendered as literal text, no MiniMessage interpretation). */
    public static TagResolver u(String key, String value) {
        return Placeholder.unparsed(key, value == null ? "" : value);
    }

    /** Resolver for a numeric value. */
    public static TagResolver n(String key, long value) {
        return Placeholder.unparsed(key, Long.toString(value));
    }

    /** Resolver for a Component value (preserves any styling on the child). */
    public static TagResolver c(String key, Component value) {
        return Placeholder.component(key, value == null ? Component.empty() : value);
    }
}
