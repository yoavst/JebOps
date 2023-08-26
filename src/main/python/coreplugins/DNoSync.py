#?type=dexdec-ir
from com.pnfsoftware.jeb.core.units.code.android.ir import AbstractDOptimizer

class DNoSync(AbstractDOptimizer):  
    def perform(self):
        cnt = 0
        for insn in self.cfg.instructions():   
            if insn.isMonitorEnter() or insn.isMonitorExit():
                insn.transformToNop()
                cnt += 1
        if cnt:
            self.cfg.invalidateDataFlowAnalysis()
        return cnt
