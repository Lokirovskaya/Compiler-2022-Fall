package optimizer;

import intercode.InterCode;

public class Optimizer {
    public static void Optimize(InterCode inter) {
        ReduceGoto.run(inter);
//        ReduceGoto.run(inter);
//        DeleteUnused.run(inter);
    }
}
