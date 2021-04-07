package birdUp;

import discord4j.core.event.domain.message.ReactionAddEvent;
import reactor.core.publisher.Mono;

public interface Reaction {
	Mono<Void> execute(ReactionAddEvent event);
}
