package birdUp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.Embed;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import discord4j.common.util.Snowflake;
import reactor.core.publisher.Mono;

//deals with Destiny related content
public class Destiny {

	private static final Map<String, String> emojiToRaid = createRaidMap();
	private static final Map<String, String> emojiToTime = createTimeMap();
	private static final long DELAY_ALLOWED = 2 * 60;
	private static final long FOUR_HOURS = 60 * 60 * 4;
	
	private static String raidStr = "Default";
	private static String emojiStr = "Default";
	
	static public Mono<Void> executeRaid(MessageCreateEvent event) {
		//set up message
		EmbedCreateSpec test = new EmbedCreateSpec();
		test.setDescription("This is a test");

		//create & send bot
		//return event.getMessage().getChannel().flatMap(channel -> channel.createMessage(raidBot)).then();
		return event.getMessage().getChannel().flatMap(channel -> channel.createEmbed(spec -> 
				spec.setColor(Color.RED)
					.setAuthor("BirdUp", null, "https://cdn.discordapp.com/avatars/562423114491756545/a14b34bf4f679ecd9c08dd70a3446079.png")
					.setTitle("Select The Raid")
					.addField("Last Wish", "<:lw:795143989694038066>", false)
					.addField("Garden of Salvation", "<:gos:795145162669424680>", false)
					.addField("Deep Stone Crypt", "<:dsc:795142534292307969>", false)
					.setFooter("0", null)
					.setTimestamp(Instant.now())
				)).then();
	}
	
	static public Mono<Void> addRaidReactions(Message message) {
		//required objects
		GatewayDiscordClient client = BirdUpBot.getClient();
		Map<String, Pair<Snowflake, Snowflake>> emojiMap = BirdUpBot.getEmojiMap();
		
		//required reactions
		ReactionEmoji dsc = ReactionEmoji.custom(client.getGuildEmojiById(emojiMap.get("dsc").first, emojiMap.get("dsc").second).block());
		ReactionEmoji gos = ReactionEmoji.custom(client.getGuildEmojiById(emojiMap.get("gos").first, emojiMap.get("gos").second).block());
		ReactionEmoji lw = ReactionEmoji.custom(client.getGuildEmojiById(emojiMap.get("lw").first, emojiMap.get("lw").second).block());
		
		return Mono.just(message)
			.flatMap((unused) -> message.addReaction(lw))
			.then(Mono.just(0))
			.flatMap((unused) -> message.addReaction(gos))
			.then(Mono.just(0))
			.flatMap((unused) -> message.addReaction(dsc))
			.then();
	}

