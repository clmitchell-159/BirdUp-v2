package birdUp;

import java.time.Duration;
import java.time.Instant;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

// deals with kitty related event
public class Kitty {

	private static int count;
	private static Instant loadTime; 
	
	static public Mono<Void> execute(MessageCreateEvent event) {
		Duration difference = Duration.between(Instant.now(), loadTime).abs();
		long days = difference.toDays();
		long hours = difference.minusDays(days).toHours();
		long minutes = difference.minusDays(days).minusHours(hours).toMinutes();
		long seconds = difference.minusDays(days).minusHours(hours).minusMinutes(minutes).getSeconds();
		String message = "There has been " + count + " kitty mentions since I have reset - " + days + " Days, " + hours + ":" + minutes + ":" + seconds;
		return event.getMessage().getChannel().flatMap(channel -> channel.createMessage(message)).then();
	}
	
	static void initialize() {
		count = 0;
		loadTime = Instant.now();
	}
}
