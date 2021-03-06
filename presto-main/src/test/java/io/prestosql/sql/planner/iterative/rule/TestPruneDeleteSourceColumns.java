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
package io.prestosql.sql.planner.iterative.rule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.prestosql.spi.connector.SchemaTableName;
import io.prestosql.sql.planner.Symbol;
import io.prestosql.sql.planner.assertions.PlanMatchPattern;
import io.prestosql.sql.planner.iterative.rule.test.BaseRuleTest;
import io.prestosql.sql.planner.plan.DeleteNode;
import org.testng.annotations.Test;

import static io.prestosql.sql.planner.assertions.PlanMatchPattern.node;
import static io.prestosql.sql.planner.assertions.PlanMatchPattern.strictProject;
import static io.prestosql.sql.planner.assertions.PlanMatchPattern.values;

public class TestPruneDeleteSourceColumns
        extends BaseRuleTest
{
    @Test
    public void testPruneInputColumn()
    {
        tester().assertThat(new PruneDeleteSourceColumns())
                .on(p -> {
                    Symbol a = p.symbol("a");
                    Symbol rowId = p.symbol("row_id");
                    Symbol partialRows = p.symbol("partial_rows");
                    Symbol fragment = p.symbol("fragment");
                    return p.delete(
                            new SchemaTableName("schema", "table"),
                            p.values(a, rowId),
                            rowId,
                            ImmutableList.of(partialRows, fragment));
                })
                .matches(
                        node(
                                DeleteNode.class,
                                strictProject(
                                        ImmutableMap.of("row_id", PlanMatchPattern.expression("row_id")),
                                        values("a", "row_id"))));
    }

    @Test
    public void testDoNotPruneRowId()
    {
        tester().assertThat(new PruneDeleteSourceColumns())
                .on(p -> {
                    Symbol rowId = p.symbol("row_id");
                    Symbol partialRows = p.symbol("partial_rows");
                    Symbol fragment = p.symbol("fragment");
                    return p.delete(
                            new SchemaTableName("schema", "table"),
                            p.values(rowId),
                            rowId,
                            ImmutableList.of(partialRows, fragment));
                })
                .doesNotFire();
    }
}
