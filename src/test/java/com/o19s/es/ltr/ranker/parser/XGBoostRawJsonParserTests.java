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

package com.o19s.es.ltr.ranker.parser;

import static com.o19s.es.ltr.LtrTestUtils.randomFeature;
import static java.util.Collections.singletonList;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.tests.util.LuceneTestCase;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.opensearch.core.common.ParsingException;

import com.o19s.es.ltr.feature.FeatureSet;
import com.o19s.es.ltr.feature.store.StoredFeatureSet;
import com.o19s.es.ltr.ranker.LtrRanker.FeatureVector;
import com.o19s.es.ltr.ranker.dectree.NaiveAdditiveDecisionTree;

public class XGBoostRawJsonParserTests extends LuceneTestCase {
    private final XGBoostRawJsonParser parser = new XGBoostRawJsonParser();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    public void testSimpleSplit() throws IOException {
        String model = "{"
            + "    \"learner\":{"
            + "        \"attributes\":{},"
            + "        \"feature_names\":[\"feat1\"],"
            + "        \"feature_types\":[\"float\"],"
            + "        \"gradient_booster\":{"
            + "        \"model\":{"
            + "            \"gbtree_model_param\":{"
            + "            \"num_parallel_tree\":\"1\","
            + "            \"num_trees\":\"1\"},"
            + "            \"iteration_indptr\":[0,1],"
            + "            \"tree_info\":[0],"
            + "            \"trees\":[{"
            + "                \"base_weights\":[1E0, 10E0, 0E0],"
            + "                \"categories\":[],"
            + "                \"categories_nodes\":[],"
            + "                \"categories_segments\":[],"
            + "                \"categories_sizes\":[],"
            + "                \"default_left\":[0, 0, 0],"
            + "                \"id\":0,"
            + "                \"left_children\":[2, -1, -1],"
            + "                \"loss_changes\":[0E0, 0E0, 0E0],"
            + "                \"parents\":[2147483647, 0, 0],"
            + "                \"right_children\":[1, -1, -1],"
            + "                \"split_conditions\":[3E0, -1E0, -1E0],"
            + "                \"split_indices\":[0, 0, 0],"
            + "                \"split_type\":[0, 0, 0],"
            + "                \"sum_hessian\":[1E0, 1E0, 1E0],"
            + "                \"tree_param\":{"
            + "                    \"num_deleted\":\"0\","
            + "                    \"num_feature\":\"1\","
            + "                    \"num_nodes\":\"3\","
            + "                    \"size_leaf_vector\":\"1\"}"
            + "                }"
            + "            ]},"
            + "            \"name\":\"gbtree\""
            + "        },"
            + "        \"learner_model_param\":{"
            + "            \"base_score\":\"5E-1\","
            + "            \"boost_from_average\":\"1\","
            + "            \"num_class\":\"0\","
            + "            \"num_feature\":\"1\","
            + "            \"num_target\":\"1\""
            + "        },"
            + "        \"objective\":{"
            + "            \"name\":\"reg:linear\","
            + "            \"reg_loss_param\":{\"scale_pos_weight\":\"1\"}"
            + "        }"
            + "    },"
            + "    \"version\":[2,1,0]"
            + "}";

        FeatureSet set = new StoredFeatureSet("set", singletonList(randomFeature("feat1")));
        NaiveAdditiveDecisionTree tree = parser.parse(set, model);
        FeatureVector featureVector = tree.newFeatureVector(null);
        featureVector.setFeatureScore(0, 2);
        assertEquals(0.0, tree.score(featureVector), Math.ulp(0.1F));

        featureVector.setFeatureScore(0, 4);
        assertEquals(10.0, tree.score(featureVector), Math.ulp(0.1F));
    }

