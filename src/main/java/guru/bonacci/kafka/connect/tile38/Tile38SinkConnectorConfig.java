package guru.bonacci.kafka.connect.tile38;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.apache.kafka.connect.sink.SinkTask.TOPICS_CONFIG;

import java.util.Map;
import java.util.Set;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigException;

import com.google.common.collect.Sets;

public class Tile38SinkConnectorConfig extends AbstractConfig {

	//TODO connect to multiple hosts?
	public static final String TILE38_URL = "tile38.url";
	private static final String TILE38_URL_DOC = "Tile38 URL to connect.";
	public static final String TILE38_PORT = "tile38.port";
	private static final String TILE38_PORT_DOC = "Tile38 port to connect.";

	Topics topics;
	CommandTemplates cmdTemplates;

	
	public Tile38SinkConnectorConfig(Map<String, String> props) {
		this(conf(), props);
	}

	public Tile38SinkConnectorConfig(ConfigDef config, Map<String, String> props) {
		super(config, props);
		
		topics = Topics.from(props); 
		cmdTemplates = CommandTemplates.from(topics);

		validateConfiguredTopics(props);
	}

	private void validateConfiguredTopics(Map<String, String> props) {
		 Set<String> topics = props.containsKey(TOPICS_CONFIG)
				? stream((props.get(TOPICS_CONFIG).trim()).split(",")).map(String::trim).collect(toSet()) 
				: emptySet();
				 
        Set<String> configuredTopics = this.topics.configuredTopics();

        if (!Sets.symmetricDifference(topics, configuredTopics).isEmpty()) {
            throw new ConfigException(format("There is a mismatch between topics defined into the property 'topics' %s and configured topics %s", 
            		topics, configuredTopics));
        }
    }
	
	public static ConfigDef conf() {
		return new ConfigDef()
				.define(TILE38_URL, Type.STRING, Importance.HIGH, TILE38_URL_DOC)
				.define(TILE38_PORT, Type.INT, Importance.HIGH, TILE38_PORT_DOC);
	}

	public String getTile38Url() {
		return this.getString(TILE38_URL);
	}

	public Integer getTile38Port() {
		return this.getInt(TILE38_PORT);
	}
}