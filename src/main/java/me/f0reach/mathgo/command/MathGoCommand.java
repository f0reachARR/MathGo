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
import me.f0reach.mathgo.ui.Messages;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockVector;

import static me.f0reach.mathgo.ui.Messages.get;
import static me.f0reach.mathgo.ui.Messages.n;
import static me.f0reach.mathgo.ui.Messages.u;

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
                    ctx.getSource().getSender().sendMessage(get("command.reload_success"));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("top")
                        .executes(ctx -> showTop(plugin, ctx.getSource().getSender(), GameRule.STAGE_CLEAR, 10))
                        .then(Commands.literal("stage").executes(ctx ->
                                showTop(plugin, ctx.getSource().getSender(), GameRule.STAGE_CLEAR, 10)))
                        .then(Commands.literal("stage_clear").executes(ctx ->
                                showTop(plugin, ctx.getSource().getSender(), GameRule.STAGE_CLEAR, 10)))
                        .then(Commands.literal("survival").executes(ctx ->
                                showTop(plugin, ctx.getSource().getSender(), GameRule.SURVIVAL, 10))))
                .then(buildTemplateTree(plugin))
                .build();
    }

    private static int showTop(MathGoPlugin plugin, CommandSender sender, GameRule rule, int limit) {
        var repo = plugin.scoreRepository();
        if (repo == null) {
            sender.sendMessage(get("top.no_database"));
            return 0;
        }
        sender.sendMessage(get("top.header", n("limit", limit), u("rule", ruleLabel(rule))));
        var future = repo.topAsync(rule, limit);
        me.f0reach.mathgo.db.ScoreRepository.onMain(plugin, future, list -> {
            if (list.isEmpty()) {
                sender.sendMessage(get("top.empty"));
                return;
            }
            int rank = 1;
            for (var rec : list) {
                sender.sendMessage(get("top.entry",
                        n("rank", rank++),
                        u("name", rec.playerName()),
                        n("score", rec.score()),
                        n("correct", rec.correctCount()),
                        n("combo", rec.maxCombo()),
                        n("duration", rec.durationSeconds())));
            }
        });
        return Command.SINGLE_SUCCESS;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildTemplateTree(
            MathGoPlugin plugin) {
        return Commands.literal("template")
                .then(Commands.literal("wand").executes(ctx -> {
                    Player p = requirePlayer(ctx.getSource().getSender());
                    if (p == null) return 0;
                    TemplateAuthoringService svc = plugin.templateAuthoringService();
                    p.getInventory().addItem(svc.createWand());
                    p.sendMessage(get("template.wand.received"));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("items")
                        .then(Commands.argument("role", StringArgumentType.word())
                                .suggests(MathGoCommand::suggestRoles)
                                .executes(ctx -> giveMarkerItems(plugin, ctx.getSource().getSender(),
                                        StringArgumentType.getString(ctx, "role")))))
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
                    p.sendMessage(get("template.draft.cleared"));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("save")
                        .then(Commands.argument("role", StringArgumentType.word())
                                .suggests(MathGoCommand::suggestRoles)
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
                    ctx.getSource().getSender().sendMessage(get("template.reload_success"));
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
        p.sendMessage(get("template.pos_set",
                u("label", isPos1 ? "pos1" : "pos2"),
                n("x", loc.getBlockX()),
                n("y", loc.getBlockY()),
                n("z", loc.getBlockZ())));
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
        p.sendMessage(get("template.entry_exit_set",
                u("label", isEntry ? "entry" : "exit"),
                n("x", v.getBlockX()),
                n("y", v.getBlockY()),
                n("z", v.getBlockZ()),
                u("facing", facing.name())));
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
        p.sendMessage(get("template.anchor_set",
                u("label", isStop ? "stop" : "display"),
                n("x", v.getBlockX()),
                n("y", v.getBlockY()),
                n("z", v.getBlockZ())));
        return Command.SINGLE_SUCCESS;
    }

    private static int showDraft(MathGoPlugin plugin, CommandSender sender) {
        Player p = requirePlayer(sender);
        if (p == null) return 0;
        TemplateDraft d = plugin.templateAuthoringService().draftOf(p);
        p.sendMessage(get("template.draft.header"));
        p.sendMessage(get("template.draft.pos1", u("v", String.valueOf(d.pos1()))));
        p.sendMessage(get("template.draft.pos2", u("v", String.valueOf(d.pos2()))));
        p.sendMessage(get("template.draft.entry",
                u("v", String.valueOf(d.entry())),
                u("facing", String.valueOf(d.entryFacing()))));
        p.sendMessage(get("template.draft.exit",
                u("v", String.valueOf(d.exit())),
                u("facing", String.valueOf(d.exitFacing()))));
        p.sendMessage(get("template.draft.stop", u("v", String.valueOf(d.stop()))));
        p.sendMessage(get("template.draft.display", u("v", String.valueOf(d.display()))));
        return Command.SINGLE_SUCCESS;
    }

    private static int doSave(MathGoPlugin plugin, CommandSender sender, String roleName, String id, int weight) {
        Player p = requirePlayer(sender);
        if (p == null) return 0;
        SegmentRole role;
        try {
            role = SegmentRole.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            p.sendMessage(get("template.save.unknown_role", u("name", roleName)));
            return 0;
        }
        var result = plugin.templateAuthoringService().save(p, role, id, weight);
        String key = result.ok() ? "template.save.result_ok" : "template.save.result_err";
        p.sendMessage(get(key, u("msg", result.message())));
        if (result.ok()) {
            plugin.reloadTemplates();
        }
        return Command.SINGLE_SUCCESS;
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestRoles(
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        String prefix = builder.getRemaining().toLowerCase();
        for (SegmentRole r : SegmentRole.values()) {
            String name = r.name().toLowerCase();
            if (name.startsWith(prefix)) builder.suggest(name);
        }
        return builder.buildFuture();
    }

    private static int giveMarkerItems(MathGoPlugin plugin, CommandSender sender, String roleName) {
        Player p = requirePlayer(sender);
        if (p == null) return 0;
        SegmentRole role;
        try {
            role = SegmentRole.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            p.sendMessage(get("template.save.unknown_role", u("name", roleName)));
            return 0;
        }
        TemplateAuthoringService svc = plugin.templateAuthoringService();
        for (var marker : svc.markersFor(role)) {
            p.getInventory().addItem(svc.createMarker(marker));
        }
        p.sendMessage(get("template.items.received", u("role", role.name().toLowerCase())));
        return Command.SINGLE_SUCCESS;
    }

    private static String ruleLabel(GameRule rule) {
        return Messages.getRaw(
                rule == GameRule.SURVIVAL ? "game.rule_label.survival" : "game.rule_label.stage_clear",
                rule == GameRule.SURVIVAL ? "サバイバル" : "ステージクリア");
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
        sender.sendMessage(get("command.player_only"));
        return null;
    }
}
