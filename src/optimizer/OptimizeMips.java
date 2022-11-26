package optimizer;

import mips.Mips;
import optimizer.mipsoptimizer.*;

import java.util.List;

public class OptimizeMips {
    public static void optimize(List<Mips> mipsList) {
        PeepHole.run(mipsList);
        ConvertMod.run(mipsList);
        WeakenDiv.run(mipsList);
        WeakenMult.run(mipsList);
        PeepHole.run(mipsList);
    }
}