    public void testReadWithLogisticObjective() throws IOException {
        String model = "{"
            + "    \"learner\":{"
            + "        \"attributes\":{},"
            + "        \"feature_names\":[\"feat1\"],"
            + "        \"feature_types\":[\"float\"],"
            + "        \"gradient_booster\":{"
            + "        \"model\":{"
            + "            \"gbtree_model_param\":{"
            + "            \"num_parallel_tree\":\"1\","
            + "            \"num_trees\":\"1\"},"
            + "            \"iteration_indptr\":[0,1],"
            + "            \"tree_info\":[0],"
            + "            \"trees\":[{"
            + "                \"base_weights\":[1E0, -2E-1, 5E-1],"
            + "                \"categories\":[],"
            + "                \"categories_nodes\":[],"
            + "                \"categories_segments\":[],"
            + "                \"categories_sizes\":[],"
            + "                \"default_left\":[0, 0, 0],"
            + "                \"id\":0,"
            + "                \"left_children\":[2, -1, -1],"
            + "                \"loss_changes\":[0E0, 0E0, 0E0],"
            + "                \"parents\":[2147483647, 0, 0],"
            + "                \"right_children\":[1, -1, -1],"
            + "                \"split_conditions\":[3E0, -1E0, -1E0],"
            + "                \"split_indices\":[0, 0, 0],"
            + "                \"split_type\":[0, 0, 0],"
            + "                \"sum_hessian\":[1E0, 1E0, 1E0],"
            + "                \"tree_param\":{"
            + "                    \"num_deleted\":\"0\","
            + "                    \"num_feature\":\"1\","
            + "                    \"num_nodes\":\"3\","
            + "                    \"size_leaf_vector\":\"1\"}"
            + "                }"
            + "            ]},"
            + "            \"name\":\"gbtree\""
            + "        },"
            + "        \"learner_model_param\":{"
            + "            \"base_score\":\"5E-1\","
            + "            \"boost_from_average\":\"1\","
            + "            \"num_class\":\"0\","
            + "            \"num_feature\":\"1\","
            + "            \"num_target\":\"1\""
            + "        },"
            + "        \"objective\":{"
            + "            \"name\":\"reg:logistic\","
            + "            \"reg_loss_param\":{\"scale_pos_weight\":\"1\"}"
            + "        }"
            + "    },"
            + "    \"version\":[2,1,0]"
            + "}";

        FeatureSet set = new StoredFeatureSet("set", singletonList(randomFeature("feat1")));
        NaiveAdditiveDecisionTree tree = parser.parse(set, model);
        FeatureVector v = tree.newFeatureVector(null);
        v.setFeatureScore(0, 2);
        assertEquals(0.62245935F, tree.score(v), Math.ulp(0.62245935F));
        v.setFeatureScore(0, 4);
        assertEquals(0.45016602F, tree.score(v), Math.ulp(0.45016602F));
    }

    public void testBadObjectiveParam() throws IOException {
        String model = "{"
            + "    \"learner\":{"
            + "        \"attributes\":{},"
            + "        \"feature_names\":[\"feat1\", \"feat2\"],"
            + "        \"feature_types\":[\"float\", \"float\"],"
            + "        \"gradient_booster\":{"
            + "        \"model\":{"
            + "            \"gbtree_model_param\":{"
            + "            \"num_parallel_tree\":\"1\","
            + "            \"num_trees\":\"1\"},"
            + "            \"iteration_indptr\":[0,1],"
            + "            \"tree_info\":[0],"
            + "            \"trees\":[{"
            + "                \"base_weights\":[1E0, 10E0, 0E0],"
            + "                \"categories\":[],"
            + "                \"categories_nodes\":[],"
            + "                \"categories_segments\":[],"
            + "                \"categories_sizes\":[],"
            + "                \"default_left\":[0, 0, 0],"
            + "                \"id\":0,"
            + "                \"left_children\":[2, -1, -1],"
            + "                \"loss_changes\":[0E0, 0E0, 0E0],"
            + "                \"parents\":[2147483647, 0, 0],"
            + "                \"right_children\":[1, -1, -1],"
            + "                \"split_conditions\":[3E0, -1E0, -1E0],"
            + "                \"split_indices\":[0, 0, 0],"
            + "                \"split_type\":[0, 0, 0],"
            + "                \"sum_hessian\":[1E0, 1E0, 1E0],"
            + "                \"tree_param\":{"
            + "                    \"num_deleted\":\"0\","
            + "                    \"num_feature\":\"2\","
            + "                    \"num_nodes\":\"3\","
            + "                    \"size_leaf_vector\":\"1\"}"
            + "                }"
            + "            ]},"
            + "            \"name\":\"gbtree\""
            + "        },"
            + "        \"learner_model_param\":{"
            + "            \"base_score\":\"5E-1\","
            + "            \"boost_from_average\":\"1\","
            + "            \"num_class\":\"0\","
            + "            \"num_feature\":\"2\","
            + "            \"num_target\":\"1\""
            + "        },"
            + "        \"objective\":{"
            + "            \"name\":\"reg:invalid\","
            + "            \"reg_loss_param\":{\"scale_pos_weight\":\"1\"}"
            + "        }"
            + "    },"
            + "    \"version\":[2,1,0]"
            + "}";

        FeatureSet set = new StoredFeatureSet("set", singletonList(randomFeature("feat1")));
        assertThat(
            expectThrows(ParsingException.class, () -> parser.parse(set, model)).getMessage(),
            CoreMatchers.containsString("Unable to parse XGBoost object")
        );
    }

