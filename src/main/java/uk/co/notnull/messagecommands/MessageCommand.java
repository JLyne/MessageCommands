package uk.co.notnull.messagecommands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record MessageCommand(@NotNull String name, @NotNull Component response, @Nullable String description, @Nullable String permission) {
	public static MessageCommand fromConfig(String name, ConfigurationSection config) {
		String description = config.getString("description");
		String permission = config.getString("permission");
		Component response = config.getRichMessage("response");

		if(response == null) {
			throw new IllegalArgumentException("Command must have a response defined");
		}

		return new MessageCommand(name, response, description, permission);
	}

	public boolean hasPermission() {
		return permission != null;
	}
}
