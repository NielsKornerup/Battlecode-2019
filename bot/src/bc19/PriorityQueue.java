package bc19;

import java.util.ArrayList;
import java.util.List;

public class PriorityQueue<T> {
    private List<T> heap;
    private Comparator<T> comparator;

    public PriorityQueue(Comparator<T> c) {
        heap = new ArrayList<>();
        comparator = c;
    }

    public int size() {
        return heap.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public void enqueue(T value) {
        int sz = size();
        heap.add(value);
        up(size() - 1);
    }

    public T dequeue() {
        int sz = size();
        if (sz == 0) {
            throw new ArrayIndexOutOfBoundsException();
        }
        T last = heap.get(heap.size() - 1);
        heap.remove(heap.size() - 1);
        if (heap.size() > 0) {
           T ret = heap.get(0);
           heap.set(0, last);
           down(0);
           return ret;
        }
        return last;
    }

    private boolean lt(T t1, T t2) {
        return comparator.compare(t1, t2) < 0;
    }

    private boolean gt(T t1, T t2) {
        return comparator.compare(t1, t2) > 0;
    }

    private void up(int index) {
        if (index == 0) {
            return;
        }
        T elt = heap.get(index);
        int parIndex = (index - 1) / 2;
        T par = heap.get(parIndex);
        if (lt(elt, par)) {
            heap.set(index, par);
            heap.set(parIndex, elt);
            up(parIndex);
        }
    }

    private void down(int parIndex) {
        T par = heap.get(parIndex);
        int c1Index = 2 * parIndex + 1;
        int c2Index = 2 * parIndex + 2;
        if (c1Index >= heap.size()) {
            return;
        }
        T c1 = heap.get(c1Index);
        if (c2Index >= heap.size() && gt(par, c1)) {
            heap.set(parIndex, c1);
            heap.set(c1Index, par);
            return;
        }
        T c2 = heap.get(c2Index);
        if (gt(par, c1) || gt(par, c2)) {
            if (lt(c1, c2)) {
                heap.set(parIndex, c1);
                heap.set(c1Index, par);
                down(c1Index);
            } else {
                heap.set(parIndex, c2);
                heap.set(c2Index, par);
                down(c2Index);
            }
        }
    }
}