    public void testBadFeatureTypeParam() throws IOException {
        String model = "{"
            + "    \"learner\":{"
            + "        \"attributes\":{},"
            + "        \"feature_names\":[\"feat1\"],"
            + "        \"feature_types\":[\"int\"],"
            + "        \"gradient_booster\":{"
            + "        \"model\":{"
            + "            \"gbtree_model_param\":{"
            + "            \"num_parallel_tree\":\"1\","
            + "            \"num_trees\":\"1\"},"
            + "            \"iteration_indptr\":[0,1],"
            + "            \"tree_info\":[0],"
            + "            \"trees\":[{"
            + "                \"base_weights\":[1E0, 10E0, 0E0],"
            + "                \"categories\":[],"
            + "                \"categories_nodes\":[],"
            + "                \"categories_segments\":[],"
            + "                \"categories_sizes\":[],"
            + "                \"default_left\":[0, 0, 0],"
            + "                \"id\":0,"
            + "                \"left_children\":[2, -1, -1],"
            + "                \"loss_changes\":[0E0, 0E0, 0E0],"
            + "                \"parents\":[2147483647, 0, 0],"
            + "                \"right_children\":[1, -1, -1],"
            + "                \"split_conditions\":[3E0, -1E0, -1E0],"
            + "                \"split_indices\":[0, 0, 0],"
            + "                \"split_type\":[0, 0, 0],"
            + "                \"sum_hessian\":[1E0, 1E0, 1E0],"
            + "                \"tree_param\":{"
            + "                    \"num_deleted\":\"0\","
            + "                    \"num_feature\":\"1\","
            + "                    \"num_nodes\":\"3\","
            + "                    \"size_leaf_vector\":\"1\"}"
            + "                }"
            + "            ]},"
            + "            \"name\":\"gbtree\""
            + "        },"
            + "        \"learner_model_param\":{"
            + "            \"base_score\":\"5E-1\","
            + "            \"boost_from_average\":\"1\","
            + "            \"num_class\":\"0\","
            + "            \"num_feature\":\"1\","
            + "            \"num_target\":\"1\""
            + "        },"
            + "        \"objective\":{"
            + "            \"name\":\"reg:linear\","
            + "            \"reg_loss_param\":{\"scale_pos_weight\":\"1\"}"
            + "        }"
            + "    },"
            + "    \"version\":[2,1,0]"
            + "}";

        FeatureSet set = new StoredFeatureSet("set", singletonList(randomFeature("feat1")));
        assertThat(
            expectThrows(ParsingException.class, () -> parser.parse(set, model)).getMessage(),
            CoreMatchers
                .containsString(
                    "The LTR plugin only supports float feature types because "
                        + "Elasticsearch scores are always float32. Found feature type [int] in model"
                )
        );
    }

