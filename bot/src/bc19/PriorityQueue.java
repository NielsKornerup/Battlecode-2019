package bc19;

import java.util.ArrayList;
import java.util.List;

class Node implements Comparable<Node> {
    int dist;
    Point p;

    public Node(int dist, Point p) {
        this.dist = dist;
        this.p = p;
    }

    public int compareTo(Node o) {
        return dist - o.dist;
    }
}

public class PriorityQueue {
    private List<Node> heap;

    public PriorityQueue() {
        this.heap = new ArrayList<>();
    }

    public int size() {
        return this.heap.size();
    }

    public boolean isEmpty() {
        return this.size() == 0;
    }

    public void enqueue(Node value) {
        int sz = size();
        this.heap.add(value);
        this.up(this.size() - 1);
    }

    public Node dequeue() {
        int sz = size();
        if (sz == 0) {
            throw new ArrayIndexOutOfBoundsException();
        }
        Node last = this.heap.get(this.heap.size() - 1);
        this.heap.remove(this.heap.size() - 1);
        if (this.heap.size() > 0) {
            Node ret = this.heap.get(0);
            this.heap.set(0, last);
            this.down(0);
            return ret;
        }
        return last;
    }

    public Node peek() {
        if (size() == 0) {
            return null;
        } else {
            return heap.get(0);
        }
    }

    public boolean delete(Point p) {
        if (this.heap.size() == 0) {
            return false;
        }
        for (int i = 0; i < this.heap.size(); i++) {
            if (p.x == this.heap.get(i).p.x && p.y == this.heap.get(i).p.y) {
                if (this.heap.size() - 1 == i) {
                    this.heap.remove(i);
                } else {
                    this.heap.set(i, this.heap.get(this.heap.size() - 1));
                    down(i);
                    up(i);
                }
                return true;
            }
        }
        return false;
    }

    private boolean lt(Node t1, Node t2) {
        return t1.compareTo(t2) < 0;
    }

    private boolean gt(Node t1, Node t2) {
        return t1.compareTo(t2) > 0;
    }

    private void up(int index) {
        if (index == 0) {
            return;
        }
        Node elt = this.heap.get(index);
        int parIndex = (index - 1) / 2;
        Node par = this.heap.get(parIndex);
        if (this.lt(elt, par)) {
            this.heap.set(index, par);
            this.heap.set(parIndex, elt);
            this.up(parIndex);
        }
    }

    private void down(int parIndex) {
        Node par = this.heap.get(parIndex);
        int c1Index = 2 * parIndex + 1;
        int c2Index = 2 * parIndex + 2;
        if (c1Index >= this.heap.size()) {
            return;
        }
        Node c1 = this.heap.get(c1Index);
        if (c2Index >= this.heap.size()) {
            if (this.gt(par, c1)) {
                this.heap.set(parIndex, c1);
                this.heap.set(c1Index, par);
            }
            return;
        }
        Node c2 = this.heap.get(c2Index);
        if (this.gt(par, c1) || this.gt(par, c2)) {
            if (this.lt(c1, c2)) {
                this.heap.set(parIndex, c1);
                this.heap.set(c1Index, par);
                this.down(c1Index);
            } else {
                this.heap.set(parIndex, c2);
                this.heap.set(c2Index, par);
                this.down(c2Index);
            }
        }
    }
}