	static public Mono<Void> raidFSM(ReactionAddEvent reactionEvent) {
		GatewayDiscordClient client = BirdUpBot.getClient();
		Map<String, Pair<Snowflake, Snowflake>> emojiMap = BirdUpBot.getEmojiMap();
		
		Message message = reactionEvent.getMessage().block();
		Embed emb = message.getEmbeds().get(0);
		String footerText = emb.getFooter().get().getText();
		Instant time = emb.getTimestamp().get();
		long timeDiff = ChronoUnit.SECONDS.between(time, Instant.now());
		
		if (footerText.equals("0") && timeDiff <= DELAY_ALLOWED) {
			String raidName;
			Mono.just(reactionEvent)
				.map(event -> event.getEmoji().asCustomEmoji())
				.filter(custom -> custom.isPresent())
				.map(custom -> custom.get().getName())
				.map(name -> {
					setRaid(name);
					return Mono.just(name);
					})
				.block();
			raidName = emojiToRaid.get(raidStr);
			
			reactionEvent.getMessage().block().removeAllReactions().block();
			 
			reactionEvent.getMessage().block().edit(msgSpec ->
				msgSpec.setEmbed(spec ->
					spec.setColor(Color.BLUE)
						.setAuthor("BirdUp", null, "https://cdn.discordapp.com/avatars/562423114491756545/a14b34bf4f679ecd9c08dd70a3446079.png")
						.setTitle("Select Time")
						.setDescription(raidName)
						.addField("Now", "<:bell_bell:795212025943949383>", false)
						.addField("1 Hour", "<:one_one:635363187688210442>", false)
						.addField("2 Hours", "<:two_two:635363187553992704>", false)
						.addField("3 Hours", "<:three_three:795208091004174337>", false)
						.addField("4 Hours", "<:four_four:635363187449004043>", false)
						.setFooter("1", null)
						.setTimestamp(Instant.now())
			)).block();
			
			Mono.just(message)
				.flatMap((unused) -> message.addReaction(ReactionEmoji.custom(client.getGuildEmojiById(emojiMap.get("bell_bell").first, emojiMap.get("bell_bell").second).block())))
				.then(Mono.just(0))
				.flatMap((unused) -> message.addReaction(ReactionEmoji.custom(client.getGuildEmojiById(emojiMap.get("one_one").first, emojiMap.get("one_one").second).block())))
				.then(Mono.just(0))
				.flatMap((unused) -> message.addReaction(ReactionEmoji.custom(client.getGuildEmojiById(emojiMap.get("two_two").first, emojiMap.get("two_two").second).block())))
				.then(Mono.just(0))
				.flatMap((unused) -> message.addReaction(ReactionEmoji.custom(client.getGuildEmojiById(emojiMap.get("three_three").first, emojiMap.get("three_three").second).block())))
				.then(Mono.just(0))
				.flatMap((unused) -> message.addReaction(ReactionEmoji.custom(client.getGuildEmojiById(emojiMap.get("four_four").first, emojiMap.get("four_four").second).block())))
				.block();
		}
		
		if (footerText.equals("1") && timeDiff <= DELAY_ALLOWED) {
			String when;
			Mono.just(reactionEvent)
				.map(event -> event.getEmoji().asCustomEmoji())
				.filter(custom -> custom.isPresent())
				.map(custom -> custom.get().getName())
				.map(name -> {
					setEmoji(name);;
					return Mono.just(name);
					})
				.block();
			when = emojiToTime.get(emojiStr);
			String raidFull = emb.getDescription().get();
			String raidEmojiName = recoverRaid(raidFull);
			String userName = "Error Fetching Name";
			try {
				userName = reactionEvent.getMember().get().getNickname().get();
			}
			catch (NoSuchElementException e) {
				userName = reactionEvent.getMember().get().getUsername();
			}
			String finalUserName = userName;

			reactionEvent.getMessage().block().removeAllReactions().block();
			Instant currentTime = Instant.now();
			LocalDateTime utc = LocalDateTime.now(ZoneId.of("Z"));
			if (when.equals("in One Hour")) {
				utc = utc.plusHours(1);
			}
			else if (when.equals("in Two Hours")) {
				utc = utc.plusHours(2);
			}
			else if (when.equals("in Three Hours")) {
				utc = utc.plusHours(3);
			}
			else if (when.equals("in Four Hours")) {
				utc = utc.plusHours(4);
			}
			LocalDateTime utc_final = utc;
			LocalDateTime est = utc.minusHours(5);
			LocalDateTime cst = utc.minusHours(6);
			LocalDateTime mst = utc.minusHours(7);
			LocalDateTime pst = utc.minusHours(8);
			DateTimeFormatter format = DateTimeFormatter.ofPattern("LLL dd, hh:mm a");
			
			reactionEvent.getMessage().block().edit(msgSpec ->
			msgSpec.setEmbed(spec ->
				spec.setColor(Color.GREEN)
					.setAuthor("BirdUp", null, "https://cdn.discordapp.com/avatars/562423114491756545/a14b34bf4f679ecd9c08dd70a3446079.png")
					.setTitle(raidFull + " " + when)
					.setDescription("React to Join!\nRaid at:```\n" +
							utc_final.format(format) + " UTC\n" +
							est.format(format) + " EST\n" +
							cst.format(format) + " CST\n" +
							mst.format(format) + " MST\n" +
							pst.format(format) + " PST\n```"
							)
					.addField("Guardians", finalUserName + "\n", false)
					.setFooter("2", null)
					.setTimestamp(currentTime)
			)).block();

			Mono.just(message)
				.flatMap((unused) -> message.addReaction(ReactionEmoji.custom(client.getGuildEmojiById(emojiMap.get(raidEmojiName).first, emojiMap.get(raidEmojiName).second).block())))
				.block();
		}
		
		if (footerText.equals("2") && timeDiff <= FOUR_HOURS) {
			String userName = "Error Fetching Name";
			try {
				userName = reactionEvent.getMember().get().getNickname().get();
			}
			catch (NoSuchElementException e) {
				userName = reactionEvent.getMember().get().getUsername();
			}
			
			String currentGuardians = emb.getFields().get(0).getValue();
			String newGuardiands;
			
			if (!currentGuardians.contains(userName)) {
				newGuardiands = currentGuardians.concat("\n" + userName + "\n");
			}
			else {
				newGuardiands = currentGuardians;
			}

			reactionEvent.getMessage().block().edit(msgSpec ->
			msgSpec.setEmbed(spec ->
				spec.setColor(Color.GREEN)
					.setAuthor("BirdUp", null, "https://cdn.discordapp.com/avatars/562423114491756545/a14b34bf4f679ecd9c08dd70a3446079.png")
					.setTitle(emb.getTitle().get())
					.setDescription(emb.getDescription().get())
					.addField("Guardians", newGuardiands, false)
					.setFooter("2", null)
					.setTimestamp(time)
			)).block();

		}
		
		return Mono.just(0).then();
	}
	
	private static Map<String, String> createRaidMap() {
		Map<String, String> emojiMap = new HashMap<>();
		emojiMap.put("dsc", "Deep Stone Crypt");
		emojiMap.put("lw", "Last Wish");
		emojiMap.put("gos", "Garden of Salvation");
		
		return emojiMap;
	}
	
	private static Map<String, String> createTimeMap() {
		Map<String, String> emojiMap = new HashMap<>();
		emojiMap.put("bell_bell", "Right Now");
		emojiMap.put("one_one", "in One Hour");
		emojiMap.put("two_two", "in Two Hours");
		emojiMap.put("three_three", "in Three Hours");
		emojiMap.put("four_four", "in Four Hours");
		
		return emojiMap;
	}
	
	private static void setRaid(String val) {
		raidStr = val;
	}
	
	private static void setEmoji(String val) {
		emojiStr = val;
	}
	
	private static String recoverRaid(String full) {
		for (Map.Entry<String, String> e : emojiToRaid.entrySet()) {
			if (e.getValue().equals(full)) {
				return e.getKey();
			}
		}
		return null;
	}
	
}
