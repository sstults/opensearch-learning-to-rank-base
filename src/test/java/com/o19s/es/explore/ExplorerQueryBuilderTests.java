/*
 *
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
 *
 */
package com.o19s.es.explore;

import org.opensearch.ltr.stats.LTRStat;
import org.opensearch.ltr.stats.LTRStats;
import org.opensearch.ltr.stats.StatName;
import org.opensearch.ltr.stats.suppliers.CounterSupplier;
import com.o19s.es.ltr.LtrQueryParserPlugin;
import org.apache.lucene.search.Query;
import org.opensearch.core.common.ParsingException;
import org.opensearch.index.query.AbstractQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryShardContext;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.plugins.Plugin;
import org.opensearch.test.AbstractQueryTestCase;
import org.opensearch.test.TestGeoShapeFieldMapperPlugin;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.instanceOf;

public class ExplorerQueryBuilderTests extends AbstractQueryTestCase<ExplorerQueryBuilder> {
    // TODO: Remove the TestGeoShapeFieldMapperPlugin once upstream has completed the migration.
    protected Collection<Class<? extends Plugin>> getPlugins() {
        return asList(LtrQueryParserPlugin.class, TestGeoShapeFieldMapperPlugin.class);
    }
    private LTRStats ltrStats = new LTRStats(unmodifiableMap(new HashMap<String, LTRStat<?>>() {{
        put(StatName.LTR_REQUEST_TOTAL_COUNT.getName(),
                new LTRStat<>(false, new CounterSupplier()));
        put(StatName.LTR_REQUEST_ERROR_COUNT.getName(),
                new LTRStat<>(false, new CounterSupplier()));
    }}));
    @Override
    protected ExplorerQueryBuilder doCreateTestQueryBuilder() {
        ExplorerQueryBuilder builder = new ExplorerQueryBuilder();
        builder.query(new TermQueryBuilder("foo", "bar"));
        builder.statsType("sum_raw_ttf");
        builder.ltrStats(ltrStats);
        return builder;
    }

    public void testParse() throws Exception {
        String query = " {" +
                        "  \"match_explorer\": {" +
                        "    \"query\": {" +
                        "      \"match\": {" +
                        "        \"title\": \"test\"" +
                        "      }" +
                        "    }," +
                        "   \"type\": \"stddev_raw_tf\"" +
                        "  }" +
                        "}";

        ExplorerQueryBuilder builder = (ExplorerQueryBuilder)parseQuery(query);

        assertNotNull(builder.query());
        assertEquals(builder.statsType(), "stddev_raw_tf");
    }

    @Override
    public void testMustRewrite() throws IOException {
        QueryShardContext context = createShardContext();
        context.setAllowUnmappedFields(true);
        ExplorerQueryBuilder queryBuilder = createTestQueryBuilder();
        queryBuilder.boost(AbstractQueryBuilder.DEFAULT_BOOST);
        QueryBuilder rewritten = queryBuilder.rewrite(context);

        // though the query may be rewritten, we assert that we
        // always rewrite to an ExplorerQueryBuilder (same goes for ExplorerQuery...)
        assertThat(rewritten, instanceOf(ExplorerQueryBuilder.class));
        Query q = rewritten.toQuery(context);
        assertThat(q, instanceOf(ExplorerQuery.class));
    }

    public void testMissingQuery() throws Exception {
        String query =  " {" +
                        "  \"match_explorer\": {" +
                        "   \"type\": \"stddev_raw_tf\"" +
                        "  }" +
                        "}";

        expectThrows(ParsingException.class, () -> parseQuery(query));
    }

    public void testMissingType() throws Exception {
        String query =  " {" +
                        "  \"match_explorer\": {" +
                        "    \"query\": {" +
                        "      \"match\": {" +
                        "        \"title\": \"test\"" +
                        "      }" +
                        "    }" +
                        "  }" +
                        "}";

        expectThrows(ParsingException.class, () -> parseQuery(query));
    }

    @Override
    protected void doAssertLuceneQuery(ExplorerQueryBuilder queryBuilder, Query query, QueryShardContext context) throws IOException {
        assertThat(query, instanceOf(ExplorerQuery.class));
    }
}
