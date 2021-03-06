package birdUp;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.common.util.Snowflake;
import reactor.core.publisher.Mono;

// deals with controling the bot - on, off, help, etc
public class Control {

	static public Mono<Void> executeOff(MessageCreateEvent event) {
		//set all flag in map to false
		return Mono.justOrEmpty(event.getGuildId()).flatMap(guildId -> Mono.just(BirdUpBot.getGuildStatus().computeIfPresent(guildId, (S, B) -> B = false))).then();
	}

	static public Mono<Void> executeOn(MessageCreateEvent event) {
		//set all flag in map to true
		return Mono.justOrEmpty(event.getGuildId()).flatMap(guildId -> Mono.just(BirdUpBot.getGuildStatus().computeIfPresent(guildId, (S, B) -> B = true))).then();

	}

	static public Mono<Void> executeShutDown(MessageCreateEvent event) {
		//only I can shutdown
		User author = Mono.justOrEmpty(event.getMessage().getAuthor()).block();
		if (!author.getId().equals(Snowflake.of("157708010095902720"))) {
			return event.getMessage().getChannel().flatMap(channel -> channel.createMessage("You cannot ask for this; you can turn off all mode with !birdoff")).then();
		}

		ReactionEmoji checkmarkEmoji = ReactionEmoji.unicode("🏁");
		event.getMessage().addReaction(checkmarkEmoji).block();
		BirdUpBot.exit();
		return null;
	}

	static public Mono<Void> executeStatus(MessageCreateEvent event) {
		//get status
		Boolean isEnabled = BirdUpBot.getGuildStatus().get(Mono.justOrEmpty(event.getGuildId()).block());

		//compose message
		String response = new String("BirdUp (v2) is currently online." + System.lineSeparator() + "*Note: Not all features are working.*" + System.lineSeparator() + "All mode enabled: " + isEnabled.toString() + System.lineSeparator());

		//send message
		return event.getMessage().getChannel().flatMap(channel -> channel.createMessage(response)).then();
	}

	static public Mono<Void> executeIp(MessageCreateEvent event) {
		//only I can get IP address
		User author = Mono.justOrEmpty(event.getMessage().getAuthor()).block();
		if (!author.getId().equals(Snowflake.of("157708010095902720"))) {
			return event.getMessage().getChannel().flatMap(channel -> channel.createMessage("You cannot ask for this")).then();
		}

		//get ip addresses
		String response = "Hello master, my ip addresses are:" + System.lineSeparator();
		try {
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			for (;networkInterfaces.hasMoreElements();) {
				NetworkInterface currentInterface = networkInterfaces.nextElement();
				response = response.concat(currentInterface.getDisplayName() + ": ");
				for (Enumeration<InetAddress> address = currentInterface.getInetAddresses(); address.hasMoreElements(); ) {
					response = response.concat(address.nextElement().getHostAddress() + " | ");
				}
				response = response.concat(System.lineSeparator());
			}
		} catch (SocketException e) {
			response = response.concat("Failed to get network interfaces" + System.lineSeparator());
		}

		//convert string to final
		String finalResponse = response;

		//send message
		return event.getMessage().getChannel().flatMap(channel -> channel.createMessage(finalResponse)).then();
	}

	static public Mono<Void> executeHelp(MessageCreateEvent event) {
		return event.getMessage().getChannel().flatMap(channel -> channel.createMessage(
			"Welcome to Bird Help!\n" +
			"\n" +
			"Commands:\n" +
			"!birdraid for raid setup\n" +
			"!birdhelp for this menu\n"
			)).then();
	}
	
	static public Mono<Void> executeTime(MessageCreateEvent event) {
		LocalDateTime utc = LocalDateTime.now(ZoneId.of("Z"));
		LocalDateTime est = utc.minusHours(5);
		LocalDateTime cst = utc.minusHours(6);
		LocalDateTime mst = utc.minusHours(7);
		LocalDateTime pst = utc.minusHours(8);
		DateTimeFormatter format = DateTimeFormatter.ofPattern("LLL dd, hh:mm a");
		
		return event.getMessage().getChannel().flatMap(channel -> channel.createMessage(
			"Current Time:\n```" +
			utc.format(format) + " UTC\n" +
			est.format(format) + " EST\n" +
			cst.format(format) + " CST\n" +
			mst.format(format) + " MST\n" +
			pst.format(format) + " PST\n```"
		)).then();
	}
}

