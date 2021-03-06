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
package guru.bonacci.kafka.connect.tile38;

import static com.github.jcustenborder.kafka.connect.utils.SinkRecordHelper.write;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import guru.bonacci.kafka.connect.tile38.writer.Tile38Record;


public class RecordBuilderTests {

	@Test
	void buildNothing() {
		Stream<Tile38Record> result = 
				new RecordBuilder().build();
		
		assertThat(result.findAny().isPresent(), is(false));
	}

	@Test
	void buildWithoutTopic() {
		Stream<Tile38Record> result = 
				new RecordBuilder()
						.withSinkRecords(recordsProvider())
						.build();
		
		assertThat(result.findAny().isPresent(), is(false));
	}

	@Test
	void build() {
		Stream<Tile38Record> result = 
				new RecordBuilder()
						.withTopics(ImmutableSet.of("t1","t2"))
						.withSinkRecords(recordsProvider())
						.build();

		Map<String, List<Tile38Record>> byTopic = result
        		.collect(Collectors.groupingBy(Tile38Record::getTopic));

		assertThat(byTopic.get("t1"), hasSize(2));
		assertThat(byTopic.get("t2"), hasSize(1));
	}

	private List<SinkRecord> recordsProvider() {
		// two records on t1
		Schema schema11 = SchemaBuilder.struct().field("id", Schema.STRING_SCHEMA)
				.field("foo", Schema.STRING_SCHEMA).field("bar", Schema.STRING_SCHEMA).build();
		Struct value11 = new Struct(schema11).put("id", "some id").put("foo", "some foo").put("bar", "some bar");
		SinkRecord rec11 = write("t1", Schema.STRING_SCHEMA, "id", schema11, value11);

		Schema schema12 = SchemaBuilder.struct().field("id", Schema.STRING_SCHEMA)
				.field("foo", Schema.STRING_SCHEMA).field("bar", Schema.STRING_SCHEMA).build();
		Struct value12 = new Struct(schema12).put("id", "another id").put("foo", "another foo").put("bar", "another bar");
		SinkRecord rec12 = write("t1", Schema.STRING_SCHEMA, "id", schema12, value12);

		// one record on t2
		Schema schema21 = SchemaBuilder.struct().field("id", Schema.STRING_SCHEMA)
				.field("no foo", Schema.STRING_SCHEMA).build();
		Struct value21 = new Struct(schema21).put("id", "some id").put("no foo", "nop");
		SinkRecord rec21 = write("t2", Schema.STRING_SCHEMA, "id", schema21, value21);

		// one record on non-configured t3
		Schema schema31 = SchemaBuilder.struct().field("id", Schema.STRING_SCHEMA).build();
		Struct value31 = new Struct(schema31).put("id", "unused id");
		SinkRecord rec31 = write("t3", Schema.STRING_SCHEMA, "id", schema31, value31);

		return ImmutableList.of(rec11, rec21, rec12, rec31);
	}
}
