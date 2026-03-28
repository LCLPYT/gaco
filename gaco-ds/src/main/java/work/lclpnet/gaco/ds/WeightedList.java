package work.lclpnet.gaco.ds;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.floats.FloatLists;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A list implementation, where every item has a weight, that is used to randomly sample and element from the list.
 * Higher weights correspond to higher frequency.
 * @param <E> The element type to store.
 */
public class WeightedList<E> extends AbstractList<E> {

    private final List<E> elements;
    private final FloatList cumulativeWeights;  // is always ordered ascending
    private float totalWeight;

    public WeightedList() {
        this(10);
    }

    public WeightedList(int initialCapacity) {
        this(new ArrayList<>(initialCapacity), new FloatArrayList(initialCapacity), 0);
    }

    protected WeightedList(List<E> elements, FloatList cumulativeWeights, float totalWeight) {
        this.elements = elements;
        this.cumulativeWeights = cumulativeWeights;
        this.totalWeight = totalWeight;
    }

    @Override
    public synchronized int size() {
        return elements.size();
    }

    @Override
    public synchronized E get(int index) {
        return elements.get(index);
    }

    public void add(E item, float weight) {
        if (weight < 0f) throw new IllegalArgumentException("Weight must be positive");
        else if (!Float.isFinite(weight)) throw new IllegalArgumentException("Weight must be finite");

        synchronized (this) {
            elements.add(item);
            totalWeight += weight;
            cumulativeWeights.add(totalWeight);
        }
    }

    @Override
    public synchronized E remove(int index) {
        Objects.checkIndex(index, elements.size());

        E element = elements.remove(index);
        float offset = cumulativeWeights.removeFloat(index);

        float cumulativeWeight = index > 0 ? cumulativeWeights.getFloat(index - 1) : 0f;

        // update subsequent cumulative weights
        for (int i = index, size = cumulativeWeights.size(); i < size; i++) {
            float oldCumulativeWeight = cumulativeWeights.getFloat(i);
            float weight = oldCumulativeWeight - offset;
            offset = oldCumulativeWeight;

            cumulativeWeight += weight;

            cumulativeWeights.set(i, cumulativeWeight);
        }

        return element;
    }

    @Override
    public synchronized void clear() {
        elements.clear();
        cumulativeWeights.clear();
        totalWeight = 0;
    }

    /**
     * Get a random element, respecting the elements corresponding weights.
     * The runtime of this method is dependent of:
     * 1. The random index lookup (implemented as binary search with <code>O(log(n))</code>).
     * 2. The index access of the backing list (which is ArrayList by default, with <code>O(1)</code>).
     * Therefore, the runtime is <code>O(log(n))</code> by default.
     * @param random The RNG.
     * @return A random element, or null, if there are no elements.
     */
    @Nullable
    public E getRandomElement(Random random) {
        int index = getRandomIndex(random);

        if (index == -1) return null;

        return get(index);
    }

    public synchronized int getRandomIndex(Random random) {
        final float target = random.nextFloat() * totalWeight;

        int index = binarySearch(cumulativeWeights, target);

        if (index < 0) {
            index = -index - 1;
        }

        if (index >= elements.size()) {
            return -1;
        }

        return index;
    }

    @VisibleForTesting
    static int binarySearch(FloatList list, float target) {
        int lo = 0;
        int hi = list.size() - 1;

        while (lo <= hi) {
            int mid = (hi + lo) >>> 1;
            float val = list.getFloat(mid);
            int cmp = Float.compare(val, target);

            if (cmp < 0) {
                if (mid < hi && target <= list.getFloat(mid + 1)) {
                    return mid + 1;
                }

                lo = mid + 1;
            } else if (cmp > 0) {
                float start = mid > 0 ? list.getFloat(mid - 1) : 0.0f;

                if (target > start) {
                    return mid;
                }

                hi = mid - 1;
            } else {
                return mid;
            }
        }

        return -1;
    }

    /**
     * Applies a mapping function to each element.
     * The order of the mapped elements and their weights stay the same.
     * @param mapper The mapping function to apply to each element.
     * @return A copy of this list, with the mapped elements.
     * @param <U> The mapped element result type.
     */
    public synchronized <U> WeightedList<U> map(Function<E, U> mapper) {
        List<U> mappedElements = this.elements.stream()
                .map(mapper)
                .collect(Collectors.toCollection(ArrayList::new));

        return new WeightedList<>(mappedElements, new FloatArrayList(cumulativeWeights), totalWeight);
    }