    public void testMismatchingFeatureList() throws IOException {
        String model = "{"
            + "    \"learner\":{"
            + "        \"attributes\":{},"
            + "        \"feature_names\":[\"feat1\", \"feat2\"],"
            + "        \"feature_types\":[\"float\"],"
            + "        \"gradient_booster\":{"
            + "        \"model\":{"
            + "            \"gbtree_model_param\":{"
            + "            \"num_parallel_tree\":\"1\","
            + "            \"num_trees\":\"1\"},"
            + "            \"iteration_indptr\":[0,1],"
            + "            \"tree_info\":[0],"
            + "            \"trees\":[{"
            + "                \"base_weights\":[1E0, 10E0, 0E0],"
            + "                \"categories\":[],"
            + "                \"categories_nodes\":[],"
            + "                \"categories_segments\":[],"
            + "                \"categories_sizes\":[],"
            + "                \"default_left\":[0, 0, 0],"
            + "                \"id\":0,"
            + "                \"left_children\":[2, -1, -1],"
            + "                \"loss_changes\":[0E0, 0E0, 0E0],"
            + "                \"parents\":[2147483647, 0, 0],"
            + "                \"right_children\":[1, -1, -1],"
            + "                \"split_conditions\":[3E0, -1E0, -1E0],"
            + "                \"split_indices\":[0, 0, 0],"
            + "                \"split_type\":[0, 0, 0],"
            + "                \"sum_hessian\":[1E0, 1E0, 1E0],"
            + "                \"tree_param\":{"
            + "                    \"num_deleted\":\"0\","
            + "                    \"num_feature\":\"2\","
            + "                    \"num_nodes\":\"3\","
            + "                    \"size_leaf_vector\":\"1\"}"
            + "                }"
            + "            ]},"
            + "            \"name\":\"gbtree\""
            + "        },"
            + "        \"learner_model_param\":{"
            + "            \"base_score\":\"5E-1\","
            + "            \"boost_from_average\":\"1\","
            + "            \"num_class\":\"0\","
            + "            \"num_feature\":\"2\","
            + "            \"num_target\":\"1\""
            + "        },"
            + "        \"objective\":{"
            + "            \"name\":\"reg:logistic\","
            + "            \"reg_loss_param\":{\"scale_pos_weight\":\"1\"}"
            + "        }"
            + "    },"
            + "    \"version\":[2,1,0]"
            + "}";

        FeatureSet set = new StoredFeatureSet("set", List.of(randomFeature("feat1"), randomFeature("feat2")));
        assertThat(
            expectThrows(ParsingException.class, () -> parser.parse(set, model)).getMessage(),
            CoreMatchers.containsString("Feature names list and feature types list must have the same length")
        );
    }

    public void testSplitMissingLeftChild() throws IOException {
        String model = "{"
            + "    \"learner\":{"
            + "        \"attributes\":{},"
            + "        \"feature_names\":[\"feat1\"],"
            + "        \"feature_types\":[\"float\"],"
            + "        \"gradient_booster\":{"
            + "        \"model\":{"
            + "            \"gbtree_model_param\":{"
            + "            \"num_parallel_tree\":\"1\","
            + "            \"num_trees\":\"1\"},"
            + "            \"iteration_indptr\":[0,1],"
            + "            \"tree_info\":[0],"
            + "            \"trees\":[{"
            + "                \"base_weights\":[1E0, 10E0, 0E0],"
            + "                \"categories\":[],"
            + "                \"categories_nodes\":[],"
            + "                \"categories_segments\":[],"
            + "                \"categories_sizes\":[],"
            + "                \"default_left\":[0, 0, 0],"
            + "                \"id\":0,"
            + "                \"left_children\":[100, -1, -1],"
            + "                \"loss_changes\":[0E0, 0E0, 0E0],"
            + "                \"parents\":[2147483647, 0, 0],"
            + "                \"right_children\":[1, -1, -1],"
            + "                \"split_conditions\":[3E0, -1E0, -1E0],"
            + "                \"split_indices\":[0, 0, 0],"
            + "                \"split_type\":[0, 0, 0],"
            + "                \"sum_hessian\":[1E0, 1E0, 1E0],"
            + "                \"tree_param\":{"
            + "                    \"num_deleted\":\"0\","
            + "                    \"num_feature\":\"1\","
            + "                    \"num_nodes\":\"3\","
            + "                    \"size_leaf_vector\":\"1\"}"
            + "                }"
            + "            ]},"
            + "            \"name\":\"gbtree\""
            + "        },"
            + "        \"learner_model_param\":{"
            + "            \"base_score\":\"5E-1\","
            + "            \"boost_from_average\":\"1\","
            + "            \"num_class\":\"0\","
            + "            \"num_feature\":\"1\","
            + "            \"num_target\":\"1\""
            + "        },"
            + "        \"objective\":{"
            + "            \"name\":\"reg:linear\","
            + "            \"reg_loss_param\":{\"scale_pos_weight\":\"1\"}"
            + "        }"
            + "    },"
            + "    \"version\":[2,1,0]"
            + "}";

        try {
            FeatureSet set = new StoredFeatureSet("set", singletonList(randomFeature("feat1")));
            parser.parse(set, model);
            fail("Expected an exception");
        } catch (ParsingException e) {
            assertThat(e.getMessage(), CoreMatchers.containsString("Unable to parse XGBoost object"));
            Throwable rootCause = e.getCause().getCause().getCause().getCause().getCause().getCause();
            assertThat(rootCause, CoreMatchers.instanceOf(IllegalArgumentException.class));
            assertThat(rootCause.getMessage(), CoreMatchers.containsString("Child node reference ID [100] is invalid"));
        }
    }

