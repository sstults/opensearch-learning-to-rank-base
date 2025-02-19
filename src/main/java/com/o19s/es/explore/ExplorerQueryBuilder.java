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
 *
 */

package com.o19s.es.explore;

import org.opensearch.ltr.stats.LTRStats;
import org.opensearch.ltr.stats.StatName;
import org.apache.lucene.search.Query;
import org.opensearch.core.ParseField;
import org.opensearch.core.common.ParsingException;
import org.opensearch.core.common.io.stream.NamedWriteable;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.ObjectParser;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.index.query.AbstractQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryRewriteContext;
import org.opensearch.index.query.QueryShardContext;
import org.opensearch.index.query.Rewriteable;

import java.io.IOException;
import java.util.Objects;

public class ExplorerQueryBuilder extends AbstractQueryBuilder<ExplorerQueryBuilder> implements NamedWriteable {
    public static final String NAME = "match_explorer";

    private static final ParseField QUERY_NAME = new ParseField("query");
    private static final ParseField TYPE_NAME = new ParseField("type");
    private static final ObjectParser<ExplorerQueryBuilder, Void> PARSER;

    static {
        PARSER = new ObjectParser<>(NAME, ExplorerQueryBuilder::new);
        PARSER.declareObject(
                ExplorerQueryBuilder::query,
                (parser, context) -> parseInnerQueryBuilder(parser),
                QUERY_NAME
        );
        PARSER.declareString(ExplorerQueryBuilder::statsType, TYPE_NAME);
        declareStandardFields(PARSER);
    }

    private QueryBuilder query;
    private String type;
    private LTRStats ltrStats;

    public ExplorerQueryBuilder() {
    }


    public ExplorerQueryBuilder(StreamInput in, LTRStats ltrStats) throws IOException {
        super(in);
        query = in.readNamedWriteable(QueryBuilder.class);
        type = in.readString();
        this.ltrStats = ltrStats;
    }

    public static ExplorerQueryBuilder fromXContent(XContentParser parser, LTRStats ltrStats) throws IOException {
        final ExplorerQueryBuilder builder;

        try {
            builder = PARSER.parse(parser, null);
        } catch (IllegalArgumentException iae) {
            throw new ParsingException(parser.getTokenLocation(), iae.getMessage(), iae);
        }

        if (builder.query == null) {
            throw new ParsingException(parser.getTokenLocation(), "Field [" + QUERY_NAME + "] is mandatory.");
        }
        if (builder.statsType() == null) {
            throw new ParsingException(parser.getTokenLocation(), "Field [" + TYPE_NAME + "] is mandatory.");
        }
        builder.ltrStats(ltrStats);
        return builder;
    }

    @Override
    protected void doWriteTo(StreamOutput out) throws IOException {
        out.writeNamedWriteable(query);
        out.writeString(type);
    }

    @Override
    protected void doXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(NAME);
        printBoostAndQueryName(builder);
        builder.field(QUERY_NAME.getPreferredName(), query);
        builder.field(TYPE_NAME.getPreferredName(), type);
        builder.endObject();
    }

    @Override
    protected Query doToQuery(QueryShardContext context) throws IOException {
        ltrStats.getStat(StatName.LTR_REQUEST_TOTAL_COUNT.getName()).increment();
        return new ExplorerQuery(query.toQuery(context), type, ltrStats);
    }

    @Override
    protected QueryBuilder doRewrite(QueryRewriteContext queryRewriteContext) throws IOException {
        if (queryRewriteContext != null) {

            ExplorerQueryBuilder rewritten = new ExplorerQueryBuilder();
            rewritten.type = this.type;
            rewritten.query = Rewriteable.rewrite(query, queryRewriteContext);
            rewritten.ltrStats = this.ltrStats;
            rewritten.boost(boost());
            rewritten.queryName(queryName());

            if (!rewritten.equals(this)) {
                return rewritten;
            }
        }
        return super.doRewrite(queryRewriteContext);
    }

    @Override
    protected int doHashCode() {
        return Objects.hash(query, type);
    }

    @Override
    protected boolean doEquals(ExplorerQueryBuilder other) {
        return Objects.equals(query, other.query)
                && Objects.equals(type, other.type);
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    public QueryBuilder query() {
        return query;
    }

    public ExplorerQueryBuilder query(QueryBuilder query) {
        this.query = query;
        return this;
    }

    public ExplorerQueryBuilder ltrStats(LTRStats ltrStats) {
        this.ltrStats = ltrStats;
        return this;
    }

    public String statsType() {
        return type;
    }

    public ExplorerQueryBuilder statsType(String type) {
        this.type = type;
        return this;
    }
}
