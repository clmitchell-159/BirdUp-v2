package birdUp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.discordjson.json.gateway.StatusUpdate;
import discord4j.common.util.Snowflake;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class BirdUpBot {

	//file names
	private static final String GUILD_TOGGLES_CSV = "guildToggles.csv";
	private static final String GUILD_EMOJI_CSV = "guildEmoji.csv";
	private static final String TOKEN_FILE = "token";

	//main discord client
	private static GatewayDiscordClient client;

	//map of serverId to status
	private static Map<Snowflake, Boolean> guildStatus;


	//map of emoji name to emoji ID
	private static Map<String, Pair<Snowflake, Snowflake>> emojiMap;

	//map of command phrases to commands
	private static Map<String, Command> commandMap = new HashMap<String, Command>();

	public static void main(String[] args) {
		Scanner tokenFile;
		BirdUpBot birdUp;
		try {
			tokenFile = new Scanner(new FileReader(TOKEN_FILE));
			
			//token in stored in a token file
			birdUp = new BirdUpBot();
			
			//login
			client = DiscordClientBuilder.create(tokenFile.nextLine()).build().login().block();

			System.out.println("My id: " + client.getSelfId().asString());
			
			//trigger on login
			client.getEventDispatcher().on(ReadyEvent.class).flatMap(event -> birdUp.setup(event)).subscribe();

			//trigger on message sent
			client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event -> birdUp.processMessage(event));

			//trigger on reaction add event
			client.getEventDispatcher().on(ReactionAddEvent.class).subscribe(event -> birdUp.processReaction(event));
			
			//trigger on disconnect; do nothing
			client.onDisconnect().block();
		} catch (FileNotFoundException e) {
			System.err.println("Cannot read token file: " + e.getMessage());
			System.exit(-2);
		}
	}

	private Mono<Boolean> addGuildId(Snowflake guildId) {
		if (guildStatus.containsKey(guildId)) {
			return Mono.just(guildStatus.get(guildId));
		}
		else {
			guildStatus.put(guildId, false);
			return Mono.just(guildStatus.get(guildId));
		}
	}

	private void addBirdUpReaction(Message message) {
		//get birdUpEmoji
		ReactionEmoji birdUpEmoji = ReactionEmoji.custom(client.getGuildEmojiById(emojiMap.get("birdup").first, emojiMap.get("birdup").second).block());

		//add reaction
		message.addReaction(birdUpEmoji).block();
	}

	private void processMessage(MessageCreateEvent event) {
		Mono.justOrEmpty(event.getMessage())
			.filter(message -> message.getAuthor().map(user -> user.getId().asString().equals(client.getSelfId().asString())).orElse(false))
			.filter(message -> !message.getEmbeds().isEmpty())
	        .flatMap(message -> Destiny.addRaidReactions(message))
	        .subscribe();
		
		//check if message matches command in the map
		Mono.justOrEmpty(event.getMessage().getContent()).flatMap(content -> Flux.fromIterable(commandMap.entrySet())
				.filter(entry -> content.startsWith("!bird" + entry.getKey()) || content.startsWith("hey alfred, " + entry.getKey()))
				.flatMap(entey -> entey.getValue().execute(event)).next()).subscribe();

		//check if message guild is known, then check if all mode of the bot is enabled
		Mono.justOrEmpty(event.getGuildId()).flatMap(guildId -> addGuildId(guildId)).subscribe(value -> {
			if (value == true) {
				Mono.justOrEmpty(event.getMessage())
					.subscribe(message -> addBirdUpReaction(message));
			}
			else {
				//check if message contains birdup
				Mono.justOrEmpty(event.getMessage())
					.filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
					.filter(message -> message.getContent().toLowerCase().contains("birdup"))
					.subscribe(message -> addBirdUpReaction(message));
			}
		});	
	}

	private void processReaction(ReactionAddEvent reactionEvent) {
		Mono.just(reactionEvent)
			.filter(event -> !event.getUserId().asString().equals(client.getSelfId().asString()))
			.filter(event -> event.getMessage().block().getAuthor().map(user -> user.getId().asString().equals(client.getSelfId().asString())).orElse(false))
			.flatMap(event -> Destiny.raidFSM(event))
			.subscribe();
	}

	//make client from token
	private BirdUpBot() {
		//initialize variables
		guildStatus = new HashMap<Snowflake, Boolean>();
		emojiMap = new HashMap<String, Pair<Snowflake,Snowflake>>();

		//read files
		readGuildStatus();
		readConstantGuildsEmoji();

		//populate command list
		this.populateCommandMap();
	}

	private void populateCommandMap() {
		//!bird[key] -> command to run
		commandMap.put("raid", event -> Destiny.executeRaid(event).then());
		commandMap.put("off", event -> Control.executeOff(event).then());
		commandMap.put("all", event -> Control.executeOn(event).then());
		commandMap.put("up", event -> Control.executeOn(event).then());
		commandMap.put("shutdown", event -> Control.executeShutDown(event).then());
		commandMap.put("status", event -> Control.executeStatus(event).then());
		commandMap.put("ip", event -> Control.executeIp(event).then());
		commandMap.put("help", event -> Control.executeHelp(event).then());
		commandMap.put("roles", event -> Guild.executeRolls(event).then());
		commandMap.put("time", event -> Control.executeTime(event).then());
	}

	//This needs to work
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

	//This can be generated on a fresh run
	private void readGuildStatus() {
		//check if file exists
		File guildFile = new File(GUILD_TOGGLES_CSV);
		if (guildFile.exists()) {
			//load config csv
			try {
				Scanner guilds = new Scanner(new FileReader(GUILD_TOGGLES_CSV));
				while (guilds.hasNext()) {
					String[] line = guilds.nextLine().split(",");
					guildStatus.put(Snowflake.of(line[0]), line[1].equalsIgnoreCase("true"));
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
		else {
			try {
				guildFile.createNewFile();
			} catch (IOException e) {
				System.err.println("Failed to create guild toggles: " + e.getMessage());
				System.exit(-1);
			}
		}
	}

	//At startup, set presence
	private Mono<Void> setup(ReadyEvent readyEvent) {
		//set presence
		StatusUpdate presence = Presence.online(Activity.playing("BirdUp!"));
		return Mono.justOrEmpty(readyEvent).flatMap(event -> event.getClient().updatePresence(presence));
	}

	public static void exit() {
		//write server states to file
		try {
			BufferedWriter guildStatusOut = new BufferedWriter(new FileWriter(GUILD_TOGGLES_CSV));
			guildStatus.forEach((U, V) -> {
				try {
					guildStatusOut.write(U.asLong() + "," + V + System.lineSeparator());
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

	public static GatewayDiscordClient getClient() {
		return client;
	}

	public static Map<String, Pair<Snowflake, Snowflake>> getEmojiMap() {
		return emojiMap;
	}
}
