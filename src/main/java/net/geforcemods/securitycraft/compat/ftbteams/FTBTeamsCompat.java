package net.geforcemods.securitycraft.compat.ftbteams;

import java.util.ArrayList;
import java.util.Collection;

import net.geforcemods.securitycraft.api.Owner;
import net.geforcemods.securitycraft.util.TeamUtils.TeamRepresentation;
import net.minecraft.server.level.ServerPlayer;

public class FTBTeamsCompat {
	private FTBTeamsCompat() {}

	public static boolean areOnSameTeam(Owner owner1, Owner owner2) {
		//		try {
		//			API api = FTBTeamsAPI.api();
		//
		//			if (api.isManagerLoaded())
		//				return api.getManager().arePlayersInSameTeam(UUID.fromString(owner1.getUUID()), UUID.fromString(owner2.getUUID()));
		//			else if (api.isClientManagerLoaded()) {
		//				ClientTeamManager manager = api.getClientManager();
		//				Optional<KnownClientPlayer> optional1 = manager.getKnownPlayer(UUID.fromString(owner1.getUUID()));
		//
		//				if (optional1.isPresent()) {
		//					Optional<KnownClientPlayer> optional2 = manager.getKnownPlayer(UUID.fromString(owner2.getUUID()));
		//
		//					return optional2.isPresent() && optional1.get().teamId().equals(optional2.get().teamId());
		//				}
		//			}
		//		}
		//		catch (IllegalArgumentException e) {}

		return false;
	}

	public static TeamRepresentation getTeamRepresentation(Owner owner) {
		//		try {
		//			API api = FTBTeamsAPI.api();
		//			Team team = null;
		//
		//			if (api.isManagerLoaded()) {
		//				Optional<Team> optional = api.getManager().getTeamForPlayerID(UUID.fromString(owner.getUUID()));
		//
		//				if (optional.isPresent())
		//					team = optional.get();
		//			}
		//			else if (api.isClientManagerLoaded()) {
		//				ClientTeamManager manager = api.getClientManager();
		//				Optional<KnownClientPlayer> optionalPlayer = manager.getKnownPlayer(UUID.fromString(owner.getUUID()));
		//
		//				if (optionalPlayer.isPresent()) {
		//					Optional<Team> optionalTeam = manager.getTeamByID(optionalPlayer.get().teamId());
		//
		//					if (optionalTeam.isPresent())
		//						team = optionalTeam.get();
		//				}
		//			}
		//
		//			if (team != null)
		//				return new TeamRepresentation(team.getProperty(TeamProperties.DISPLAY_NAME), team.getProperty(TeamProperties.COLOR).toStyle().getColor().getValue());
		//		}
		//		catch (IllegalArgumentException e) {}

		return null;
	}

	public static Collection<ServerPlayer> getOnlinePlayersInTeam(Owner owner) {
		//		try {
		//			API api = FTBTeamsAPI.api();
		//
		//			if (api.isManagerLoaded()) {
		//				Optional<Team> optional = api.getManager().getTeamForPlayerID(UUID.fromString(owner.getUUID()));
		//
		//				if (optional.isPresent())
		//					return optional.get().getOnlineMembers();
		//			}
		//		}
		//		catch (IllegalArgumentException e) {}

		return new ArrayList<>();
	}
}
