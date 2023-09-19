package net.geforcemods.securitycraft.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import net.geforcemods.securitycraft.ClientHandler;
import net.geforcemods.securitycraft.api.Owner;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.server.ServerLifecycleHooks;

public class TeamUtils {
	private TeamUtils() {}

	/**
	 * Checks if two given players are on the same scoreboard/FTB Teams team
	 *
	 * @param owner1 The first owner object representing a player
	 * @param owner2 The second owner object representing a player
	 * @return true if both players are on the same team, false otherwise
	 */
	public static boolean areOnSameTeam(Owner owner1, Owner owner2) {
		if (owner1.equals(owner2))
			return true;

		PlayerTeam team = TeamUtils.getVanillaTeamFromPlayer(owner1.getName());

		return team != null && team.getPlayers().contains(owner2.getName());
	}

	/**
	 * Gets the scoreboard team the given player is on
	 *
	 * @param playerName The player whose team to get
	 * @return The team the given player is on. null if the player is not part of a team
	 */
	public static PlayerTeam getVanillaTeamFromPlayer(String playerName) {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

		if (server != null)
			return server.getScoreboard().getPlayersTeam(playerName);
		else
			return ClientHandler.getClientPlayer().getScoreboard().getPlayersTeam(playerName);
	}

	/**
	 * Gets a representation containing the team's name and color
	 *
	 * @param owner The owner whose team to get
	 * @return The {@link TeamRepresentation} of the owner's team, {@code null} if they are not part of a team
	 */
	public static TeamRepresentation getTeamRepresentation(Owner owner) {
		PlayerTeam team = TeamUtils.getVanillaTeamFromPlayer(owner.getName());

		if (team != null) {
			Integer color = team.getColor().getColor();

			return new TeamRepresentation(team.getDisplayName().getString(), color == null ? ChatFormatting.GRAY.getColor() : color);
		}

		return null;
	}

	/**
	 * Gets all players that are in a team and currently online
	 *
	 * @param server The server
	 * @param owner The owner whose team to get the players of
	 * @return A list containing all online players who are in the same team as the owner
	 */
	public static Collection<ServerPlayer> getOnlinePlayersInTeam(MinecraftServer server, Owner owner) {
		PlayerTeam team = TeamUtils.getVanillaTeamFromPlayer(owner.getName());

		if (team != null)
			return team.getPlayers().stream().map(server.getPlayerList()::getPlayerByName).filter(Objects::nonNull).toList();

		return new ArrayList<>();
	}

	public record TeamRepresentation(String name, int color) {}
}
