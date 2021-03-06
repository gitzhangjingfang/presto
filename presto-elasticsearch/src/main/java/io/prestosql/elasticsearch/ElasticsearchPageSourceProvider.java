/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.prestosql.elasticsearch;

import io.prestosql.elasticsearch.client.ElasticsearchClient;
import io.prestosql.spi.connector.ColumnHandle;
import io.prestosql.spi.connector.ConnectorPageSource;
import io.prestosql.spi.connector.ConnectorPageSourceProvider;
import io.prestosql.spi.connector.ConnectorSession;
import io.prestosql.spi.connector.ConnectorSplit;
import io.prestosql.spi.connector.ConnectorTableHandle;
import io.prestosql.spi.connector.ConnectorTransactionHandle;
import io.prestosql.spi.predicate.TupleDomain;

import javax.inject.Inject;

import java.util.List;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.prestosql.elasticsearch.ElasticsearchTableHandle.Type.QUERY;
import static java.util.Objects.requireNonNull;

public class ElasticsearchPageSourceProvider
        implements ConnectorPageSourceProvider
{
    private final ElasticsearchClient client;

    @Inject
    public ElasticsearchPageSourceProvider(ElasticsearchClient client)
    {
        this.client = requireNonNull(client, "client is null");
    }

    @Override
    public ConnectorPageSource createPageSource(
            ConnectorTransactionHandle transaction,
            ConnectorSession session,
            ConnectorSplit split,
            ConnectorTableHandle table,
            List<ColumnHandle> columns,
            TupleDomain<ColumnHandle> dynamicFilter)
    {
        requireNonNull(split, "split is null");
        requireNonNull(table, "table is null");

        ElasticsearchTableHandle elasticsearchTable = (ElasticsearchTableHandle) table;
        ElasticsearchSplit elasticsearchSplit = (ElasticsearchSplit) split;

        if (elasticsearchTable.getType().equals(QUERY)) {
            return new PassthroughQueryPageSource(client, elasticsearchTable);
        }

        if (columns.isEmpty()) {
            return new CountQueryPageSource(client, session, elasticsearchTable, elasticsearchSplit);
        }

        return new ScanQueryPageSource(
                client,
                session,
                elasticsearchTable,
                elasticsearchSplit,
                columns.stream()
                        .map(ElasticsearchColumnHandle.class::cast)
                        .collect(toImmutableList()));
    }
}
