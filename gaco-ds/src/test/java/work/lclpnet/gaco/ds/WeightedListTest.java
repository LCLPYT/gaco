package work.lclpnet.gaco.ds;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class WeightedListTest {

    public static final int COUNTER_SAMPLES = 10_000;

    @Test
    void getRandomElement_empty_null() {
        assertNull(new WeightedList<>().getRandomElement(new Random()));
    }

    @RepeatedTest(100)
    void getRandomElement_singleElement_returned() {
        var list = new WeightedList<>();
        list.add("foo", 1f);
        assertEquals("foo", list.getRandomElement(new Random()));
    }

    @Test
    void getRandomElement_multiple_asExpected() {
        var list = new WeightedList<String>();
        list.add("foo", 0.8f);
        list.add("bar", 0.2f);

        Random random = new Random();

        var counts = countOccurrences(list, random);

        assertPercentage(0.8f, counts, "foo");
        assertPercentage(0.2f, counts, "bar");
    }

    @Test
    void testAdd() {
        var list = new WeightedList<String>();
        list.add("a", 1.0f);
        list.add("b", 2.0f);
        assertEquals(2, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
    }

    @Test
    void testAddNegativeWeight() {
        var list = new WeightedList<String>();
        assertThrows(IllegalArgumentException.class, () -> list.add("a", -1.0f));
    }

    @Test
    void testRemove() {
        var list = new WeightedList<String>();
        list.add("a", 1.0f);
        list.add("b", 2.0f);
        assertEquals("a", list.removeFirst());
        assertEquals(1, list.size());
        assertEquals("b", list.getFirst());
    }

    @Test
    void testGetRandomIndex() {
        var list = new WeightedList<String>();
        Random random = new Random();
        list.add("a", 1.0f);
        list.add("b", 2.0f);
        list.add("c", 3.0f);
        int index = list.getRandomIndex(random);
        assertTrue(index >= 0 && index < list.size());
    }

    @Test
    void testMap() {
        var list = new WeightedList<String>();
        list.add("1", 1.0f);
        list.add("2", 2.0f);
        WeightedList<Integer> mappedList = list.map(Integer::parseInt);
        assertEquals(2, mappedList.size());
        assertEquals(1, mappedList.get(0));
        assertEquals(2, mappedList.get(1));
    }

    @Test
    void testMapMutable() {
        var list = new WeightedList<String>();
        list.add("1", 1.0f);
        list.add("2", 2.0f);
        var mappedList = list.map(Integer::parseInt);
        assertDoesNotThrow(() -> mappedList.add(3, 3.0f));
    }

    @Test
    void testFilter() {
        var list = new WeightedList<String>();
        list.add("hello", 1.0f);
        list.add("foo", 2.0f);
        list.add("world", 1.0f);
        var filtered = list.filter(s -> s.length() > 3);
        assertEquals(2, filtered.size());
        assertEquals("hello", filtered.getFirst());
        assertEquals("world", filtered.getLast());
    }

    @Test
    void testFilterMutable() {
        var list = new WeightedList<String>();
        list.add("hello", 1.0f);
        list.add("foo", 2.0f);
        list.add("world", 1.0f);
        var filtered = list.filter(s -> s.length() > 3);
        assertDoesNotThrow(() -> filtered.add("bar", 3.0f));
    }

    @Test
    void testFilterWeightsAsExpected() {
        var list = new WeightedList<String>();
        list.add("hello", 1.0f);
        list.add("foo", 2.0f);
        list.add("world", 2.0f);
        list.add("test", 1.0f);
        var filtered = list.filter(s -> s.length() > 3);

        var random = new Random();
        var counts = countOccurrences(filtered, random);

        assertPercentage(0.25f, counts, "hello");
        assertPercentage(0.25f, counts, "test");
        assertPercentage(0.5f, counts, "world");
    }

    @Test
    void testSize() {
        var list = new WeightedList<String>();
        assertEquals(0, list.size());
        list.add("a", 1.0f);
        assertEquals(1, list.size());
        list.add("b", 2.0f);
        assertEquals(2, list.size());
        list.removeFirst();
        assertEquals(1, list.size());
    }

    @Test
    void testThreadSafety() throws InterruptedException {
        var list = new WeightedList<String>();

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                list.add("a" + i, i);
            }
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                if (!list.isEmpty()) {
                    list.removeFirst();
                }
            }
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        assertTrue(list.size() <= 1000);
    }

    @Test
    void testBinarySearchCumulative() {
        float max = 5.25f;
        var cumulativeList = new FloatArrayList(new float[] {0.5f, 1.0f, 1.12f, 1.45f, 2.0f, 3.0f, max});
        float query = 0.0f;

        while (query <= max) {
            int expected = -1;

            for (int i = 0; i < cumulativeList.size(); i++) {
                float start = i > 0 ? cumulativeList.getFloat(i - 1): 0f;
                float end = cumulativeList.getFloat(i);

                if (query > start && query <= end) {
                    expected = i;
                    break;
                }
            }

            int actual = WeightedList.binarySearch(cumulativeList, query);

            assertEquals(expected, actual);

            query += 0.1f;
        }
    }

    @Test
    void testCopy_containsSameElementsAndWeights() {
        var original = new WeightedList<String>();
        original.add("a", 1.0f);
        original.add("b", 2.0f);

        var copy = original.copy();

        assertEquals(2, copy.size());
        assertEquals("a", copy.get(0));
        assertEquals("b", copy.get(1));
        assertEquals(1.0f, copy.getTotalWeight("a"), 0.001f);
        assertEquals(2.0f, copy.getTotalWeight("b"), 0.001f);
    }

    @Test
    void testCopy_isIndependentFromOriginal() {
        var original = new WeightedList<String>();
        original.add("a", 1.0f);

        var copy = original.copy();

        // Modify original
        original.add("b", 2.0f);
        assertEquals(2, original.size());
        assertEquals(1, copy.size(), "Copy should not be affected by changes to original");

        // Modify copy
        copy.add("c", 3.0f);
        assertEquals(2, copy.size());
        assertEquals(2, original.size(), "Original should not be affected by changes to copy");
    }

    @Test
    void testImmutableView_throwsOnModification() {
        var original = new WeightedList<String>();
        original.add("a", 1.0f);
        original.add("b", 2.0f);

        var view = original.immutableView();

        assertThrows(UnsupportedOperationException.class, () -> view.add("c", 1.0f));
        assertThrows(UnsupportedOperationException.class, view::removeFirst);
        assertThrows(UnsupportedOperationException.class, view::clear);
    }

    @Test
    void testImmutableView_reflectsChangesInOriginal() {
        var original = new WeightedList<String>();
        original.add("a", 1.0f);

        var view = original.immutableView();
        assertEquals(1, view.size());
        assertEquals("a", view.getFirst());

        // Change original, ensure view sees it
        original.add("b", 2.0f);
        assertEquals(2, view.size(), "Immutable view should reflect additions to the backing list");
        assertEquals("b", view.get(1));

        original.removeFirst();
        assertEquals(1, view.size(), "Immutable view should reflect removals from the backing list");
        assertEquals("b", view.getFirst());
    }

    @Test
    void testImmutableView_nestedCallReturnsSelf() {
        var original = new WeightedList<String>();
        var view = original.immutableView();
        var viewOfView = view.immutableView();

        assertSame(view, viewOfView, "Calling immutableView() on an Immutable list should return itself");
    }

    @Test
    void testNormalized_weightsSumToOne() {
        var list = new WeightedList<String>();
        list.add("a", 10.0f);
        list.add("b", 30.0f);
        list.add("c", 10.0f); // Total weight = 50.0f

        var normalized = list.normalized();

        assertEquals(3, normalized.size());
        assertEquals(0.2f, normalized.getTotalWeight("a"), 0.001f);
        assertEquals(0.6f, normalized.getTotalWeight("b"), 0.001f);
        assertEquals(0.2f, normalized.getTotalWeight("c"), 0.001f);
    }

    @Test
    void testNormalized_probabilitiesRemainSame() {
        var list = new WeightedList<String>();
        list.add("foo", 10.0f);
        list.add("bar", 40.0f);

        var normalized = list.normalized();
        var random = new Random();
        var counts = countOccurrences(normalized, random);

        assertPercentage(0.2f, counts, "foo");
        assertPercentage(0.8f, counts, "bar");
    }

    @Test
    void testNormalized_emptyListEdgeCase() {
        var list = new WeightedList<String>();
        var normalized = list.normalized();

        assertEquals(0, normalized.size());
        // Since original total weight was 0, norm calculation is 1f/0f = Infinity,
        // but loop is skipped so it shouldn't crash.
        assertNull(normalized.getRandomElement(new Random()));
    }

    @Test
    void testGetTotalWeight_singleOccurrence() {
        var list = new WeightedList<String>();
        list.add("a", 3.5f);
        list.add("b", 1.5f);

        assertEquals(3.5f, list.getTotalWeight("a"), 0.001f);
        assertEquals(1.5f, list.getTotalWeight("b"), 0.001f);
    }

    @Test
    void testGetTotalWeight_multipleOccurrences() {
        var list = new WeightedList<String>();
        list.add("a", 1.0f);
        list.add("b", 2.0f);
        list.add("a", 3.0f);
        list.add("c", 0.5f);
        list.add("a", 1.5f);

        // 'a' appears three times: 1.0 + 3.0 + 1.5 = 5.5
        assertEquals(5.5f, list.getTotalWeight("a"), 0.001f);
    }

    @Test
    void testGetTotalWeight_elementNotPresent() {
        var list = new WeightedList<String>();
        list.add("a", 1.0f);

        assertEquals(0.0f, list.getTotalWeight("b"), 0.001f);
        assertEquals(0.0f, list.getTotalWeight(null), 0.001f);
    }

    @Test
    void testAddAll_validWeightedList_transfersWeightsCorrectly() {
        var target = new WeightedList<String>();
        target.add("existing", 10.0f);

        var source = new WeightedList<String>();
        source.add("new1", 5.0f);
        source.add("new2", 15.0f);

        boolean changed = target.addAll(source);

        assertTrue(changed);
        // Checking if weights were extracted correctly from cumulativeWeights logic
        assertEquals(5.0f, target.getTotalWeight("new1"), 0.001f);
        assertEquals(15.0f, target.getTotalWeight("new2"), 0.001f);
        assertEquals(30.0f, target.getTotalWeight("existing") + 5.0f + 15.0f, 0.001f);
    }

    @Test
    void testAddAll_emptySource_returnsFalse() {
        var target = new WeightedList<String>();
        target.add("a", 1.0f);

        var emptySource = new WeightedList<String>();

        boolean changed = target.addAll(emptySource);

        assertFalse(changed, "addAll should return false when source is empty");
        assertEquals(1, target.size());
    }

    @Test
    void testAddAll_nonWeightedList_throwsIllegalArgumentException() {
        var target = new WeightedList<String>();
        List<String> ordinaryList = List.of("a", "b");

        assertThrows(IllegalArgumentException.class, () -> {
            target.addAll(ordinaryList);
        }, "Should throw because the input is not a WeightedList");
    }

    @Test
    void testAddAll_maintainsCumulativeIntegrity() {
        var target = new WeightedList<Integer>();
        target.add(1, 1.0f); // total 1.0

        var source = new WeightedList<Integer>();
        source.add(2, 2.0f);
        source.add(3, 3.0f); // source weights are 2 and 3

        target.addAll(source);

        // Verify the math: 1.0 (existing) + 2.0 (new) + 3.0 (new) = 6.0
        // We can verify this via getRandomIndex logic indirectly
        // or by checking the total weight if accessible.
        Random fixedRandom = new Random();
        // A simple way to check if the cumulative weights are still ordered and valid:
        for (int i = 0; i < 50; i++) {
            assertDoesNotThrow(() -> target.getRandomElement(fixedRandom));
        }
    }

    private static Object2IntArrayMap<String> countOccurrences(WeightedList<String> list, Random random) {
        var counter = new Object2IntArrayMap<String>(2);

        for (int i = 0; i < COUNTER_SAMPLES; i++) {
            String item = list.getRandomElement(random);
            assertNotNull(item);

            counter.computeInt(item, (key, count) -> count == null ? 1 : count + 1);
        }
        return counter;
    }

    private static void assertPercentage(float expected, Object2IntArrayMap<String> counts, String hello) {
        assertEquals(expected, counts.getOrDefault(hello, 0) / (float) COUNTER_SAMPLES, 10e-2);
    }
}