    /**
     * Filters elements from this list and returns a new weighted list of the remaining elements.
     * @param predicate The predicate.
     * @return A copy of this list, with only the filtered elements.
     */
    public synchronized WeightedList<E> filter(Predicate<E> predicate) {
        int[] indices = IntStream.range(0, this.elements.size())
                .filter(i -> predicate.test(get(i)))
                .toArray();

        List<E> filtered = new ArrayList<>(indices.length);
        FloatList filteredCumulativeWeights = new FloatArrayList(indices.length);

        float totalWeight = 0;

        for (int i : indices) {
            filtered.add(elements.get(i));
            float offset = i > 0 ? cumulativeWeights.getFloat(i - 1) : 0f;
            float weight = cumulativeWeights.getFloat(i) - offset;
            totalWeight += weight;
            filteredCumulativeWeights.add(totalWeight);
        }

        return new WeightedList<>(filtered, filteredCumulativeWeights, totalWeight);
    }

    /**
     * Creates a copy of this list.
     * @return A copy of this weighted list.
     */
    public synchronized WeightedList<E> copy() {
        return new WeightedList<>(new ArrayList<>(elements), new FloatArrayList(cumulativeWeights), totalWeight);
    }

    /**
     * Returns an immutable view of this list.
     * Changes to the source are still reflected in the immutable view.
     * If this is undesirable, use {@link #copy()} instead.
     * @return An immutable (read only) view of this weighted list.
     */
    public synchronized WeightedList<E> immutableView() {
        if (this.getClass() == Immutable.class) {
            return this;
        }

        return new Immutable<>(elements, cumulativeWeights, totalWeight);
    }

    /**
     * Returns a normalized copy of this list, so that the totalWeight of all the weights will be one.
     * @return A normalized copy of this list.
     */
    public synchronized WeightedList<E> normalized() {
        final float norm = 1f / totalWeight;
        final int size = size();

        FloatList normalizedCumulativeWeights = new FloatArrayList(size);

        for (int i = 0; i < size; i++) {
            normalizedCumulativeWeights.add(i, cumulativeWeights.getFloat(i) * norm);
        }

        return new WeightedList<>(new ArrayList<>(elements), normalizedCumulativeWeights, 1f);
    }

    /**
     * Gets the total weight of an element.
     * Since the element may be in the list multiple times, the method needs to walk the whole list -> O(n).
     * @param element The element to check.
     * @return The total probability of the
     */
    public synchronized float getTotalWeight(E element) {
        float totalWeight = 0;

        for (int i = 0, size = size(); i < size; i++) {
            if (!Objects.equals(elements.get(i), element)) continue;

            float lastWeight = i >= 1 ? cumulativeWeights.getFloat(i - 1) : 0f;
            float cumulativeWeight = cumulativeWeights.getFloat(i);

            float weight = cumulativeWeight - lastWeight;

            totalWeight += weight;
        }

        return totalWeight;
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends E> c) {
        if (!(c instanceof WeightedList<? extends E> weighted)) {
            throw new IllegalArgumentException("Input list must also be a weighted list");
        }

        if (weighted.isEmpty()) return false;

        int size = weighted.size();

        for (int i = 0; i < size; i++) {
            float prevWeight = i >= 1 ? weighted.cumulativeWeights.getFloat(i - 1) : 0f;
            float cumulativeWeight = weighted.cumulativeWeights.getFloat(i);
            float weight = cumulativeWeight - prevWeight;

            add(weighted.elements.get(i), weight);
        }

        return super.addAll(c);
    }

    public static <E> WeightedList<E> of(Collection<? extends E> elements, Function<E, Number> probabilityMapper) {
        var list = new WeightedList<E>(elements.size());

        for (E element : elements) {
            float probability = probabilityMapper.apply(element).floatValue();
            list.add(element, probability);
        }

        return list;
    }

    /**
     * Returns an empty immutable {@link WeightedList}.
     * @return An empty {@link WeightedList}.
     * @param <E> The {@link WeightedList}'s element type.
     */
    @SuppressWarnings("unchecked")
    public static <E> WeightedList<E> empty() {
        return (WeightedList<E>) Immutable.EMPTY;
    }

    private static class Immutable<E> extends WeightedList<E> {
        private static final WeightedList<?> EMPTY = new Immutable<>(List.of(), FloatLists.emptyList(), 0);

        private Immutable(List<E> elements, FloatList cumulativeWeights, float totalWeight) {
            super(elements, cumulativeWeights, totalWeight);
        }

        @Override
        public void add(E item, float weight) {
            throw new UnsupportedOperationException();
        }

        @Override
        public E remove(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    }
}
