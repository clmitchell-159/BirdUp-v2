package birdUp;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

// command interface
public interface Command {
	Mono<Void> execute(MessageCreateEvent event);
}
