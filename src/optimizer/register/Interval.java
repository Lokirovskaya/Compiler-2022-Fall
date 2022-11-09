package optimizer.register;

import intercode.Operand.VirtualReg;
import mips.MipsCoder;
import util.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class Interval {
    VirtualReg vreg;
    int realReg = -1;
    List<Pair<Integer, Integer>> rangeList = new ArrayList<>();
    List<Integer> usePointList = new ArrayList<>();

    void addRange(int start, int end) {
        rangeList.add(new Pair<>(start, end));
        mergeRanges();
    }

    void addUsePoint(int usePoint) {
        usePointList.add(usePoint);
    }

    int start() {
        return rangeList.get(0).first;
    }

    int end() {
        return rangeList.get(rangeList.size() - 1).second;
    }

    // 将 rangeList 排序，并合并其中的区间
    private void mergeRanges() {
        rangeList.sort(Comparator.comparingInt(r -> r.first));
        int nowIdx = 0, nextIdx = 1;
        while (nextIdx < rangeList.size()) {
            Pair<Integer, Integer> now = rangeList.get(nowIdx), next = rangeList.get(nextIdx);
            if (next.first <= now.second + 1) {
                now.second = Math.max(now.second, next.second);
                next.first = -1; // to be deleted
                nextIdx++;
                continue;
            }
            nowIdx = nextIdx;
            nextIdx++;
        }
        rangeList.removeIf(r -> r.first == -1);
    }

    @Override
    public String toString() {
        return String.format("{vreg: %s, reg: %s, ranges: %s, use: %s}",
                vreg, realReg, rangeList, usePointList);
    }
}
