package npc

import chisel3._
import chisel3.util._
import chisel3.util.HasBlackBoxInline

class  RiscvCpu extends Module{
    val io = IO(new Bundle{
        //val PcFlag =  Input(Bool())
        //val PCIN = Input(UInt(parm.PCWIDTH.W))
        val halt = Output(Bool())
        val abort = Output(Bool())
        val jalr = Output(Bool())
        val instvalid = Output(Bool())
        val pcvalid = Output(Bool())
//if (parm.DIFFTEST){
        val SkipRef = Output(Bool())
//}
    })
    //val M = Mem(parm.MSIZE/4,UInt(parm.INSTWIDTH.W))
    //chisel里变量还未命名好像不能直接用
    val PcReg = Module(new PC_REG()) 
    val NpcMux = Module(new NPCMUX())
    val SRAM    = Module(new Axi4LiteSRAM)
    
    val ICache   = Module(new CpuCache(true))
    val DCache   = Module(new CpuCache)
    //val SRAMLSU = Module(new Axi4LiteSRAM)
    //模拟设备的
    val SRAMDEV = Module(new Axi4LiteSRAM)
    val SramArb = Module(new RamArbiter)
    val Ifu = Module(new IFU())
    //val If_Id = Module(new IF_ID())
    val Regfile = Module(new RegFile)
    val ScoreB = Module(new ScoreBoard)
    val Idu = Module(new IDU())
    //val Id_Ex = Module(new ID_EX())
    val Exu = Module(new EXU())
    //val Ex_Ls = Module(new EX_LS())
    val Lsu = Module(new LSU())
    val Clint = Module(new CLINT())
    //val Ls_Wb = Module(new LS_WB())
    val Wbu = Module(new WBU())
// Ram
    SRAM.io.Sram <> ICache.io.Sram.Axi
    //SRAMLSU.io.Sram <> DCache.io.Sram.Axi
    SRAM.io.Sram <> SramArb.io.sram.Axi
    SramArb.io.ifu.Axi <> ICache.io.Sram.Axi
    SramArb.io.lsu.Axi <> DCache.io.Sram.Axi
//pc   
    val PcRegOut = Wire(UInt(parm.PCWIDTH.W))
    //val addr  = (PcRegOut-parm.INITIAL_PC.U)>>2
    //val instr = M(addr)//因为M/4，所以PC要把低两位去掉
    //printf(p"addr=${addr} instr=0x${Hexadecimal(M(addr))} \n")
    //printf(p"pc=0x${Hexadecimal(PcRegOut)} instr=0x${Hexadecimal(instr)}\n")
    NpcMux.io.IDNPC <> Idu.io.IDNPC
    NpcMux.io.LSNPC <> Lsu.io.NPC
    //val resetflag = PcReg.io.pc_o === 0.U
    //PcReg.io.pc_i := NpcMux.io.NPC
    PcRegOut := PcReg.io.PcIf.pc
    PcReg.io.RegPc <> NpcMux.io.RegPc
    NpcMux.io.NPC  <> PcReg.io.NPC
    PcReg.io.ReadyIF <> Ifu.io.ReadyPC
    //NpcMux.io.PcEnable := Ifu.io.instvalid & Lsu.io.Lsuvalid
//ifu
    //val Ifu = Module(new IFU())
    
    PcReg.io.PcIf  <> Ifu.io.PcIf
    Ifu.io.Cache <> ICache.io.Cache
    
    ICache.io.pc := Ifu.io.PcIf.pc
   // Ifu.io.IFRAM <> SramArb.io.ifu//SRAM.io.Sram
    //Ifu.io.pc_i := PcReg.io.pc_o
    //Ifu.io.instr_i := instr
    if(parm.MODE == "single"){
        if(parm.DPI){
            val instrread = Module(new InstrReadDPI)
            instrread.io.a := PcReg.io.PcIf.pc
            Ifu.io.instr_i := instrread.io.b
        }
    }
    else {
        Ifu.io.instr_i := 0.U
        //Ifu.io.IFRAM <> SRAM.io.Sram
    }
//if_id
    Ifu.io.ReadyID <> Idu.io.ReadyIF
//pipline  流水化
//整理一下流水线的大致思路
/* 
    首先是关于下游模块什么时候读信号，个人认为是上游valid的时候即表示当前寄存器中的值是可以继续流动的,
    这个valid信号是根据当前模块的数据是否有效来的由模块内部和上游接受的valid决定
    //这样子很多模块内部也不用锁存数据了，因为数据自动缩存流水级的寄存器内
    //这一点只能慢慢调整了

    还有就是寄存器什么时候写入上有模块的值，个人理解是下游的ready拉高的时候，表示下游模块这个周期可以处理完数据
    //并且下个周期能够接受新的数据，即可以写入上有模块的数据
*/
    val IfidEnable = Idu.io.ReadyIF.ready
    val IfidReg = RegEnable(Ifu.io.IFID,IfidEnable)
    Idu.io.IFID := Mux(IfidReg.instvalid,IfidReg,0.U.asTypeOf(new Ifu2Idu))
    //If_Id.io.nop := NpcMux.io.NOP
// regfile
    Regfile.io.IDRegFile <> Idu.io.IDRegFile
    Regfile.io.WBREG <> Wbu.io.WBREG 
    Regfile.io.pc := Ifu.io.IFID.pc//
//id
    //val Idu = Module(new IDU())
    Idu.io.NPC <> NpcMux.io.NPCId
    //Idu.io.pc_i := If_Id.io.idpc
    Idu.io.RegFileID <> Regfile.io.RegFileID
    Idu.io.Score <> ScoreB.io.IDU
//ID_EX
    //val Id_Ex = Module(new ID_EX())
    //Idu.io.
    Idu.io.ReadyEX := Exu.io.ReadyID
    val IdexEnable = Exu.io.ReadyID.ready
    val IdexReg = RegEnable(Idu.io.idex,IdexEnable)
    