    public void testSplitMissingRightChild() throws IOException {
        String model = "{"
            + "    \"learner\":{"
            + "        \"attributes\":{},"
            + "        \"feature_names\":[\"feat1\"],"
            + "        \"feature_types\":[\"float\"],"
            + "        \"gradient_booster\":{"
            + "        \"model\":{"
            + "            \"gbtree_model_param\":{"
            + "            \"num_parallel_tree\":\"1\","
            + "            \"num_trees\":\"1\"},"
            + "            \"iteration_indptr\":[0,1],"
            + "            \"tree_info\":[0],"
            + "            \"trees\":[{"
            + "                \"base_weights\":[1E0, 10E0, 0E0],"
            + "                \"categories\":[],"
            + "                \"categories_nodes\":[],"
            + "                \"categories_segments\":[],"
            + "                \"categories_sizes\":[],"
            + "                \"default_left\":[0, 0, 0],"
            + "                \"id\":0,"
            + "                \"left_children\":[1, -1, -1],"
            + "                \"loss_changes\":[0E0, 0E0, 0E0],"
            + "                \"parents\":[2147483647, 0, 0],"
            + "                \"right_children\":[100, -1, -1],"
            + "                \"split_conditions\":[3E0, -1E0, -1E0],"
            + "                \"split_indices\":[0, 0, 0],"
            + "                \"split_type\":[0, 0, 0],"
            + "                \"sum_hessian\":[1E0, 1E0, 1E0],"
            + "                \"tree_param\":{"
            + "                    \"num_deleted\":\"0\","
            + "                    \"num_feature\":\"1\","
            + "                    \"num_nodes\":\"3\","
            + "                    \"size_leaf_vector\":\"1\"}"
            + "                }"
            + "            ]},"
            + "            \"name\":\"gbtree\""
            + "        },"
            + "        \"learner_model_param\":{"
            + "            \"base_score\":\"5E-1\","
            + "            \"boost_from_average\":\"1\","
            + "            \"num_class\":\"0\","
            + "            \"num_feature\":\"1\","
            + "            \"num_target\":\"1\""
            + "        },"
            + "        \"objective\":{"
            + "            \"name\":\"reg:linear\","
            + "            \"reg_loss_param\":{\"scale_pos_weight\":\"1\"}"
            + "        }"
            + "    },"
            + "    \"version\":[2,1,0]"
            + "}";

        try {
            FeatureSet set = new StoredFeatureSet("set", singletonList(randomFeature("feat1")));
            parser.parse(set, model);
            fail("Expected an exception");
        } catch (ParsingException e) {
            assertThat(e.getMessage(), CoreMatchers.containsString("Unable to parse XGBoost object"));
            Throwable rootCause = e.getCause().getCause().getCause().getCause().getCause().getCause();
            assertThat(rootCause, CoreMatchers.instanceOf(IllegalArgumentException.class));
            assertThat(rootCause.getMessage(), CoreMatchers.containsString("Child node reference ID [100] is invalid"));
        }
    }

