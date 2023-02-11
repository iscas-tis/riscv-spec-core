package rvspeccore.core.tool

import chisel3._
import chisel3.util._

import rvspeccore.core.BaseCore
import rvspeccore.core.spec.instset.csr._
import java.awt.print.Book
class PTWLevel()(implicit XLEN: Int) extends Bundle {
  val valid    = Bool()
  val success  = Bool()
  val addr     = UInt(XLEN.W)
  val pte      = UInt(XLEN.W) //FIXME: Just for SV39
}

trait LoadStore extends BaseCore with MMU{
//   def ModeU     = 0x0.U // 00 User/Application
//   def ModeS     = 0x1.U // 01 Supervisor
//   def ModeR     = 0x2.U // 10 Reserved
//   def ModeM     = 0x3.U // 11 Machine
  def memRead(addr: UInt, memWidth: UInt): UInt = {
    val mstatusStruct = now.csr.mstatus.asTypeOf(new MstatusStruct)
    val pv = Mux(mstatusStruct.mprv.asBool, mstatusStruct.mpp, priviledgeMode)
    val vmEnable = now.csr.satp.asTypeOf(new SatpStruct).mode === 8.U && (pv < 0x3.U)
    printf("[Debug]Read addr:%x, priviledgeMode:%x %x %x %x vm:%x\n", addr, pv, mstatusStruct.mprv.asBool, mstatusStruct.mpp, priviledgeMode, vmEnable)
    mem.read.valid    := true.B
    when(vmEnable){
        mem.read.addr     := AddrTransRead(addr)
    }.otherwise{
        mem.read.addr     := addr
    }
    mem.read.memWidth := memWidth
    mem.read.data
  }
  def memWrite(addr: UInt, memWidth: UInt, data: UInt): Unit = {
      // val pv = Mux(now.csr.mstatus)
    val mstatusStruct = now.csr.mstatus.asTypeOf(new MstatusStruct)
    val pv = Mux(mstatusStruct.mprv.asBool, mstatusStruct.mpp, priviledgeMode)
    val vmEnable = now.csr.satp.asTypeOf(new SatpStruct).mode === 8.U && (pv < 0x3.U)
    printf("[Debug]Write addr:%x, priviledgeMode:%x %x %x %x vm:%x\n", addr, pv, mstatusStruct.mprv.asBool, mstatusStruct.mpp, priviledgeMode, vmEnable)
    mem.write.valid    := true.B
    when(vmEnable){
        val (success, finaladdr) = PageTableWalk_new(addr,0)
        when(success){
            mem.write.addr := finaladdr
        }.otherwise{
            raiseException(MExceptionCode.storeOrAMOPageFault)
        }
    }.otherwise{
        mem.write.addr := addr
    }
    mem.write.memWidth := memWidth
    mem.write.data     := data
  }
}

trait MMU extends BaseCore with ExceptionSupport{
    // 地址转换 先搞一个临时的
    // 0000_0000_8000_1000
    // "hff".U
    // "h8000_0000_0000_0000"
    def PARead(addr: UInt, memWidth: UInt): UInt = {
        mem.read.valid    := true.B
        mem.read.addr     := addr
        mem.read.memWidth := memWidth
        mem.read.data
    }

    def PAReadMMU(addr: UInt, memWidth: UInt, no: Int): UInt = {
        mem.Anotherread(no).valid    := true.B
        mem.Anotherread(no).addr     := addr
        mem.Anotherread(no).memWidth := memWidth
        mem.Anotherread(no).data
    }
    
