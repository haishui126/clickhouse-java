package com.clickhouse.client;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import com.clickhouse.client.data.ClickHouseDateTimeValue;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ClickHouseParameterizedQueryTest {
    @DataProvider(name = "queryWithoutParameterProvider")
    private Object[][] getQueriesWithoutAnyParameter() {
        return new Object[][] { { "1" }, { "select 1" }, { "select 1::Float32" } };
    }

    @Test(groups = { "unit" })
    public void testApplyCollection() {
        String query = "select :param1::String";
        ClickHouseParameterizedQuery q = ClickHouseParameterizedQuery.of(query);
        Assert.assertTrue(q.getOriginalQuery() == query);
        Assert.assertEquals(q.apply((Collection<String>) null), "select NULL::String");
        Assert.assertEquals(q.apply(Collections.emptyList()), "select NULL::String");
        Assert.assertEquals(q.apply(Collections.emptySet()), "select NULL::String");
        Assert.assertEquals(q.apply(Arrays.asList(new String[] { "first", "last" })), "select first::String");

        query = "select :param1::String,:param2 + 1 as result";
        q = ClickHouseParameterizedQuery.of(query);
        Assert.assertTrue(q.getOriginalQuery() == query);
        Assert.assertEquals(q.apply((Collection<String>) null), "select NULL::String,NULL + 1 as result");
        Assert.assertEquals(q.apply(Collections.emptyList()), "select NULL::String,NULL + 1 as result");
        Assert.assertEquals(q.apply(Collections.emptySet()), "select NULL::String,NULL + 1 as result");
        Assert.assertEquals(q.apply(Arrays.asList(new String[] { "first" })),
                "select first::String,NULL + 1 as result");
        Assert.assertEquals(q.apply(Arrays.asList(new String[] { "first", "last" })),
                "select first::String,last + 1 as result");
        Assert.assertEquals(q.apply(Arrays.asList(new String[] { "first", "last", "more" })),
                "select first::String,last + 1 as result");

        query = "select :p1 p1, :p2 p2, :p1 p3";
        q = ClickHouseParameterizedQuery.of(query);
        Assert.assertTrue(q.getOriginalQuery() == query);
        Assert.assertEquals(q.apply((Collection<String>) null), "select NULL p1, NULL p2, NULL p3");
        Assert.assertEquals(q.apply(Collections.emptyList()), "select NULL p1, NULL p2, NULL p3");
        Assert.assertEquals(q.apply(Collections.emptySet()), "select NULL p1, NULL p2, NULL p3");
        Assert.assertEquals(q.apply(Arrays.asList(new String[] { "first" })),
                "select first p1, NULL p2, first p3");
        Assert.assertEquals(q.apply(Arrays.asList(new String[] { "first", "last" })),
                "select first p1, last p2, first p3");
        Assert.assertEquals(q.apply(Arrays.asList(new String[] { "first", "last", "more" })),
                "select first p1, last p2, first p3");
    }

    @Test(groups = { "unit" })
    public void testApplyObjects() {
        String query = "select :param1::String";
        ClickHouseParameterizedQuery q = ClickHouseParameterizedQuery.of(query);
        Assert.assertTrue(q.getOriginalQuery() == query);
        Assert.assertEquals(q.apply((Object) null), "select NULL::String");
        Assert.assertEquals(q.apply((Object) null, (Object) null), "select NULL::String");
        Assert.assertEquals(q.apply('a'), "select 97::String");
        Assert.assertEquals(q.apply(1, (Object) null), "select 1::String");
        Assert.assertEquals(q.apply(Collections.singletonList('a')), "select (97)::String");
        Assert.assertEquals(q.apply(Arrays.asList(1, null)), "select (1,NULL)::String");

        query = "select :param1::String,:param2 + 1 as result";
        q = ClickHouseParameterizedQuery.of(query);
        Assert.assertTrue(q.getOriginalQuery() == query);
        Assert.assertEquals(q.apply((Object) null), "select NULL::String,NULL + 1 as result");
        Assert.assertEquals(q.apply((Object) null, (Object) null), "select NULL::String,NULL + 1 as result");
        Assert.assertEquals(q.apply('a'), "select 97::String,NULL + 1 as result");
        Assert.assertEquals(q.apply(1, (Object) null), "select 1::String,NULL + 1 as result");
        Assert.assertEquals(q.apply(ClickHouseDateTimeValue.ofNull(3).update(1), (Object) null),
                "select '1970-01-01 00:00:00.001'::String,NULL + 1 as result");
        Assert.assertEquals(q.apply(Collections.singletonList('a')), "select (97)::String,NULL + 1 as result");
        Assert.assertEquals(q.apply(Arrays.asList(1, null)), "select (1,NULL)::String,NULL + 1 as result");
        Assert.assertEquals(q.apply(Arrays.asList(ClickHouseDateTimeValue.ofNull(3).update(1), null)),
                "select ('1970-01-01 00:00:00.001',NULL)::String,NULL + 1 as result");

        query = "select :p1 p1, :p2 p2, :p1 p3";
        q = ClickHouseParameterizedQuery.of(query);
        Assert.assertTrue(q.getOriginalQuery() == query);
        Assert.assertEquals(q.apply((Object) null), "select NULL p1, NULL p2, NULL p3");
        Assert.assertEquals(q.apply((Object) null, (Object) null), "select NULL p1, NULL p2, NULL p3");
        Assert.assertEquals(q.apply('a'), "select 97 p1, NULL p2, 97 p3");
        Assert.assertEquals(q.apply(1, (Object) null), "select 1 p1, NULL p2, 1 p3");
        Assert.assertEquals(q.apply(ClickHouseDateTimeValue.ofNull(3).update(1), (Object) null),
                "select '1970-01-01 00:00:00.001' p1, NULL p2, '1970-01-01 00:00:00.001' p3");
        Assert.assertEquals(q.apply(Collections.singletonList('a')), "select (97) p1, NULL p2, (97) p3");
        Assert.assertEquals(q.apply(Arrays.asList(1, null)), "select (1,NULL) p1, NULL p2, (1,NULL) p3");
        Assert.assertEquals(q.apply(Arrays.asList(ClickHouseDateTimeValue.ofNull(3).update(1), null)),
                "select ('1970-01-01 00:00:00.001',NULL) p1, NULL p2, ('1970-01-01 00:00:00.001',NULL) p3");
        Assert.assertEquals(q.apply(new StringBuilder("321"), new StringBuilder("123"), new StringBuilder("456")),
                "select 321 p1, 123 p2, 321 p3");
    }

    @Test(groups = { "unit" })
    public void testApplyMap() {
        String query = "select :param1::String";
        ClickHouseParameterizedQuery q = ClickHouseParameterizedQuery.of(query);
        Assert.assertTrue(q.getOriginalQuery() == query);
        Assert.assertEquals(q.apply((Map<String, String>) null), "select NULL::String");
        Assert.assertEquals(q.apply(Collections.emptyMap()), "select NULL::String");
        Assert.assertEquals(q.apply(Collections.singletonMap("key", "value")), "select NULL::String");
        Assert.assertEquals(q.apply(Collections.singletonMap("param1", "value")), "select value::String");

        query = "select :param1::String,:param2 + 1 as result";
        q = ClickHouseParameterizedQuery.of(query);
        Assert.assertTrue(q.getOriginalQuery() == query);
        Assert.assertEquals(q.apply((Map<String, String>) null), "select NULL::String,NULL + 1 as result");
        Assert.assertEquals(q.apply(Collections.emptyMap()), "select NULL::String,NULL + 1 as result");
        Map<String, String> map = new HashMap<>();
        map.put("param2", "v2");
        map.put("param1", "v1");
        map.put("param3", "v3");
        Assert.assertEquals(q.apply(Collections.singletonMap("key", "value")),
                "select NULL::String,NULL + 1 as result");
        Assert.assertEquals(q.apply(Collections.singletonMap("param2", "value")),
                "select NULL::String,value + 1 as result");
        Assert.assertEquals(q.apply(map), "select v1::String,v2 + 1 as result");
    }

    @Test(groups = { "unit" })
    public void testApplyStrings() {
        String query = "select :param1::String";
        ClickHouseParameterizedQuery q = ClickHouseParameterizedQuery.of(query);
        Assert.assertTrue(q.getOriginalQuery() == query);
        Assert.assertEquals(q.apply((String) null), "select null::String");
        Assert.assertEquals(q.apply((String) null, (String) null), "select null::String");
        Assert.assertEquals(q.apply("'a'"), "select 'a'::String");
        Assert.assertEquals(q.apply("1", (String) null), "select 1::String");

        query = "select :param1::String,:param2 + 1 as result";
        q = ClickHouseParameterizedQuery.of(query);
        Assert.assertTrue(q.getOriginalQuery() == query);
        Assert.assertEquals(q.apply((String) null), "select null::String,NULL + 1 as result");
        Assert.assertEquals(q.apply((String) null, (String) null), "select null::String,null + 1 as result");
        Assert.assertEquals(q.apply("'a'"), "select 'a'::String,NULL + 1 as result");
        Assert.assertEquals(q.apply("1", (String) null), "select 1::String,null + 1 as result");

        query = "select :p1 p1, :p2 p2, :p1 p3";
        q = ClickHouseParameterizedQuery.of(query);
        Assert.assertTrue(q.getOriginalQuery() == query);
        Assert.assertEquals(q.apply((String) null), "select null p1, NULL p2, null p3");
        Assert.assertEquals(q.apply((String) null, (String) null), "select null p1, null p2, null p3");
        Assert.assertEquals(q.apply("'a'"), "select 'a' p1, NULL p2, 'a' p3");
        Assert.assertEquals(q.apply("1", (String) null), "select 1 p1, null p2, 1 p3");
        Assert.assertEquals(q.apply("1", "2", "3"), "select 1 p1, 2 p2, 1 p3");
    }

    @Test(groups = { "unit" })
    public void testInvalidQuery() {
        Assert.assertThrows(IllegalArgumentException.class, () -> ClickHouseParameterizedQuery.of(null));
        Assert.assertThrows(IllegalArgumentException.class, () -> ClickHouseParameterizedQuery.of(""));
    }

    @Test(dataProvider = "queryWithoutParameterProvider", groups = { "unit" })
    public void testQueryWithoutAnyParameter(String query) {
        ClickHouseParameterizedQuery q = ClickHouseParameterizedQuery.of(query);
        Assert.assertTrue(q.getOriginalQuery() == query);
        Assert.assertTrue(q.apply((String) null) == query);
        Assert.assertTrue(q.apply((Object) null) == query);
        Assert.assertTrue(q.apply((Collection<String>) null) == query);
        Assert.assertTrue(q.apply(Collections.emptyList()) == query);
        Assert.assertTrue(q.apply(Collections.emptySet()) == query);
        Assert.assertTrue(q.apply((Enumeration<String>) null) == query);
        Assert.assertTrue(q.apply(new Enumeration<String>() {
            @Override
            public boolean hasMoreElements() {
                return false;
            }

            @Override
            public String nextElement() {
                throw new NoSuchElementException();
            }
        }) == query);
        Assert.assertTrue(q.apply((Map<String, String>) null) == query);
        Assert.assertTrue(q.apply(Collections.emptyMap()) == query);
        Assert.assertTrue(q.apply("test") == query);
        Assert.assertTrue(q.apply("test1", "test2") == query);
        Assert.assertFalse(q.hasParameter());
        Assert.assertTrue((Object) q.getParameters() == Collections.emptyList());
        Assert.assertEquals(q.getQueryParts().toArray(new String[0][]),
                new String[][] { new String[] { query, null } });
    }

    @Test(groups = { "unit" })
    public void testQueryWithParameters() {
        String query = "select 2>1?3:2, name, value, value::Decimal64(3) from my_table where value != ':ccc' and num in (:no ) and value = :v(String)";
        ClickHouseParameterizedQuery q = ClickHouseParameterizedQuery.of(query);
        Assert.assertTrue(q.getOriginalQuery() == query);
        Assert.assertTrue(q.hasParameter());
        Assert.assertEquals(q.getQueryParts().toArray(new String[0][]), new String[][] { new String[] {
                "select 2>1?3:2, name, value, value::Decimal64(3) from my_table where value != ':ccc' and num in (",
                "no" }, new String[] { " ) and value = ", "v" } });
        Assert.assertEquals(q.apply((String) null),
                "select 2>1?3:2, name, value, value::Decimal64(3) from my_table where value != ':ccc' and num in (null ) and value = NULL");
        Assert.assertEquals(q.apply((String) null, (String) null, (String) null),
                "select 2>1?3:2, name, value, value::Decimal64(3) from my_table where value != ':ccc' and num in (null ) and value = null");
        Assert.assertEquals(q.apply("1", "2", "3"),
                "select 2>1?3:2, name, value, value::Decimal64(3) from my_table where value != ':ccc' and num in (1 ) and value = 2");
        Assert.assertEquals(q.apply("''", "'\\''", "233"),
                "select 2>1?3:2, name, value, value::Decimal64(3) from my_table where value != ':ccc' and num in ('' ) and value = '\\''");
    }

    @Test(groups = { "unit" })
    public void testApplyNamedParameters() {
        String sql = "select 2>1?3:2, name, value, value::Decimal64(3) from my_table where value != ':ccc' and num in (:no ) and value = :v(String)";
        Map<String, String> params = new HashMap<>();
        params.put("no", "1,2,3");
        params.put("v", "'s t r'");

        Assert.assertEquals(ClickHouseParameterizedQuery.apply(sql, params),
                "select 2>1?3:2, name, value, value::Decimal64(3) from my_table where value != ':ccc' and num in (1,2,3 ) and value = 's t r'");
    }

    @Test(groups = { "unit" })
    public void testApplyTypedParameters() {
        LocalDateTime ts = LocalDateTime.ofEpochSecond(10000, 123456789, ZoneOffset.UTC);
        String sql = "select :ts1 ts1, :ts2(DateTime32) ts2, :ts2 ts3";
        ClickHouseParameterizedQuery pq = ClickHouseParameterizedQuery.of(sql);
        ClickHouseValue[] templates = pq.getParameterTemplates();
        Assert.assertEquals(templates.length, pq.getParameters().size());
        Assert.assertNull(templates[0]);
        Assert.assertTrue(templates[1] instanceof ClickHouseDateTimeValue);
        Assert.assertEquals(((ClickHouseDateTimeValue) templates[1]).getScale(), 0);
        Assert.assertEquals(pq.apply(ts, ts),
                "select '1970-01-01 02:46:40.123456789' ts1, '1970-01-01 02:46:40' ts2, '1970-01-01 02:46:40' ts3");
    }
}