    public void testBadStruct() throws IOException {
        String model = "[{"
            + "    \"learner\":{"
            + "        \"attributes\":{},"
            + "        \"feature_names\":[\"feat1\", \"feat2\"],"
            + "        \"feature_types\":[\"float\", \"float\"],"
            + "        \"gradient_booster\":{"
            + "        \"model\":{"
            + "            \"gbtree_model_param\":{"
            + "            \"num_parallel_tree\":\"1\","
            + "            \"num_trees\":\"1\"},"
            + "            \"iteration_indptr\":[0,1],"
            + "            \"tree_info\":[0],"
            + "            \"trees\":[{"
            + "                \"base_weights\":[1E0, 10E0, 0E0],"
            + "                \"categories\":[],"
            + "                \"categories_nodes\":[],"
            + "                \"categories_segments\":[],"
            + "                \"categories_sizes\":[],"
            + "                \"default_left\":[0, 0, 0],"
            + "                \"id\":0,"
            + "                \"left_children\":[2, -1, -1],"
            + "                \"loss_changes\":[0E0, 0E0, 0E0],"
            + "                \"parents\":[2147483647, 0, 0],"
            + "                \"right_children\":[1, -1, -1],"
            + "                \"split_conditions\":[3E0, -1E0, -1E0],"
            + "                \"split_indices\":[0, 0, 0],"
            + "                \"split_type\":[0, 0, 0],"
            + "                \"sum_hessian\":[1E0, 1E0, 1E0],"
            + "                \"tree_param\":{"
            + "                    \"num_deleted\":\"0\","
            + "                    \"num_feature\":\"2\","
            + "                    \"num_nodes\":\"3\","
            + "                    \"size_leaf_vector\":\"1\"}"
            + "                }"
            + "            ]},"
            + "            \"name\":\"gbtree\""
            + "        },"
            + "        \"learner_model_param\":{"
            + "            \"base_score\":\"5E-1\","
            + "            \"boost_from_average\":\"1\","
            + "            \"num_class\":\"0\","
            + "            \"num_feature\":\"2\","
            + "            \"num_target\":\"1\""
            + "        },"
            + "        \"objective\":{"
            + "            \"name\":\"reg:linear\","
            + "            \"reg_loss_param\":{\"scale_pos_weight\":\"1\"}"
            + "        }"
            + "    },"
            + "    \"version\":[2,1,0]"
            + "}]";
        FeatureSet set = new StoredFeatureSet("set", singletonList(randomFeature("feat1")));
        assertThat(
            expectThrows(ParsingException.class, () -> parser.parse(set, model)).getMessage(),
            CoreMatchers.containsString("Expected [START_OBJECT] but got")
        );
    }

    public void testMissingFeat() throws IOException {
        String model = "{"
            + "    \"learner\":{"
            + "        \"attributes\":{},"
            + "        \"feature_names\":[\"feat1\", \"feat2\"],"
            + "        \"feature_types\":[\"float\",\"float\"],"
            + "        \"gradient_booster\":{"
            + "        \"model\":{"
            + "            \"gbtree_model_param\":{"
            + "            \"num_parallel_tree\":\"1\","
            + "            \"num_trees\":\"1\"},"
            + "            \"iteration_indptr\":[0,1],"
            + "            \"tree_info\":[0],"
            + "            \"trees\":[{"
            + "                \"base_weights\":[1E0, 10E0, 0E0],"
            + "                \"categories\":[],"
            + "                \"categories_nodes\":[],"
            + "                \"categories_segments\":[],"
            + "                \"categories_sizes\":[],"
            + "                \"default_left\":[0, 0, 0],"
            + "                \"id\":0,"
            + "                \"left_children\":[2, -1, -1],"
            + "                \"loss_changes\":[0E0, 0E0, 0E0],"
            + "                \"parents\":[2147483647, 0, 0],"
            + "                \"right_children\":[1, -1, -1],"
            + "                \"split_conditions\":[3E0, -1E0, -1E0],"
            + "                \"split_indices\":[0, 0, 100],"
            + "                \"split_type\":[0, 0, 0],"
            + "                \"sum_hessian\":[1E0, 1E0, 1E0],"
            + "                \"tree_param\":{"
            + "                    \"num_deleted\":\"0\","
            + "                    \"num_feature\":\"2\","
            + "                    \"num_nodes\":\"3\","
            + "                    \"size_leaf_vector\":\"1\"}"
            + "                }"
            + "            ]},"
            + "            \"name\":\"gbtree\""
            + "        },"
            + "        \"learner_model_param\":{"
            + "            \"base_score\":\"5E-1\","
            + "            \"boost_from_average\":\"1\","
            + "            \"num_class\":\"0\","
            + "            \"num_feature\":\"2\","
            + "            \"num_target\":\"1\""
            + "        },"
            + "        \"objective\":{"
            + "            \"name\":\"reg:linear\","
            + "            \"reg_loss_param\":{\"scale_pos_weight\":\"1\"}"
            + "        }"
            + "    },"
            + "    \"version\":[2,1,0]"
            + "}";
        FeatureSet set = new StoredFeatureSet("set", singletonList(randomFeature("feat1234")));
        assertThat(
            expectThrows(ParsingException.class, () -> parser.parse(set, model)).getMessage(),
            CoreMatchers.containsString("Unknown features in model: [feat1, feat2]")
        );
    }