    Exu.io.id := Mux(IdexReg.valid,IdexReg,0.U.asTypeOf(new Idu2Exu))  // RegEnable
//EXU
    val DivU = Module(new Divder)
    val MulU = Module(new Multi)
    Exu.io.DivU <> DivU.io.Exu
    Exu.io.MulU <> MulU.io.Exu
    Exu.io.PC <> PcReg.io.EXU
    Exu.io.ReadyLS <> Lsu.io.ReadyEX
    //val Exu = Module(new EXU())
//EX_LS
    //Lsu部分接入流水线
    //两层逻辑一个是LSU接受的逻辑，另一个是EXU发送
    //首先EXU接收到乘除法指令时，发送忙信号流水线寄存当前所有的所有的值 并且整个流水线要暂停
    //然后是LSU 接受，如果发现流水线来的信号busy拉高了，那么LSU的其他信号都要拉低
    //并且当valid到来时，流水线继续，然后并且流水级的计算结果等信号更新成乘除法的计算结果。。
    val ExlsEnable = Lsu.io.ReadyEX.ready//exu不忙的情况下，流水线才更新
    val RegExls = RegEnable(Exu.io.EXLS,ExlsEnable)
    //valid信号到来的时候，流水线的值要更新成其计算结果
    //这里也是,exu计算完成了，valid更新,新的数据也自动写入寄存器，当然这是在下游是ready的情况下。
    //比如乘除法指令到来的时候，当周期对下游模块的valid拉低，
    //当前周期ready也要拉低，保证寄存器不会写入IDU当前周期的驿马结果                                   
    //下一个周期exu进入busy，exls的寄存器写入了valid为低的信号，不会再有有效结果进行传递
    //ready信号会一直向前传递，阻塞IF_ID模块的写入，让IDU一直接受当前美传下去的驿马结果，
    //IFU也被阻塞，由于idready拉低，IF也不会向cache发送新的取指请求，即不会有新的指令向if_id写入
    //这样子也不用在模块内部锁存信号了，因为流水线阻塞了，信号还在寄存器中
    //主要是EXU和LSU两个模块索存了大量的信号,这里要把以前写的锁存的删掉了。。。。
    //alures 更新 然后是寄存器的写如更新。。。。
    //严格来说额,这两是一个东西，但是写的时候每分开。
    /*
    when(Exu.io.AluValid){
        RegExls.alures := Exu.io.EXLS.alures
        RegExls.RegFileIO.wdata := Exu.io.EXLS.alures
    }
    */
    Lsu.io.EXLS := Mux(RegExls.valid,RegExls,0.U.asTypeOf(new Exu2Lsu))//Exu.io.EXLS
// LSU
    Lsu.io.Cache <> DCache.io.Cache
    DCache.io.pc := Ifu.io.IFID.pc
    Lsu.io.PC <> PcReg.io.LSU
    
    Lsu.io.LSRAM.Axi <> SRAMDEV.io.Sram//SRAMLSU.io.Sram
// CLINT
    Clint.io.LsuIn <> Lsu.io.LSCLINT
//LS_WB
    Lsu.io.ReadyWB <> Wbu.io.ReadyLS
    //流水线
    // 
    val LswbEnable = Wbu.io.ReadyLS.ready
    val LswbReg = RegEnable(Lsu.io.LSWB,LswbEnable)
    Wbu.io.LSWB := Mux(LswbReg.valid,LswbReg,0.U.asTypeOf(new Lsu2Wbu))
//WB
    Wbu.io.REGWB <>  Regfile.io.REGWB 
    Wbu.io.CLINTWB  := Clint.io.CLINTWB
    Wbu.io.Score <> ScoreB.io.WBU
//out
    if(parm.DPI){
        val ebrdpi = Module(new ebreakDPI)
        ebrdpi.io.a := Idu.io.ebreak 
        val pcdpi = Module(new pcDPI)
        pcdpi.io.pc := Idu.io.pc_o
        pcdpi.io.dnpc := NpcMux.io.NPC.npc
        val instrdpi = Module(new InstrFetchDPI)
        instrdpi.io.a := Idu.io.instr_o
        val srcdpi = Module(new SrcFetchDPI)
        srcdpi.io.rs1 := Idu.io.IDRegFile.raddr1
        srcdpi.io.rd := Idu.io.idex.rdaddr
        srcdpi.io.imm := Idu.io.idex.imm
    }
    //when it is not need ,it can be removed
    io.instvalid := Ifu.io.IFID.instvalid
    io.pcvalid := false.B//PcReg.io.PcIf.pcvalid
    io.halt := Idu.io.ebreak&&(Regfile.io.a0data===0.U)
    io.abort := Idu.io.instrnoimpl
    io.jalr := Idu.io.IDNPC.jal === 2.U
    if (parm.DIFFTEST){
    io.SkipRef := Lsu.io.SkipRef
    }  else io.SkipRef := false.B
    //io.res := Exu.io.expres

}
//for AXI-LITE signals , use these function
object DecoupledConnect{
    def apply[T <: Data] (arch: String,left: DecoupledIO[T], right:DecoupledIO[T] ) = {
        //val arch = "single"
        if (arch == "single") {right.bits := left.bits}
        else if (arch == "multi") {right <> left} 
        else if (arch == "pipeline") {right <> RegEnable(left,left.fire)}
    }
}
