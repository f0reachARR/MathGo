package me.f0reach.mathgo.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.f0reach.mathgo.MathGoPlugin;
import me.f0reach.mathgo.game.GameManager;
import me.f0reach.mathgo.game.GameRule;
import me.f0reach.mathgo.track.Direction;
import me.f0reach.mathgo.track.SegmentRole;
import me.f0reach.mathgo.track.template.TemplateAuthoringService;
import me.f0reach.mathgo.track.template.TemplateDraft;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockVector;

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
                .then(Commands.literal("rule")
                        .then(Commands.literal("stage").executes(ctx -> {
                            Player p = requirePlayer(ctx.getSource().getSender());
                            if (p == null) return 0;
                            GameManager m = plugin.gameManager();
                            if (m != null) m.setRule(p, GameRule.STAGE_CLEAR);
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(Commands.literal("stage_clear").executes(ctx -> {
                            Player p = requirePlayer(ctx.getSource().getSender());
                            if (p == null) return 0;
                            GameManager m = plugin.gameManager();
                            if (m != null) m.setRule(p, GameRule.STAGE_CLEAR);
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(Commands.literal("survival").executes(ctx -> {
                            Player p = requirePlayer(ctx.getSource().getSender());
                            if (p == null) return 0;
                            GameManager m = plugin.gameManager();
                            if (m != null) m.setRule(p, GameRule.SURVIVAL);
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(Commands.literal("reload").executes(ctx -> {
                    plugin.reloadMathGo();
                    ctx.getSource().getSender().sendMessage(Component.text("MathGo: config reloaded.",
                            NamedTextColor.GREEN));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(buildTemplateTree(plugin))
                .build();
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildTemplateTree(
            MathGoPlugin plugin) {
        return Commands.literal("template")
                .then(Commands.literal("wand").executes(ctx -> {
                    Player p = requirePlayer(ctx.getSource().getSender());
                    if (p == null) return 0;
                    TemplateAuthoringService svc = plugin.templateAuthoringService();
                    p.getInventory().addItem(svc.createWand());
                    p.sendMessage(Component.text("テンプレ設計者の杖を入手しました。",
                            NamedTextColor.GREEN));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("pos1").executes(ctx -> setPos(plugin, ctx.getSource().getSender(), true)))
                .then(Commands.literal("pos2").executes(ctx -> setPos(plugin, ctx.getSource().getSender(), false)))
                .then(Commands.literal("entry").executes(ctx -> setEntryOrExit(plugin, ctx.getSource().getSender(), true)))
                .then(Commands.literal("exit").executes(ctx -> setEntryOrExit(plugin, ctx.getSource().getSender(), false)))
                .then(Commands.literal("anchor")
                        .then(Commands.literal("stop").executes(ctx -> setAnchor(plugin, ctx.getSource().getSender(), true)))
                        .then(Commands.literal("display").executes(ctx -> setAnchor(plugin, ctx.getSource().getSender(), false))))
                .then(Commands.literal("show").executes(ctx -> showDraft(plugin, ctx.getSource().getSender())))
                .then(Commands.literal("clear").executes(ctx -> {
                    Player p = requirePlayer(ctx.getSource().getSender());
                    if (p == null) return 0;
                    plugin.templateAuthoringService().draftOf(p).clear();
                    p.sendMessage(Component.text("draft をクリアしました。", NamedTextColor.GRAY));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("save")
                        .then(Commands.argument("role", StringArgumentType.word())
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> doSave(plugin, ctx.getSource().getSender(),
                                                StringArgumentType.getString(ctx, "role"),
                                                StringArgumentType.getString(ctx, "id"), 1))
                                        .then(Commands.argument("weight", IntegerArgumentType.integer(1))
                                                .executes(ctx -> doSave(plugin, ctx.getSource().getSender(),
                                                        StringArgumentType.getString(ctx, "role"),
                                                        StringArgumentType.getString(ctx, "id"),
                                                        IntegerArgumentType.getInteger(ctx, "weight")))))))
                .then(Commands.literal("reload").executes(ctx -> {
                    plugin.reloadTemplates();
                    ctx.getSource().getSender().sendMessage(Component.text("テンプレートを再ロードしました。",
                            NamedTextColor.GREEN));
                    return Command.SINGLE_SUCCESS;
                }));
    }

    private static int setPos(MathGoPlugin plugin, CommandSender sender, boolean isPos1) {
        Player p = requirePlayer(sender);
        if (p == null) return 0;
        Location loc = p.getLocation().getBlock().getLocation();
        TemplateDraft draft = plugin.templateAuthoringService().draftOf(p);
        if (isPos1) draft.setPos1(loc);
        else draft.setPos2(loc);
        p.sendMessage(Component.text((isPos1 ? "pos1" : "pos2") + ": ("
                + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")",
                NamedTextColor.AQUA));
        return Command.SINGLE_SUCCESS;
    }

    private static int setEntryOrExit(MathGoPlugin plugin, CommandSender sender, boolean isEntry) {
        Player p = requirePlayer(sender);
        if (p == null) return 0;
        Location loc = p.getLocation();
        BlockVector v = new BlockVector(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        Direction facing = yawToDirection(loc.getYaw());
        TemplateDraft draft = plugin.templateAuthoringService().draftOf(p);
        if (isEntry) draft.setEntry(v, facing);
        else draft.setExit(v, facing);
        p.sendMessage(Component.text((isEntry ? "entry" : "exit") + ": ("
                + v.getBlockX() + ", " + v.getBlockY() + ", " + v.getBlockZ() + ") facing " + facing,
                NamedTextColor.AQUA));
        return Command.SINGLE_SUCCESS;
    }

    private static int setAnchor(MathGoPlugin plugin, CommandSender sender, boolean isStop) {
        Player p = requirePlayer(sender);
        if (p == null) return 0;
        Location loc = p.getLocation();
        BlockVector v = new BlockVector(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        TemplateDraft draft = plugin.templateAuthoringService().draftOf(p);
        if (isStop) draft.setStop(v);
        else draft.setDisplay(v);
        p.sendMessage(Component.text("anchor " + (isStop ? "stop" : "display") + ": ("
                + v.getBlockX() + ", " + v.getBlockY() + ", " + v.getBlockZ() + ")",
                NamedTextColor.AQUA));
        return Command.SINGLE_SUCCESS;
    }

    private static int showDraft(MathGoPlugin plugin, CommandSender sender) {
        Player p = requirePlayer(sender);
        if (p == null) return 0;
        TemplateDraft d = plugin.templateAuthoringService().draftOf(p);
        p.sendMessage(Component.text("--- Template Draft ---", NamedTextColor.GOLD));
        p.sendMessage(Component.text("pos1: " + d.pos1(), NamedTextColor.GRAY));
        p.sendMessage(Component.text("pos2: " + d.pos2(), NamedTextColor.GRAY));
        p.sendMessage(Component.text("entry: " + d.entry() + " " + d.entryFacing(), NamedTextColor.GRAY));
        p.sendMessage(Component.text("exit: " + d.exit() + " " + d.exitFacing(), NamedTextColor.GRAY));
        p.sendMessage(Component.text("stop: " + d.stop(), NamedTextColor.GRAY));
        p.sendMessage(Component.text("display: " + d.display(), NamedTextColor.GRAY));
        return Command.SINGLE_SUCCESS;
    }

    private static int doSave(MathGoPlugin plugin, CommandSender sender, String roleName, String id, int weight) {
        Player p = requirePlayer(sender);
        if (p == null) return 0;
        SegmentRole role;
        try {
            role = SegmentRole.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            p.sendMessage(Component.text("unknown role: " + roleName, NamedTextColor.RED));
            return 0;
        }
        var result = plugin.templateAuthoringService().save(p, role, id, weight);
        p.sendMessage(Component.text(result.message(), result.ok() ? NamedTextColor.GREEN : NamedTextColor.RED));
        if (result.ok()) {
            plugin.reloadTemplates();
        }
        return Command.SINGLE_SUCCESS;
    }

    private static Direction yawToDirection(float yaw) {
        // Minecraft yaw: 0 = south, 90 = west, 180 = north, -90 / 270 = east.
        float y = ((yaw % 360) + 360) % 360;
        if (y >= 45 && y < 135) return Direction.WEST;
        if (y >= 135 && y < 225) return Direction.NORTH;
        if (y >= 225 && y < 315) return Direction.EAST;
        return Direction.SOUTH;
    }

    private static Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player p) return p;
        sender.sendMessage(Component.text("プレイヤー専用のコマンドです。", NamedTextColor.RED));
        return null;
    }
}
