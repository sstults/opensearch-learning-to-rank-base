/*
 * Copyright [2017] Wikimedia Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.o19s.es.ltr.query;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.opensearch.core.ParseField;
import org.opensearch.core.common.ParsingException;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.ObjectParser;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.index.query.AbstractQueryBuilder;
import org.opensearch.index.query.QueryShardContext;
import org.opensearch.index.query.QueryShardException;
import org.opensearch.ltr.stats.LTRStats;
import org.opensearch.ltr.stats.StatName;

import com.o19s.es.ltr.LtrQueryContext;
import com.o19s.es.ltr.feature.Feature;
import com.o19s.es.ltr.feature.FeatureSet;
import com.o19s.es.ltr.feature.FeatureValidation;
import com.o19s.es.ltr.feature.store.CompiledLtrModel;
import com.o19s.es.ltr.feature.store.PrecompiledExpressionFeature;
import com.o19s.es.ltr.feature.store.StorableElement;
import com.o19s.es.ltr.feature.store.StoredFeature;
import com.o19s.es.ltr.feature.store.StoredFeatureSet;
import com.o19s.es.ltr.feature.store.StoredLtrModel;
import com.o19s.es.ltr.ranker.linear.LinearRanker;
import com.o19s.es.ltr.ranker.parser.LtrRankerParserFactory;

public class ValidatingLtrQueryBuilder extends AbstractQueryBuilder<ValidatingLtrQueryBuilder> {
    public static final Set<String> SUPPORTED_TYPES = unmodifiableSet(
        new HashSet<>(asList(StoredFeature.TYPE, StoredFeatureSet.TYPE, StoredLtrModel.TYPE))
    );

    public static final String NAME = "validating_ltr_query";
    private static final ParseField VALIDATION = new ParseField("validation");
    private static final ObjectParser<ValidatingLtrQueryBuilder, Void> PARSER = new ObjectParser<>(NAME);

    static {
        BiConsumer<ValidatingLtrQueryBuilder, StorableElement> setElem = (b, v) -> {
            if (b.element != null) {
                throw new IllegalArgumentException(
                    "["
                        + b.element.type()
                        + "] already set, only one element can be set at a time ("
                        + SUPPORTED_TYPES.stream().collect(joining(","))
                        + ")."
                );
            }
            b.element = v;
        };

        PARSER.declareObject(setElem, (parser, ctx) -> StoredFeature.parse(parser), new ParseField(StoredFeature.TYPE));
        PARSER.declareObject(setElem, (parser, ctx) -> StoredFeatureSet.parse(parser), new ParseField(StoredFeatureSet.TYPE));
        PARSER.declareObject(setElem, (parser, ctx) -> StoredLtrModel.parse(parser), new ParseField(StoredLtrModel.TYPE));
        PARSER.declareObject((b, v) -> b.validation = v, (p, c) -> FeatureValidation.PARSER.apply(p, null), new ParseField("validation"));
        declareStandardFields(PARSER);
    }

    private final transient LtrRankerParserFactory factory;
    private StorableElement element;
    private FeatureValidation validation;
    private LTRStats ltrStats;

    private ValidatingLtrQueryBuilder(LtrRankerParserFactory factory) {
        this.factory = factory;
    }

    public ValidatingLtrQueryBuilder(
        StorableElement element,
        FeatureValidation validation,
        LtrRankerParserFactory factory,
        LTRStats ltrStats
    ) {
        this(factory);
        this.element = Objects.requireNonNull(element);
        this.validation = Objects.requireNonNull(validation);
        this.ltrStats = ltrStats;
    }

    public ValidatingLtrQueryBuilder(StreamInput input, LtrRankerParserFactory factory, LTRStats ltrStats) throws IOException {
        super(input);
        // XXX: hack because AbstractQueryTest does not inject
        // our NamedWriteable to the context.
        String type = input.readString();
        final Reader<StorableElement> reader;
        switch (type) {
            case StoredFeature.TYPE:
                reader = StoredFeature::new;
                break;
            case StoredFeatureSet.TYPE:
                reader = StoredFeatureSet::new;
                break;
            case StoredLtrModel.TYPE:
                reader = StoredLtrModel::new;
                break;
            default:
                throw new IOException("Unsupported storable element [" + type + "]");
        }
        this.element = reader.read(input);
        this.validation = new FeatureValidation(input);
        this.factory = factory;
        this.ltrStats = ltrStats;
    }

    public static ValidatingLtrQueryBuilder fromXContent(XContentParser parser, LtrRankerParserFactory factory, LTRStats ltrStats)
        throws IOException {
        try {
            ValidatingLtrQueryBuilder builder = new ValidatingLtrQueryBuilder(factory);
            PARSER.parse(parser, builder, null);
            if (builder.element == null) {
                throw new ParsingException(
                    parser.getTokenLocation(),
                    "Element of type [" + SUPPORTED_TYPES.stream().collect(joining(",")) + "] is mandatory."
                );
            }
            if (builder.validation == null) {
                throw new ParsingException(parser.getTokenLocation(), "Expected field [" + VALIDATION.getPreferredName() + "]");
            }
            builder.ltrStats(ltrStats);
            return builder;
        } catch (IllegalArgumentException iae) {
            throw new ParsingException(parser.getTokenLocation(), iae.getMessage(), iae);
        }
    }

    @Override
    protected void doWriteTo(StreamOutput out) throws IOException {
        out.writeString(element.getWriteableName());
        element.writeTo(out);
        validation.writeTo(out);
    }

    @Override
    protected void doXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(NAME);
        builder.field(element.type(), element);
        builder.field(VALIDATION.getPreferredName(), validation);
        printBoostAndQueryName(builder);
        builder.endObject();
    }

    @Override
    protected Query doToQuery(QueryShardContext queryShardContext) throws IOException {
        ltrStats.getStat(StatName.LTR_REQUEST_TOTAL_COUNT.getName()).increment();
        try {
            return doToQueryInternal(queryShardContext);
        } catch (Exception e) {
            ltrStats.getStat(StatName.LTR_REQUEST_ERROR_COUNT.getName()).increment();
            throw e;
        }
    }

    private Query doToQueryInternal(QueryShardContext queryShardContext) throws IOException {
        // TODO: should we be passing activeFeatures here?
        LtrQueryContext context = new LtrQueryContext(queryShardContext);
        if (StoredFeature.TYPE.equals(element.type())) {
            Feature feature = ((StoredFeature) element).optimize();
            if (feature instanceof PrecompiledExpressionFeature) {
                // Derived features cannot be tested alone
                return new MatchAllDocsQuery();
            }
            // TODO: support activeFeatures in Validating queries
            return feature.doToQuery(context, null, validation.getParams());
        } else if (StoredFeatureSet.TYPE.equals(element.type())) {
            FeatureSet set = ((StoredFeatureSet) element).optimize();
            LinearRanker ranker = new LinearRanker(new float[set.size()]);
            CompiledLtrModel model = new CompiledLtrModel("validation", set, ranker);
            return RankerQuery.build(model, context, validation.getParams(), false, ltrStats);
        } else if (StoredLtrModel.TYPE.equals(element.type())) {
            CompiledLtrModel model = ((StoredLtrModel) element).compile(factory);
            return RankerQuery.build(model, context, validation.getParams(), false, ltrStats);
        } else {
            throw new QueryShardException(queryShardContext, "Unknown element type [" + element.type() + "]");
        }
    }

    @Override
    protected boolean doEquals(ValidatingLtrQueryBuilder other) {
        return Objects.equals(element, other.element) && Objects.equals(validation, other.validation);
    }

    @Override
    protected int doHashCode() {
        return Objects.hash(element, validation);
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    public StorableElement getElement() {
        return element;
    }

    public FeatureValidation getValidation() {
        return validation;
    }

    public ValidatingLtrQueryBuilder ltrStats(LTRStats ltrStats) {
        this.ltrStats = ltrStats;
        return this;
    }
}