    def PAWrite(addr: UInt, memWidth: UInt, data: UInt): Unit = {
        mem.write.valid    := true.B
        mem.write.addr     := addr    
        mem.write.memWidth := memWidth
        mem.write.data     := data
    }
    def PAWriteMMU(addr: UInt, memWidth: UInt, data: UInt): Unit = {
        mem.Anotherwrite(0).valid    := true.B
        mem.Anotherwrite(0).addr     := addr    
        mem.Anotherwrite(0).memWidth := memWidth
        mem.Anotherwrite(0).data     := data
    }
    def LegalAddr(vaddr:UInt): Bool = {
        val sum = now.csr.mstatus.asTypeOf((new MstatusStruct)).sum
        val pg_base = (now.csr.satp.asTypeOf((new SatpStruct)).ppn) << 12
        // val flag = (now.pc === "h8000_0238".U)
        // when(now.pc === "h8000_0238".U){
        //     // 巨页处理
        //     flag := false.B
        // }.otherwise{
        //     flag := true.B
        // }
        // sum.asBool & ~flag
        sum.asBool
    }
    def LegalAddrStep5(): Bool = {
        // FIXME: 需要进一步改这个函数 看手册哈
        val sum = now.csr.mstatus.asTypeOf((new MstatusStruct)).sum
        // val pg_base = (now.csr.satp.asTypeOf((new SatpStruct)).ppn) << 12
        // val flag = (now.pc === "h8000_0238".U)
        sum.asBool
    }
    def ValidPage(PTE:PTEFlag): Bool = {
        PTE.r || PTE.x
    }
    def LegalPage(PTE:PTEFlag, level:Int): Bool = {
        ~((!PTE.v || (!PTE.r && PTE.w)) || (level < 0).asBool)
    }
    def IsWriteDirty(PTE:SV39PTE, PA:UInt) = {
        val FlagPTE = PTE.flag.asTypeOf(new PTEFlag())
        val FlagPTEnew = 0.U(8.W).asTypeOf(new PTEFlag())
        when(~FlagPTE.a || ~FlagPTE.d){
            FlagPTEnew := FlagPTE
            FlagPTEnew.a := true.B
            FlagPTEnew.d := true.B
            val PTEnew = Cat(PTE.reserved.asUInt, PTE.ppn.asUInt, PTE.rsw.asUInt, FlagPTEnew.asUInt)
            printf("[Debug]Is Dirty!!! Need Write Addr: %x old: %x -> new:%x \n", PA, PTE.asUInt, PTEnew.asUInt)
            PAWriteMMU(PA, 64.U, PTEnew.asUInt)
        }
    }
    def LevelCalc(data: UInt):UInt = {
        MuxLookup(
            data,
            3.U, // faild
            Array(
                "b100".U   -> 2.U,
                "b010".U   -> 1.U,
                "b001".U   -> 0.U
            )
        )
    }
    def IsSuperPage() = {
    // def IsSuperPage(PTE:PTEFlag) = {
        val flag = (now.pc === "h8000_0238".U)
        false.B | flag
    }
    def AddrRSWLegal(addr:UInt) : Bool = {
        val flag = Wire(Bool())
        // FIXME: 需要修一下
        // when((addr << (64 - 39)) >> (63 - 39) === addr){
        //     flag := true.B
        // }.otherwise{
        //     flag := false.B
        // }
        flag := true.B
        flag
    }
    def PageTableWalk_new(addr:UInt, accsessType: Int): (Bool, UInt) = {
        printf("[Debug]PageTableWalk_new!!! Addr: %x, Type: %d\n", addr, accsessType.asUInt)
        // // FIXME:  临时的屎
        //     val SatpNow = now.csr.satp.asTypeOf((new SatpStruct))
        //     val sum = now.csr.mstatus.asTypeOf((new MstatusStruct)).sum
        //     val L2PA = Cat(Cat(0.U(8.W),Cat(SatpNow.ppn,addr(38,30))),0.U(3.W))
        //     val PTE = PARead(L2PA, 64.U).asTypeOf(new SV39PTE())
        //     val FlagPTE = PTE.flag.asTypeOf(new PTEFlag())
        //     IsWriteDirty(PTE, L2PA)
        // // FIXME: 临时拉的屎结束
        // Vaddr 前保留位校验 Begin
        // 失败 则Go bad
        val finalSuccess = Wire(Bool())
        val finaladdr = Wire(UInt(XLEN.W))
        when(AddrRSWLegal(addr)){
            printf("[Debug] Vaddr Legal\n")
            // 三级页表翻译 Begin
            val LevelVec = Wire(Vec(3, new PTWLevel()))
            val SatpNow = now.csr.satp.asTypeOf((new SatpStruct))
            LevelVec(2).valid := true.B     // 第一级肯定要打开
            LevelVec(2).addr  := Cat(Cat(0.U(8.W),Cat(SatpNow.ppn,addr(38,30))),0.U(3.W))
            for(level <- 0 to 2){
                // 循环生成三级页表的处理
                when(LevelVec(2 - level).valid){
                    printf("[Debug]LevelTest:%d %x\n", (2-level).U, LevelVec(2 - level).valid)
                    // 寻页且继续的那个函数 返回第二级的值
                    val PTE_PA = LevelVec(2 - level).addr
                    val PTE = PAReadMMU(LevelVec(2 - level).addr, 64.U, level).asTypeOf(new SV39PTE())
                    val PTEFlag = PTE.flag.asTypeOf(new PTEFlag())

                    when(~PTEFlag.v || (~PTEFlag.r && PTEFlag.w)){
                        // 失败了 后面也不继续找了 
                        if(2 - level - 1 >= 0){
                            LevelVec(2 - level - 1).valid   := false.B     // 下一级的有效就不用打开了
                            LevelVec(2 - level - 1).addr    := 0.U
                        }
                        LevelVec(2 - level).success := false.B  // 这一级的寻找失败了
                        LevelVec(2 - level).pte     := 0.U
                    }.otherwise{
                        when(PTEFlag.r || PTEFlag.x){
                            // 成功了
                            if(2 - level - 1 >= 0){
                                LevelVec(2 - level - 1).valid   := false.B     // 下一级的有效就不用打开了
                                LevelVec(2 - level - 1).addr    := 0.U
                            }
                            LevelVec(2 - level).success := true.B  // 这一级的寻找成功了
                            LevelVec(2 - level).pte     := PTE.asUInt
                        }.otherwise{
                            // 需要继续找
                            if(2 - level - 1 >= 0){
                                LevelVec(2 - level - 1).valid   := true.B     // 下一级的有效打开
                                // FIXME: 需要特别优化
                                if((2 - level - 1) == 1){
                                    LevelVec(2 - level - 1).addr    := Cat(Cat(0.U(8.W),Cat(PTE.ppn, addr(29,21))),0.U(3.W))
                                }
                                if((2 - level - 1) == 0){
                                    LevelVec(2 - level - 1).addr    := Cat(Cat(0.U(8.W),Cat(PTE.ppn, addr(20,12))),0.U(3.W))
                                }
                            }
                            LevelVec(2 - level).success := false.B  // 这一级的寻找失败了
                            LevelVec(2 - level).pte     := 0.U
                        }
                    }
                }.otherwise{
                    // // 这一级无效 需要把这一级的success 和 下一级的有效信号给干掉
                    if(2 - level - 1 >= 0){
                        LevelVec(2 - level - 1).valid   := false.B     // 下一级的有效关闭
                        LevelVec(2 - level - 1).addr    := 0.U
                    }
                    LevelVec(2 - level).success := false.B
                    LevelVec(2 - level).pte     := 0.U

                }
                // when(LevelVec(2 - level).success){
                //     printf("[Debug]LevelTest:%d level success %x\n", (2-level).U, LevelVec(2 - level).success)
                // }
            }
            printf("[Debug]LevelSuccess : %d %d %d\n", LevelVec(2).success, LevelVec(1).success, LevelVec(0).success)
            printf("[Debug]LevelPTE     : %x %x %x\n", LevelVec(2).pte, LevelVec(1).pte, LevelVec(0).pte)
            printf("[Debug]LevelSuccess2: %x\n", Cat(Cat(LevelVec(2).success, LevelVec(1).success), LevelVec(0).success))
            printf("[Debug]LevelSuccess3: %d\n", LevelCalc(Cat(Cat(LevelVec(2).success, LevelVec(1).success), LevelVec(0).success)))

            
            // 三级页表翻译 End
            // finalSuccess := LevelVec(2).success || LevelVec(1).success || LevelVec(0).success
            when(LevelVec(2).success || LevelVec(1).success || LevelVec(0).success){
                // 翻译暂时成功了
                when(LegalAddrStep5()){
                    // 检测超大页
                    when(IsSuperPage()){
                        // 是大页
                        finalSuccess := false.B
                        finaladdr := 0.U
                    }.otherwise{
                        // 成功了 但是还需要操作一下Dirty
                        // FIXME: 暂时向读热码妥协
                        // val PTE = PAReadMMU(LevelVec(2).addr, 64.U, 2).asTypeOf(new SV39PTE())
                        val successLevel = LevelCalc(Cat(Cat(LevelVec(2).success, LevelVec(1).success), LevelVec(0).success))
                        when(successLevel === 3.U){
                            // fail, not success
                            finalSuccess := false.B
                            finaladdr := 0.U
                        }.otherwise{
                            printf("[Debug]PTE.d test: Addr:%x PTE:%x\n", LevelVec(successLevel).addr, LevelVec(successLevel).pte)
                            IsWriteDirty(LevelVec(successLevel).pte.asTypeOf(new SV39PTE()), LevelVec(successLevel).addr)
                            // 就是说 暂时做两次 另外一个也用上 但是这样更离谱了 明天接着改这里 感觉需要把PTE也提前存一遍
                            finalSuccess := true.B
                            // FIXME: 其实也不是或到关系 漏洞百出 应该拼接
                            finaladdr := "h0000_0000_8000_0000".U | addr
                        }

                    }
                }.otherwise{
                    // 又失败了
                    finalSuccess := false.B
                    finaladdr := 0.U
                }
                // finaladdr := "h0000_0000_8000_0000".U | addr
            }.otherwise{
                // 翻译失败了
                finalSuccess := false.B
                finaladdr := 0.U
            }

            // 这个时候失败是一定失败 成功可不一定成功
        }.otherwise{
            printf("[Debug] Vaddr illegal\n")
            finalSuccess := false.B
            finaladdr := 0.U
        }
        // Vaddr 前保留位校验 End



        // 合法性检测 Begin
            // 失败 直接return fail
        // 合法性检测 End

        // 在 level为 2 和 1 的 情况下 检测 SuperPage Begin
            // 失败 go bad
        // 在 level为 2 和 1 的 情况下 检测 SuperPage End

        // dirty位处理 Begin

        // dirty位处理 End

        // 返回地址 | OK

        // Bad 处理Begin
            // 再检测一次权限
            // 返回fail
        // Bad 处理End
        (finalSuccess, finaladdr)
        // while( level > 0 ){
        //     println( "Value of level-1: " + (level-1) );
        //     level = level - 1;
        //     // p_pte = pg_base + VPNi(vaddr, level) * PTE_SIZE;
        //     val PTE_PA = Cat(Cat(0.U(8.W),Cat(SatpNow.ppn,addr(38,30))),0.U(3.W))
        //     val PTE = PARead(PTE_PA, 64.U).asTypeOf(new SV39PTE())
        // }
        // val PTE_PA = Cat(Cat(0.U(8.W),Cat(SatpNow.ppn,addr(38,30))),0.U(3.W))
        // val PTE = PARead(PTE_PA, 64.U).asTypeOf(new SV39PTE())
        // when(LegalPage(PTE.flag.asTypeOf(new PTEFlag()), level)){
        //     when(ValidPage(PTE.flag.asTypeOf(new PTEFlag()))){
        //         IsLargePage(PTE.flag.asTypeOf(new PTEFlag()))
        //         addr := "h8000_0000_0000_0000".U | addr
        //     }
        // }.otherwise{
        //     level = level -1
        //     PTE_PA := addr
        //     PTE := PARead(PTE_PA, 64.U).asTypeOf(new SV39PTE())
        //     when(LegalPage(PTE.flag.asTypeOf(new PTEFlag()), level)){
        //         when(ValidPage(PTE.flag.asTypeOf(new PTEFlag()))){
        //             IsLargePage(PTE.flag.asTypeOf(new PTEFlag()))
        //             addr := "h8000_0000_0000_0000".U | addr
        //         }
        //     }
        // }
        // addr
    }
    def PageTableWalk(addr:UInt, accsessType: Int): UInt = {
        // TODO: 可以优化
        printf("[Debug]PageTableWalk_old!!! Addr: %x, Type: %d\n", addr, accsessType.asUInt)
        val SatpNow = now.csr.satp.asTypeOf((new SatpStruct))
        val sum = now.csr.mstatus.asTypeOf((new MstatusStruct)).sum
        val L2PA = Cat(Cat(0.U(8.W),Cat(SatpNow.ppn,addr(38,30))),0.U(3.W))
        val PTE = PARead(L2PA, 64.U).asTypeOf(new SV39PTE())
        val FlagPTE = PTE.flag.asTypeOf(new PTEFlag())
        // val FlagPTEnew = 0.U(8.W).asTypeOf(new PTEFlag())
        // printf("[Debug]L2PA:%x, Page Value:%x \n", L2PA, PARead(L2PA, 64.U))
        // mstatus->sum
        printf("[Debug]FlagPTE: %x, A: %d D: %d PTEAddr:%x \n", FlagPTE.asUInt, FlagPTE.a, FlagPTE.d, L2PA)
        val test = PageTableWalk_new(addr, accsessType)
        printf("[Debug]PageTableWalk_new Test Success:%d, Addr:%x \n", test._1, test._2)
        IsWriteDirty(PTE, L2PA)
        // when(~FlagPTE.a || ~FlagPTE.d){
        //     FlagPTEnew := FlagPTE
        //     FlagPTEnew.a := true.B
        //     FlagPTEnew.d := true.B
        //     val PTEnew = Cat(PTE.reserved.asUInt, PTE.ppn.asUInt, PTE.rsw.asUInt, FlagPTEnew.asUInt)
        //     printf("[Debug]Is Dirty!!! Need Write Addr: %x Value:%x \n", L2PA, PTEnew.asUInt)
        //     PAWriteMMU(L2PA, 64.U, PTEnew.asUInt)
        // }
        "h0000_0000_8000_0000".U | addr
    }
    def AddrTrans(addr:UInt): UInt = {
        val SatpNow = WireInit(now.csr.satp.asTypeOf((new SatpStruct)))
        val sum = now.csr.mstatus.asTypeOf((new MstatusStruct)).sum
        val L2PA = Cat(Cat(0.U(8.W),Cat(SatpNow.ppn,addr(38,30))),0.U(3.W))
        val FlagPTE = PARead(L2PA, 64.U).asTypeOf(new SV39PTE())
        printf("[Debug]L2PA_W:%x, Page Value:%x Flag:%x PPN:%x PA:%x SUM:%x \n", L2PA, PARead(L2PA, 64.U), FlagPTE.flag, FlagPTE.ppn, Cat(0.U(8.W),Cat(FlagPTE.ppn,addr(11,0))), sum)
        // // mstatus->sum
        // "h8000_0000_0000_0000".U | addr
        PageTableWalk(addr,0)
    }
    def AddrTransRead(addr:UInt): UInt = {
        val SatpNow = WireInit(now.csr.satp.asTypeOf((new SatpStruct)))
        val sum = now.csr.mstatus.asTypeOf((new MstatusStruct)).sum
        val L2PA = Cat(Cat(0.U(8.W),Cat(SatpNow.ppn,addr(38,30))),0.U(3.W))
        val FlagPTE = PARead(L2PA, 64.U).asTypeOf(new SV39PTE())
        printf("[Debug]L2PA_R:%x, Page Value:%x Flag:%x PPN:%x PA:%x SUM:%x \n", L2PA, PARead(L2PA, 64.U), FlagPTE.flag, FlagPTE.ppn, Cat(0.U(8.W),Cat(FlagPTE.ppn,addr(11,0))), sum)
        // // mstatus->sum
        "h0000_0000_8000_0000".U | addr
        // PageTableWalk(addr,0)
    }
}