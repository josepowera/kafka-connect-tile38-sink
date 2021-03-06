/**
 * 	Copyright 2020 Jeffrey van Helden (aabcehmu@mailfence.com)
 *	
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	    http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */
package guru.bonacci.kafka.connect.tile38.writer;

import static guru.bonacci.kafka.connect.tile38.commands.CommandGenerator.from;
import static io.lettuce.core.codec.StringCodec.UTF8;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import org.apache.kafka.connect.errors.ConnectException;

import guru.bonacci.kafka.connect.tile38.commands.CommandGenerators;
import guru.bonacci.kafka.connect.tile38.commands.CommandResult;
import guru.bonacci.kafka.connect.tile38.commands.CommandTemplates;
import guru.bonacci.kafka.connect.tile38.config.Tile38SinkConnectorConfig;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Tile38Writer {

	@Getter private final RedisClient client; // getter for testing
	private final RedisAsyncCommands<String, String> async;

	private final CommandGenerators cmds;

	public Tile38Writer(Tile38SinkConnectorConfig config) {
		final SocketOptions socketOptions = SocketOptions.builder()
		          .tcpNoDelay(config.getTcpNoDelay())
		          .connectTimeout(Duration.ofMillis(config.getConnectTimeout()))
		          .keepAlive(config.getKeepAliveEnabled())
		          .build();

		final ClientOptions.Builder clientOptions = ClientOptions.builder()
				.socketOptions(socketOptions)
				.requestQueueSize(config.getRequestQueueSize())
				.autoReconnect(config.getAutoReconnectEnabled());
		
		this.client = RedisClient.create(
    			String.format("redis://%s:%d", config.getHost(), config.getPort()));
	    this.client.setOptions(clientOptions.build());

		this.async = client.connect().async();
		
		// authenticate
		if (isNotBlank(config.getTile38Password())) {
			try {
				String authenticated = async.dispatch(
					CommandType.AUTH,
				    new StatusOutput<>(UTF8), 
				    new CommandArgs<>(UTF8).add(config.getTile38Password()))
				.get();
				log.info("Authentication: {}", authenticated);
			} catch (InterruptedException | ExecutionException e) {
				new ConnectException("Failed to establish a connection to Tile38", e);
			}
		}	

	    // disable auto-flushing to allow for batch inserts
		this.async.setAutoFlushCommands(false);
		
		final CommandTemplates cmdTemplates = config.getCmdTemplates();
		// a command generator for each configured topic
		this.cmds = CommandGenerators.from(cmdTemplates.configuredTopics()
				.collect(toMap(identity(), topic -> from(cmdTemplates.templateForTopic(topic)))));
    }


	public RedisFuture<?>[] write(Stream<Tile38Record> records) {
		final RedisFuture<?>[] futures = records
				.map(event -> cmds.generatorForTopic(event.getTopic()).compile(event)) // create command(s)
				.flatMap(CommandResult::asStream)
				.filter(Objects::nonNull) // non-expire commands are null
				.map(cmd -> async.dispatch(cmd.getLeft(), cmd.getMiddle(), cmd.getRight())) // execute command
				.toArray(RedisFuture[]::new); // collect futures

		// async batch insert
		async.flushCommands();
		return futures;
    }

    public void close() {
		client.shutdown();
	}
}
