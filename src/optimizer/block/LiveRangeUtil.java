package optimizer.block;

import intercode.Operand.VirtualReg;
import util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class LiveRangeUtil {
    static final Map<VirtualReg, List<LiveRange>> vregLiveRangesMap = new HashMap<>();

    static void addLiveRange(VirtualReg vreg, int start, int end) {
        if (!vregLiveRangesMap.containsKey(vreg)) {
            vregLiveRangesMap.put(vreg, new ArrayList<>());
        }
        vregLiveRangesMap.get(vreg).add(new LiveRange(vreg, start, end));
    }

    static void mergeAndSortLiveRanges() {
        for (List<LiveRange> liveRangeList: vregLiveRangesMap.values()) {
            liveRangeList.sort(Comparator.comparingInt(x -> x.start));
            int nowIdx = 0, nextIdx = 1;
            while (nextIdx < liveRangeList.size()) {
                LiveRange now = liveRangeList.get(nowIdx), next = liveRangeList.get(nextIdx);
                if (next.start <= now.end + 1) {
                    now.end = Math.max(now.end, next.end);
                    next.start = -1; // to be deleted
                    nextIdx++;
                    continue;
                }
                nowIdx = nextIdx;
                nextIdx++;
            }
            for (int i = 0; i < liveRangeList.size(); i++) {
                if (liveRangeList.get(i).start == -1) {
                    liveRangeList.remove(i);
                    i--;
                }
            }
        }
    }

    public static void printLiveRanges() {
        for (VirtualReg vreg : vregLiveRangesMap.keySet().stream()
                .sorted(Comparator.comparingInt(x -> x.regID))
                .collect(Collectors.toList())) {
            System.out.printf("%s: ", vreg);
            for (LiveRange liveRange : vregLiveRangesMap.get(vreg)) {
                System.out.printf("(%d, %d) ", liveRange.start, liveRange.end);
            }
            System.out.println();
        }
    }
}