    public void testMultiTreeEnsemble() throws IOException {
        String model = "{"
            + "    \"learner\":{"
            + "        \"attributes\":{},"
            + "        \"feature_names\":[\"feat1\", \"feat2\"],"
            + "        \"feature_types\":[\"float\", \"float\"],"
            + "        \"gradient_booster\":{"
            + "        \"model\":{"
            + "            \"gbtree_model_param\":{"
            + "            \"num_parallel_tree\":\"1\","
            + "            \"num_trees\":\"2\"},"
            + "            \"iteration_indptr\":[0,1,2],"
            + "            \"tree_info\":[0, 1],"
            + "            \"trees\":[{"
            + "                \"base_weights\":[1E0, 10E0, 0E0],"
            + "                \"categories\":[],"
            + "                \"categories_nodes\":[],"
            + "                \"categories_segments\":[],"
            + "                \"categories_sizes\":[],"
            + "                \"default_left\":[0, 0, 0],"
            + "                \"id\":0,"
            + "                \"left_children\":[2, -1, -1],"
            + "                \"loss_changes\":[0E0, 0E0, 0E0],"
            + "                \"parents\":[2147483647, 0, 0],"
            + "                \"right_children\":[1, -1, -1],"
            + "                \"split_conditions\":[3E0, -1E0, -1E0],"
            + "                \"split_indices\":[0, 0, 0],"
            + "                \"split_type\":[0, 0, 0],"
            + "                \"sum_hessian\":[1E0, 1E0, 1E0],"
            + "                \"tree_param\":{"
            + "                    \"num_deleted\":\"0\","
            + "                    \"num_feature\":\"2\","
            + "                    \"num_nodes\":\"3\","
            + "                    \"size_leaf_vector\":\"1\"}"
            + "                },"
            + "                {"
            + "                \"base_weights\":[1E0, 5E0, 0E0],"
            + "                \"categories\":[],"
            + "                \"categories_nodes\":[],"
            + "                \"categories_segments\":[],"
            + "                \"categories_sizes\":[],"
            + "                \"default_left\":[0, 0, 0],"
            + "                \"id\":1,"
            + "                \"left_children\":[2, -1, -1],"
            + "                \"loss_changes\":[0E0, 0E0, 0E0],"
            + "                \"parents\":[2147483647, 0, 0],"
            + "                \"right_children\":[1, -1, -1],"
            + "                \"split_conditions\":[3E0, -1E0, -1E0],"
            + "                \"split_indices\":[1, 1, 1],"
            + "                \"split_type\":[0, 0, 0],"
            + "                \"sum_hessian\":[1E0, 1E0, 1E0],"
            + "                \"tree_param\":{"
            + "                    \"num_deleted\":\"0\","
            + "                    \"num_feature\":\"2\","
            + "                    \"num_nodes\":\"3\","
            + "                    \"size_leaf_vector\":\"1\"}"
            + "                }"
            + "            ]},"
            + "            \"name\":\"gbtree\""
            + "        },"
            + "        \"learner_model_param\":{"
            + "            \"base_score\":\"5E-1\","
            + "            \"boost_from_average\":\"1\","
            + "            \"num_class\":\"0\","
            + "            \"num_feature\":\"2\","
            + "            \"num_target\":\"1\""
            + "        },"
            + "        \"objective\":{"
            + "            \"name\":\"reg:linear\","
            + "            \"reg_loss_param\":{\"scale_pos_weight\":\"1\"}"
            + "        }"
            + "    },"
            + "    \"version\":[2,1,0]"
            + "}";

        FeatureSet set = new StoredFeatureSet("set", List.of(randomFeature("feat1"), randomFeature("feat2")));
        NaiveAdditiveDecisionTree tree = parser.parse(set, model);
        FeatureVector featureVector = tree.newFeatureVector(null);
        featureVector.setFeatureScore(0, 2);
        featureVector.setFeatureScore(1, 2);
        assertEquals(0.0, tree.score(featureVector), Math.ulp(0.1F));

        featureVector.setFeatureScore(0, 4);
        featureVector.setFeatureScore(1, 4);
        assertEquals(15.0, tree.score(featureVector), Math.ulp(0.1F));
    }

