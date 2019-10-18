package birdUp;

import java.io.FileNotFoundException;
import java.io.FileReader;
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

	//main discord client
	private static DiscordClient client;

	//map of serverId to status
	private static Map<Snowflake, Boolean> serverStatus;

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
		client = new DiscordClientBuilder(token).build();
		serverStatus = new HashMap<Snowflake, Boolean>();
		
		//load config csv
		try {
			Scanner in = new Scanner(new FileReader("guildToggles.csv"));
			while (in.hasNext()) {
				String[] line = in.nextLine().split(",");
				serverStatus.put(Snowflake.of(line[0]), line[1].equalsIgnoreCase("1"));
			}
			in.close();
		} catch (FileNotFoundException e) {
			System.err.println("Cannot read config file \"guildToggles.csv\"");
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


}
