package guru.bonacci.kafka.connect.tile38;

import static guru.bonacci.kafka.connect.tile38.Constants.COMMAND_PREFIX;
import static java.util.stream.Collectors.toMap;
import static lombok.AccessLevel.PRIVATE;

import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Topics configured to have a Redis command.
 */
@Getter
@ToString
@RequiredArgsConstructor(access = PRIVATE)
class Topics {

	private final Map<String, String> cmdsByTopic;

	
	Set<String> configuredTopics() {
		return cmdsByTopic.keySet();
	}

	static Topics from(Map<String, String> config) {
		// Filter and remove the prefix from the topic config keys
		return new Topics(config.entrySet().stream()
				.filter(prop -> prop.getKey().startsWith(COMMAND_PREFIX))
				.collect(toMap(k -> k.getKey().replace(COMMAND_PREFIX, ""), 
			                   v -> v.getValue())));
	}
}
