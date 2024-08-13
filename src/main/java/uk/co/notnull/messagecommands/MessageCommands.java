package uk.co.notnull.messagecommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;
import static io.papermc.paper.command.brigadier.argument.ArgumentTypes.*;


@SuppressWarnings("UnstableApiUsage")
public class MessageCommands extends JavaPlugin implements Listener {
	private final Map<String, MessageCommand> commands = new HashMap<>();

	@Override
	public void onEnable() {
		loadCommands();

		this.getServer().getPluginManager().registerEvents(this, this);

		LifecycleEventManager<Plugin> manager = getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> registerCommands(event.registrar()));
	}

	private void loadCommands() {
		commands.clear();
		saveDefaultConfig();
		reloadConfig();

		ConfigurationSection commandConfig = getConfig().getConfigurationSection("commands");

		if(commandConfig != null) {
			commandConfig.getKeys(false).forEach(key -> {
				if(commandConfig.isConfigurationSection(key)) {
					try {
						commands.put(key,
									 MessageCommand.fromConfig(
											 key, Objects.requireNonNull(commandConfig.getConfigurationSection(key))));

						getLogger().info("Registering command " + key);
					} catch(IllegalArgumentException e) {
						getLogger().warning("Failed to register command " + key + ": " + e.getMessage());
					}
				}
			});
		}
	}

	private void registerCommands(Commands commands) {
		LiteralCommandNode<CommandSourceStack> reloadCommand = literal("reload")
				.requires(source -> source.getSender().hasPermission("messagecommands.reload"))
				.executes(ctx -> {
					try {
						loadCommands();
						getServer().reloadData();
						ctx.getSource().getSender().sendMessage(
								Component.text("Config reloaded").color(NamedTextColor.GREEN));
					} catch (Exception e) {
						ctx.getSource().getSender().sendMessage(
								Component.text("An error occurred while reloading the config. Check the console for details")
										.color(NamedTextColor.GREEN));
					}

					return Command.SINGLE_SUCCESS;
				}).build();

		LiteralCommandNode<CommandSourceStack> runCommand = literal("run")
				.requires(source -> source.getSender().hasPermission("messagecommands.run"))
				.then(argument("command", StringArgumentType.word())
							  .executes(this::onRun)
							  .then(argument("player", players())
											.requires(source -> source.getSender()
													.hasPermission("messagecommands.run.other"))
											.executes(this::onRunWithPlayer)))
				.build();

		LiteralCommandNode<CommandSourceStack> topCommand = literal("messagecommands")
				.requires(source ->
								  source.getSender().hasPermission("messagecommands.reload")
										  || source.getSender().hasPermission("messagecommands.run"))
				.then(reloadCommand)
				.then(runCommand)
				.build();

		commands.register(topCommand, "Main command for MessageCommands");

		this.commands.forEach((name, command) -> {
			LiteralCommandNode<CommandSourceStack> theCommand = literal(name)
					.requires(ctx -> !command.hasPermission() || ctx.getSender().hasPermission(command.permission()))
					.executes(ctx -> {
						ctx.getSource().getSender().sendMessage(command.response());
						return Command.SINGLE_SUCCESS;
					}).build();

			commands.register(theCommand, command.description());
		});
	}

	private int onRun(CommandContext<CommandSourceStack> ctx) {
		String command = ctx.getArgument("command", String.class);
		runCommand(command, ctx.getSource().getSender());

		return Command.SINGLE_SUCCESS;
	}

	private int onRunWithPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		List<Player> players = ctx.getArgument("players", PlayerSelectorArgumentResolver.class)
				.resolve(ctx.getSource());
		String command = ctx.getArgument("command", String.class);

		for(Player player : players) {
			runCommand(command, player);
			ctx.getSource().getSender().sendMessage(Component.text("Message sent to " + player.getName())
															.color(NamedTextColor.GREEN));
		}

		return Command.SINGLE_SUCCESS;
	}

	private void runCommand(String command, CommandSender sender) {
		if(commands.containsKey(command)) {
			sender.sendMessage(commands.get(command).response());
		}
	}
}
