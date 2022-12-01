package optimizer.register;

import java.util.*;

class RegPool {
    static List<Integer> fullPool = new ArrayList<>(Arrays.asList(
            8, 9, 10, 11, 12, 13, 14, 15, // t0-t7
            16, 17, 18, 19, 20, 21, 22, 23, // s0-s7
            26, 27, 3, 30 // k0, k1, v1, fp
    ));

    Deque<Integer> pool = new ArrayDeque<>(fullPool);

    // 若返回 null，表示池已空
    Integer fetch() {
        return pool.pollFirst();
    }

    void free(int x) {
        assert x > 0;
        pool.addFirst(x);
    }
}