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
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.entity.Message;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.reaction.ReactionEmoji.Custom;
import discord4j.core.object.util.Snowflake;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class BirdUpBot {

	//file names
	private static final String GUILD_TOGGLES_CSV = "guildToggles.csv";
	private static final String GUILD_EMOJI_CSV = "guildEmoji.csv";

	//main discord client
	private static DiscordClient client;

	//map of serverId to status
	private static Map<Snowflake, Boolean> guildStatus;


	//map of emoji name to emoji ID
	private static Map<String, Pair<Snowflake, Snowflake>> emojiMap;

	//map of command phrases to commands
	private static Map<String, Command> commandMap = new HashMap<String, Command>();

	public static void main(String[] args) {
		//args[0] is the token provided by discord unique to the bot
		BirdUpBot birdUp = new BirdUpBot(args[0]);

		//trigger on login
		client.getEventDispatcher().on(ReadyEvent.class).flatMap(event -> birdUp.setup(event)).subscribe();

		//trigger on message sent
		client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event -> birdUp.processMessage(event));

		//login
		client.login().block();
	}


	private void processMessage(MessageCreateEvent event) {
		System.out.println();
		//check if message matches command in the map
		Mono.justOrEmpty(event.getMessage().getContent()).flatMap(content -> Flux.fromIterable(commandMap.entrySet())
				.filter(entry -> content.startsWith("!bird" + entry.getKey()) || content.startsWith("hey alfred, " + entry.getKey()))
				.flatMap(entey -> entey.getValue().execute(event)).next()).subscribe();

		//get birdUpEmoji
		ReactionEmoji birdUpEmoji = ReactionEmoji.custom(client.getGuildEmojiById(emojiMap.get("birdup").first, emojiMap.get("birdup").second).block());

		//check if message contains birdup
		Mono.justOrEmpty(event.getMessage()).filter(message -> message.getContent().orElse("").toLowerCase().contains("birdup")).subscribe(message -> message.addReaction(birdUpEmoji).block());
	}

	//event.getMessage().addReaction(client.getGuildEmojiById(emojiMap.get("birdup").first, emojiMap.get("birdup").second));
	//.flatMap(goodMessage -> goodMessage.getMessage().addReaction(birdUpEmoji));

	//make client from token
	private BirdUpBot(String token) {
		//initialize variables
		client = new DiscordClientBuilder(token).build();
		guildStatus = new HashMap<Snowflake, Boolean>();
		emojiMap = new HashMap<String, Pair<Snowflake,Snowflake>>();
		Kitty.initialize();

		//read files
		readGuildStatus();
		readConstantGuildsEmoji();

		//populate command list
		this.populateCommandMap();
	}

	private void populateCommandMap() {
		//!bird[key] -> command to run
		commandMap.put("raid", event -> Destiny.executeRaid(event).then());
		commandMap.put("kitty", event -> Kitty.execute(event).then());
		commandMap.put("off", event -> Control.executeOff(event).then());
		commandMap.put("all", event -> Control.executeAll(event).then());
		commandMap.put("up", event -> Control.executeOn(event).then());
		commandMap.put("shutdown", event -> Control.executeShutDown(event).then());
		commandMap.put("status", event -> Control.executeStatus(event).then());
		commandMap.put("ip", event -> Control.executeIp(event).then());
		commandMap.put("help", event -> Control.executeHelp(event).then());
		commandMap.put("roles", event -> Guild.executeRolls(event).then());
		commandMap.put("emote add", event -> Guild.executeEmojiUpload(event).then());
		commandMap.put("jail", event -> Guild.executeJail(event).then());
		commandMap.put("clown", event -> Guild.executeClown(event).then());

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

	public static Map<Snowflake, Boolean> getGuildStatus() {
		return guildStatus;
	}


	public static void setGuildStatus(Map<Snowflake, Boolean> guildStatus) {
		BirdUpBot.guildStatus = guildStatus;
	}
}
