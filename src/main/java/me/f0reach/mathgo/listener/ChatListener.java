package me.f0reach.mathgo.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.f0reach.mathgo.MathGoPlugin;
import me.f0reach.mathgo.game.GameManager;
import me.f0reach.mathgo.game.GameSession;
import me.f0reach.mathgo.game.GameState;
import me.f0reach.mathgo.quiz.AnswerJudge;
import me.f0reach.mathgo.ui.Messages;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.OptionalLong;

public final class ChatListener implements Listener {
    private final MathGoPlugin plugin;

    public ChatListener(MathGoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        GameManager manager = plugin.gameManager();
        if (manager == null) return;
        GameSession session = manager.sessionOf(player);
        if (session == null) return;
        if (session.state() != GameState.ANSWERING) return;

        event.setCancelled(true);
        String raw = PlainTextComponentSerializer.plainText().serialize(event.message());
        OptionalLong parsed = AnswerJudge.parseInt(raw);
        if (parsed.isEmpty()) {
            player.sendActionBar(Messages.get("game.answer.invalid_number"));
            return;
        }
        long value = parsed.getAsLong();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            GameManager m = plugin.gameManager();
            if (m != null) m.submitAnswer(player, value);
        });
    }
}
