package me.f0reach.mathgo.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.f0reach.mathgo.MathGoPlugin;
import me.f0reach.mathgo.game.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MathGoCommand {
    private MathGoCommand() {}

    public static LiteralCommandNode<CommandSourceStack> build(MathGoPlugin plugin) {
        return Commands.literal("mathgo")
                .then(Commands.literal("join").executes(ctx -> {
                    Player p = requirePlayer(ctx.getSource().getSender());
                    if (p == null) return 0;
                    GameManager m = plugin.gameManager();
                    if (m != null) m.join(p);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("start").executes(ctx -> {
                    Player p = requirePlayer(ctx.getSource().getSender());
                    if (p == null) return 0;
                    GameManager m = plugin.gameManager();
                    if (m != null) m.start(p);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("leave").executes(ctx -> {
                    Player p = requirePlayer(ctx.getSource().getSender());
                    if (p == null) return 0;
                    GameManager m = plugin.gameManager();
                    if (m != null) m.leave(p);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("stop").executes(ctx -> {
                    Player p = requirePlayer(ctx.getSource().getSender());
                    if (p == null) return 0;
                    GameManager m = plugin.gameManager();
                    if (m != null) m.stop(p);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("reload").executes(ctx -> {
                    plugin.reloadMathGo();
                    ctx.getSource().getSender().sendMessage(Component.text("MathGo: config reloaded.",
                            NamedTextColor.GREEN));
                    return Command.SINGLE_SUCCESS;
                }))
                .build();
    }

    private static Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player p) return p;
        sender.sendMessage(Component.text("プレイヤー専用のコマンドです。", NamedTextColor.RED));
        return null;
    }
}
