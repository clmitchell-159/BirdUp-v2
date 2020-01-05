package birdUp;

import java.util.Map;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

//deals with Destiny related content
public class Destiny {

	static public Mono<Void> executeRaid(MessageCreateEvent event) {
		//get required objects
		DiscordClient client = BirdUpBot.getClient();
		Map<String, Pair<Snowflake, Snowflake>> emojiMap = BirdUpBot.getEmojiMap();

		//set up message
		String raidTop = " " + System.lineSeparator() + "╔══════════════╗" + System.lineSeparator() + "║                 RAID                 ║" + System.lineSeparator() + "╚══════════════╝" + System.lineSeparator() + "SotP:     :confetti_ball:" + System.lineSeparator() + "LW:      :tropical_fish: " + System.lineSeparator() + "EoW:   :earth_americas: " + System.lineSeparator() + "Levi:     :whale2:" + System.lineSeparator() + "SoS:     :poop:" + System.lineSeparator() + "CoS:	:crown:" + System.lineSeparator() + "GoS:	:wilted_rose:";
		String raidMid = " " + System.lineSeparator() + "╔══════════════╗" + System.lineSeparator() + "║                 TIME                 ║" + System.lineSeparator() + "╚══════════════╝" + System.lineSeparator() + "Now: <:birdup:562306087823474689>" + System.lineSeparator() + "1 hr: :one:" + System.lineSeparator() + "2 hr: :two:" + System.lineSeparator() + "4 hr: :four:" + System.lineSeparator() + "Later Today: :alarm_clock:" + System.lineSeparator() + "Anytime Today: :call_me:";
		String raidBot = " " + System.lineSeparator() + "╔══════════════╗" + System.lineSeparator() + "║                   <:birdup:562306087823474689>                  ║" + System.lineSeparator() + "╚══════════════╝";

		//set up reactions
		//Raid Icons
		ReactionEmoji SotP = ReactionEmoji.unicode("🎊");
		ReactionEmoji LW = ReactionEmoji.unicode("🐠");
		ReactionEmoji EoW = ReactionEmoji.unicode("🌎");
		ReactionEmoji Levi = ReactionEmoji.unicode("🐋");
		ReactionEmoji SoS = ReactionEmoji.unicode("💩");
		ReactionEmoji CoS = ReactionEmoji.unicode("👑");
		ReactionEmoji GoS = ReactionEmoji.unicode("🥀");

		//Times
		ReactionEmoji birdUpEmoji = ReactionEmoji.custom(client.getGuildEmojiById(emojiMap.get("birdup").first, emojiMap.get("birdup").second).block());
		ReactionEmoji one = ReactionEmoji.custom(client.getGuildEmojiById(emojiMap.get("one_one").first, emojiMap.get("one_one").second).block());
		ReactionEmoji two = ReactionEmoji.custom(client.getGuildEmojiById(emojiMap.get("two_two").first, emojiMap.get("two_two").second).block());
		ReactionEmoji four = ReactionEmoji.custom(client.getGuildEmojiById(emojiMap.get("four_four").first, emojiMap.get("four_four").second).block());
		ReactionEmoji clock = ReactionEmoji.unicode("⏰");
		ReactionEmoji hand = ReactionEmoji.unicode("🤙");

		//create top message
		event.getMessage().getChannel().flatMap(channel -> channel.createMessage(raidTop))
		//add emoji
		.flatMap(msg -> msg.addReaction(SotP)
				.then(msg.addReaction(LW))
				.then(msg.addReaction(EoW))
				.then(msg.addReaction(Levi))
				.then(msg.addReaction(SoS))
				.then(msg.addReaction(CoS))
				.then(msg.addReaction(GoS))
				)
		//send
		.subscribe();

		//create mid
		event.getMessage().getChannel().flatMap(channel -> channel.createMessage(raidMid))
		//add emoji
		.flatMap(msg -> msg.addReaction(birdUpEmoji)
				.then(msg.addReaction(one))
				.then(msg.addReaction(two))
				.then(msg.addReaction(four))
				.then(msg.addReaction(clock))
				.then(msg.addReaction(hand))
				)
		//send
		.subscribe();

		//create & send bot
		return event.getMessage().getChannel().flatMap(channel -> channel.createMessage(raidBot)).then();
	}

}
