package birdUp;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

public class BirdUpBot {

	private static final String GUILD_TOGGLES_CSV = "guildToggles.csv";

	private static final String GUILD_EMOJI_CSV = "guildEmoji.csv";

	//main discord client
	private static DiscordClient client;

	//map of serverId to status
	private static Map<Snowflake, Boolean> guildStatus;

	//map of emoji name to emoji ID
	private static Map<String, Pair<Snowflake, Snowflake>> emojiMap;

	public static void main(String[] args) {
		//args[0] is the token provided by discord unique to the bot
		BirdUpBot birdUp = new BirdUpBot(args[0]);

		//trigger on login
		client.getEventDispatcher().on(ReadyEvent.class).flatMap(event -> birdUp.setup(event)).subscribe();

		//login
		client.login().block();
	}

	//make client from token
	private BirdUpBot(String token) {
		//initialize variables
		client = new DiscordClientBuilder(token).build();
		guildStatus = new HashMap<Snowflake, Boolean>();
		emojiMap = new HashMap<String, Pair<Snowflake,Snowflake>>();
		
		//read files
		readGuildStatus();
		readConstantGuildsEmoji();
	}

	private void readConstantGuildsEmoji() {
		try {
			Scanner constants = new Scanner(new FileReader(GUILD_EMOJI_CSV));
			while (constants.hasNext()) {
				String[] line = constants.nextLine().split(",");
				
				//expected format: name,guildID,emojiID
				emojiMap.put(line[0], new Pair<Snowflake, Snowflake>(Snowflake.of(line[1]), Snowflake.of(line[2])));
			}
			constants.close();
		} catch (FileNotFoundException e) {
			System.err.println("Cannot read constants file \"" + GUILD_EMOJI_CSV + "\"");
			//exit if file cannot be read
			System.exit(1);
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("guildEmoji csv format different than expected");
			//exit if bad config format
			System.exit(2);
		}

	}

	private void readGuildStatus() {
		//load config csv
		try {
			Scanner guilds = new Scanner(new FileReader(GUILD_TOGGLES_CSV));
			while (guilds.hasNext()) {
				String[] line = guilds.nextLine().split(",");
				guildStatus.put(Snowflake.of(line[0]), line[1].equalsIgnoreCase("1"));
			}
			guilds.close();
		} catch (FileNotFoundException e) {
			System.err.println("Cannot read config file \"" + GUILD_TOGGLES_CSV + "\"");
			//exit if file cannot be read
			System.exit(1);
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("Config format different than expected");
			//exit if bad config format
			System.exit(2);
		}
	}

	//At startup, set presence
	private Mono<Void> setup(ReadyEvent readyEvent) {
		//set presence
		Presence presence = Presence.online(Activity.playing("BirdUp!"));
		return Mono.justOrEmpty(readyEvent).flatMap(event -> event.getClient().updatePresence(presence));
	}

	public void exit() {
		//write server states to file
		try {
			BufferedWriter guildStatusOut = new BufferedWriter(new FileWriter(GUILD_TOGGLES_CSV));
			guildStatus.forEach((U, V) -> {
				try {
					guildStatusOut.write(U + "," + V);
				} catch (IOException e) {
					System.err.println("Error Saving guildToggles");
				}
			});
			guildStatusOut.close();
		} catch (IOException e) {
			System.err.println("Error Saving guildToggles");
		}
		
		//logout
		client.logout().block();
		System.exit(0);
	}
}