    public void testFeatureReordering() throws IOException {
        String model = "{"
            + "    \"learner\":{"
            + "        \"attributes\":{},"
            + "        \"feature_names\":[\"feat1\", \"feat2\"],"
            + "        \"feature_types\":[\"float\", \"float\"],"
            + "        \"gradient_booster\":{"
            + "        \"model\":{"
            + "            \"gbtree_model_param\":{"
            + "            \"num_parallel_tree\":\"1\","
            + "            \"num_trees\":\"1\"},"
            + "            \"iteration_indptr\":[0,1],"
            + "            \"tree_info\":[0],"
            + "            \"trees\":[{"
            + "                \"base_weights\":[1E0, 10E0, 0E0],"
            + "                \"categories\":[],"
            + "                \"categories_nodes\":[],"
            + "                \"categories_segments\":[],"
            + "                \"categories_sizes\":[],"
            + "                \"default_left\":[0, 0, 0],"
            + "                \"id\":0,"
            + "                \"left_children\":[2, -1, -1],"
            + "                \"loss_changes\":[0E0, 0E0, 0E0],"
            + "                \"parents\":[2147483647, 0, 0],"
            + "                \"right_children\":[1, -1, -1],"
            + "                \"split_conditions\":[3E0, -1E0, -1E0],"
            + "                \"split_indices\":[0, 0, 0],"
            + "                \"split_type\":[0, 0, 0],"
            + "                \"sum_hessian\":[1E0, 1E0, 1E0],"
            + "                \"tree_param\":{"
            + "                    \"num_deleted\":\"0\","
            + "                    \"num_feature\":\"2\","
            + "                    \"num_nodes\":\"3\","
            + "                    \"size_leaf_vector\":\"1\"}"
            + "                }"
            + "            ]},"
            + "            \"name\":\"gbtree\""
            + "        },"
            + "        \"learner_model_param\":{"
            + "            \"base_score\":\"5E-1\","
            + "            \"boost_from_average\":\"1\","
            + "            \"num_class\":\"0\","
            + "            \"num_feature\":\"2\","
            + "            \"num_target\":\"1\""
            + "        },"
            + "        \"objective\":{"
            + "            \"name\":\"reg:linear\","
            + "            \"reg_loss_param\":{\"scale_pos_weight\":\"1\"}"
            + "        }"
            + "    },"
            + "    \"version\":[2,1,0]"
            + "}";

        FeatureSet set = new StoredFeatureSet("set", List.of(randomFeature("feat2"), randomFeature("feat1")));
        NaiveAdditiveDecisionTree tree = parser.parse(set, model);
        FeatureVector featureVector = tree.newFeatureVector(null);
        featureVector.setFeatureScore(0, 4); // feat2
        featureVector.setFeatureScore(1, 2); // feat1
        assertEquals(0.0, tree.score(featureVector), Math.ulp(0.1F));

        FeatureSet setNoReorder = new StoredFeatureSet("set", List.of(randomFeature("feat1"), randomFeature("feat2")));
        NaiveAdditiveDecisionTree treeNoReorder = parser.parse(setNoReorder, model);
        FeatureVector featureVectorNoReorder = treeNoReorder.newFeatureVector(null);
        featureVectorNoReorder.setFeatureScore(0, 2); // feat1
        featureVectorNoReorder.setFeatureScore(1, 4); // feat2
        assertEquals(0.0, treeNoReorder.score(featureVectorNoReorder), Math.ulp(0.1F));
    }
}
