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

import static java.util.Collections.unmodifiableMap;
import static org.hamcrest.Matchers.equalTo;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.tests.util.LuceneTestCase;
import org.junit.After;
import org.junit.Before;
import org.opensearch.common.lucene.Lucene;
import org.opensearch.ltr.stats.LTRStat;
import org.opensearch.ltr.stats.LTRStats;
import org.opensearch.ltr.stats.StatName;
import org.opensearch.ltr.stats.suppliers.CounterSupplier;

public class ExplorerQueryTests extends LuceneTestCase {
    private Directory dir;
    private IndexReader reader;
    private IndexSearcher searcher;
    private LTRStats ltrStats;

    // Some simple documents to index
    private final String[] docs = new String[] {
        "how now brown cow",
        "brown is the color of cows",
        "brown cow",
        "banana cows are yummy",
        "dance with monkeys and do not stop to dance",
        "break on through to the other side... break on through to the other side... break on through to the other side" };

    @Before
    public void setupIndex() throws Exception {
        dir = new ByteBuffersDirectory();

        try (IndexWriter indexWriter = new IndexWriter(dir, new IndexWriterConfig(Lucene.STANDARD_ANALYZER))) {
            for (int i = 0; i < docs.length; i++) {
                Document doc = new Document();
                doc.add(new Field("_id", Integer.toString(i + 1), StoredField.TYPE));
                doc.add(newTextField("text", docs[i], Field.Store.YES));
                indexWriter.addDocument(doc);
            }
        }

        reader = DirectoryReader.open(dir);
        searcher = new IndexSearcher(reader);
        Map<String, LTRStat<?>> stats = new HashMap<>();
        stats.put(StatName.LTR_REQUEST_TOTAL_COUNT.getName(), new LTRStat<>(false, new CounterSupplier()));
        stats.put(StatName.LTR_REQUEST_ERROR_COUNT.getName(), new LTRStat<>(false, new CounterSupplier()));
        ltrStats = new LTRStats(unmodifiableMap(stats));
    }

    @After
    public void cleanup() throws Exception {
        try {
            reader.close();
        } finally {
            dir.close();
        }
    }

    public void testQuery() throws Exception {
        Query q = new TermQuery(new Term("text", "cow"));
        String statsType = "sum_raw_tf";

        ExplorerQuery eq = new ExplorerQuery(q, statsType, ltrStats);

        // Basic query check, should match 2 docs
        assertThat(searcher.count(eq), equalTo(2));

        // Verify explain
        TopDocs docs = searcher.search(eq, 4);
        Explanation explanation = searcher.explain(eq, docs.scoreDocs[0].doc);
        assertThat(explanation.toString().trim(), equalTo("1.0 = Stat Score: sum_raw_tf"));
    }

    public void testQueryWithEmptyResults() throws Exception {
        Query q = new TermQuery(new Term("text", "xxxxxxxxxxxxxxxxxx"));

        String statsType = "sum_raw_tf";

        ExplorerQuery eq = new ExplorerQuery(q, statsType, ltrStats);

        // Basic query check, should match 0 docs
        assertThat(searcher.count(eq), equalTo(0));

        // Verify explain
        TopDocs docs = searcher.search(eq, 4);
        assertThat(docs.scoreDocs.length, equalTo(0));
    }

    public void testQueryWithTermPositionAverage() throws Exception {
        Query q = new TermQuery(new Term("text", "dance"));
        String statsType = "avg_raw_tp";

        ExplorerQuery eq = new ExplorerQuery(q, statsType, ltrStats);

        // Basic query check, should match 1 docs
        assertThat(searcher.count(eq), equalTo(1));

        // Verify explain
        TopDocs docs = searcher.search(eq, 5);
        Explanation explanation = searcher.explain(eq, docs.scoreDocs[0].doc);
        assertThat(explanation.toString().trim(), equalTo("5.0 = Stat Score: avg_raw_tp"));
    }

    public void testQueryWithTermPositionMax() throws Exception {
        Query q = new TermQuery(new Term("text", "dance"));
        String statsType = "max_raw_tp";

        ExplorerQuery eq = new ExplorerQuery(q, statsType, ltrStats);

        // Basic query check, should match 1 docs
        assertThat(searcher.count(eq), equalTo(1));

        // Verify explain
        TopDocs docs = searcher.search(eq, 5);
        Explanation explanation = searcher.explain(eq, docs.scoreDocs[0].doc);
        assertThat(explanation.toString().trim(), equalTo("9.0 = Stat Score: max_raw_tp"));
    }

    public void testQueryWithTermPositionMin() throws Exception {
        Query q = new TermQuery(new Term("text", "dance"));
        String statsType = "min_raw_tp";

        ExplorerQuery eq = new ExplorerQuery(q, statsType, ltrStats);

        // Basic query check, should match 1 docs
        assertThat(searcher.count(eq), equalTo(1));

        // Verify explain
        TopDocs docs = searcher.search(eq, 5);
        Explanation explanation = searcher.explain(eq, docs.scoreDocs[0].doc);
        assertThat(explanation.toString().trim(), equalTo("1.0 = Stat Score: min_raw_tp"));
    }

    public void testQueryWithTermPositionMinWithTwoTerms() throws Exception {
        TermQuery tq1 = new TermQuery(new Term("text", "stop"));
        TermQuery tq2 = new TermQuery(new Term("text", "hip-hop"));
        TermQuery tq3 = new TermQuery(new Term("text", "monkeys"));

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(tq1, BooleanClause.Occur.SHOULD);
        builder.add(tq2, BooleanClause.Occur.SHOULD);
        builder.add(tq3, BooleanClause.Occur.SHOULD);

        Query q = builder.build();
        String statsType = "min_raw_tp";

        ExplorerQuery eq = new ExplorerQuery(q, statsType, ltrStats);

        // Verify score is 5 (5 unique terms)
        TopDocs docs = searcher.search(eq, 4);

        assertThat(docs.scoreDocs[0].score, equalTo(3.0f));
    }

    public void testQueryWithTermPositionMaxWithTwoTerms() throws Exception {
        TermQuery tq1 = new TermQuery(new Term("text", "stop"));
        TermQuery tq2 = new TermQuery(new Term("text", "hip-hop"));
        TermQuery tq3 = new TermQuery(new Term("text", "monkeys"));

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(tq1, BooleanClause.Occur.SHOULD);
        builder.add(tq2, BooleanClause.Occur.SHOULD);
        builder.add(tq3, BooleanClause.Occur.SHOULD);

        Query q = builder.build();
        String statsType = "max_raw_tp";

        ExplorerQuery eq = new ExplorerQuery(q, statsType, ltrStats);

        // Verify score is 5 (5 unique terms)
        TopDocs docs = searcher.search(eq, 4);

        assertThat(docs.scoreDocs[0].score, equalTo(7.0f));
    }

    public void testQueryWithTermPositionAvgWithTwoTerms() throws Exception {
        TermQuery tq1 = new TermQuery(new Term("text", "stop"));
        TermQuery tq2 = new TermQuery(new Term("text", "hip-hop"));
        TermQuery tq3 = new TermQuery(new Term("text", "monkeys"));

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(tq1, BooleanClause.Occur.SHOULD);
        builder.add(tq2, BooleanClause.Occur.SHOULD);
        builder.add(tq3, BooleanClause.Occur.SHOULD);

        Query q = builder.build();
        String statsType = "avg_raw_tp";

        ExplorerQuery eq = new ExplorerQuery(q, statsType, ltrStats);

        // Verify score is 5 (5 unique terms)
        TopDocs docs = searcher.search(eq, 4);

        assertThat(docs.scoreDocs[0].score, equalTo(5.0f));
    }

    public void testQueryWithTermPositionAvgWithNoTerm() throws Exception {
        Query q = new TermQuery(new Term("text", "xxxxxxxxxxxxxxxxxx"));
        String statsType = "avg_raw_tp";

        ExplorerQuery eq = new ExplorerQuery(q, statsType, ltrStats);

        // Basic query check, should match 1 docs
        assertThat(searcher.count(eq), equalTo(0));

        // Verify explain
        TopDocs docs = searcher.search(eq, 6);

        assertThat(docs.scoreDocs.length, equalTo(0));
    }

    public void testBooleanQuery() throws Exception {
        TermQuery tq1 = new TermQuery(new Term("text", "cow"));
        TermQuery tq2 = new TermQuery(new Term("text", "brown"));
        TermQuery tq3 = new TermQuery(new Term("text", "how"));

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(tq1, BooleanClause.Occur.SHOULD);
        builder.add(tq2, BooleanClause.Occur.SHOULD);
        builder.add(tq3, BooleanClause.Occur.SHOULD);

        Query q = builder.build();
        String statsType = "sum_raw_tf";

        ExplorerQuery eq = new ExplorerQuery(q, statsType, ltrStats);

        // Verify tf score
        TopDocs docs = searcher.search(eq, 4);
        assertThat(docs.scoreDocs[0].score, equalTo(3.0f));
    }

    public void testUniqueTerms() throws Exception {
        TermQuery tq1 = new TermQuery(new Term("text", "how"));
        TermQuery tq2 = new TermQuery(new Term("text", "now"));
        TermQuery tq3 = new TermQuery(new Term("text", "brown"));
        TermQuery tq4 = new TermQuery(new Term("text", "cow"));
        TermQuery tq5 = new TermQuery(new Term("text", "cow"));
        TermQuery tq6 = new TermQuery(new Term("text", "not_here"));

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(tq1, BooleanClause.Occur.MUST);
        builder.add(tq2, BooleanClause.Occur.MUST);
        builder.add(tq3, BooleanClause.Occur.MUST);
        builder.add(tq4, BooleanClause.Occur.MUST);
        builder.add(tq6, BooleanClause.Occur.MUST);

        Query q = builder.build();
        String statsType = "unique_terms_count";

        ExplorerQuery eq = new ExplorerQuery(q, statsType, ltrStats);

        // Verify score is 5 (5 unique terms)
        TopDocs docs = searcher.search(eq, 4);

        assertThat(docs.scoreDocs[0].score, equalTo(5.0f));
    }

    public void testInvalidStat() throws Exception {
        Query q = new TermQuery(new Term("text", "cow"));
        String statsType = "sum_invalid_stat";

        ExplorerQuery eq = new ExplorerQuery(q, statsType, ltrStats);

        expectThrows(RuntimeException.class, () -> searcher.search(eq, 4));
    }
}
