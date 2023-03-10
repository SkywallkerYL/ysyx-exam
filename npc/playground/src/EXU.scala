package npc

import chisel3._
import chisel3.util._
import chisel3.util.HasBlackBoxInline
import scala.annotation.meta.param
/*
class  ALU (val width : Int = 64) extends Module{
    val io = IO(new Bundle{
        val a = Input(UInt(width.W))
        val b = Input(UInt(width.W))
        val func3 = Input(UInt(3.W))
        val res = Output(UInt(width.W))
    })
    io.res := 0.U
    switch(io.func3){
        is("b000".U) {io.res := io.a+io.b}
        is("b001".U) {io.res := io.a-io.b}
        is("b010".U) {io.res := ~io.a}
        is("b011".U) {io.res := io.a&io.b}
        is("b100".U) {io.res := io.a|io.b}
        is("b101".U) {io.res := io.a^io.b}
        is("b110".U) {io.res := io.a<=io.b}
        is("b111".U) {io.res := io.a===io.b}
    }
}
*/


class EX extends Bundle{
    //val pc = Output(UInt(parm.PCWIDTH.W))
    //val instr = Output(UInt(parm.INSTWIDTH.W))//这个instr可能不需要继续传递了
    //val rs1 = Output(UInt(parm.REGWIDTH.W))
    val rs2 = Output(UInt(parm.REGWIDTH.W))
    //val imm = Output(UInt(parm.REGWIDTH.W))
    val alures = Output(UInt(parm.REGWIDTH.W))
    //val func3 = Output(UInt(3.W))
    //val opcode = Output(UInt(parm.OPCODEWIDTH.W))
    val rddata = Output(UInt(parm.REGWIDTH.W))
    val rdaddr = Output(UInt(parm.REGADDRWIDTH.W))
    val rden = Output(Bool())
}


class EXU extends Module{
    val io = IO(new Bundle {
    val id = Flipped(new Idu2Exu)
    val EXLS = new Exu2Lsu
    val MulU = Flipped(new MultiIO)
    val DivU = Flipped(new DivIO)
    val AluBusy = Output(Bool())
    val AluValid = Output(Bool())
    val PC = new Exu2pc

    val ReadyID = new Exu2Idu
    val ReadyLS =  Flipped(new Lsu2Exu)
  })
  io.EXLS.pc := io.id.pc
  io.EXLS.inst := io.id.inst
  //乘除法信号来的时候valid拉低，这样子下游模块才不会读入，相当于执行级暂停了
  //拉低的操作在状态机内部处理
  io.EXLS.valid := io.id.valid 
  io.EXLS.NextPc := io.id.NextPc
  io.EXLS.rs1 := io.id.rs1addr
  io.EXLS.imm := io.id.imm
  io.EXLS.rdaddr := io.id.rdaddr
  //io.instr_o := io.instr_i
  io.EXLS.rs2 := io.id.rs2
  io.EXLS.RegFileIO.waddr := io.id.rdaddr
  io.EXLS.RegFileIO.wen   := io.id.rden // 这里命名的时候歧义了，其实是wen的信号
  //val alu = Module (new ALU(parm.REGWIDTH))
  //alu.io.func3 := io.func3_i 
  io.EXLS.alures := 0.U

  io.EXLS.instvalid := io.id.instvalid
  io.EXLS.wflag := io.id.wflag
  io.EXLS.rflag := io.id.rflag
  io.EXLS.writedata := io.id.rs2
  io.EXLS.wmask := io.id.wmask
  io.EXLS.choose := io.id.choose
  io.EXLS.lsumask := io.id.lsumask
  io.EXLS.CsrWb <> io.id.CsrWb
  /*-----------------------ALU---------------------*/
  val src1 = io.id.AluOp.rd1

  val src2 = io.id.AluOp.rd2

  val op = io.id.AluOp.op
  //引入了流水线  锁存信号相关的可以删掉了
  //val opReg = RegInit(0.U((OpType.OPNUMWIDTH.W)))
  val useop = Wire(UInt(OpType.OPNUMWIDTH.W))
  useop := op
  //val alumaskReg = RegInit(0.U(parm.MaskWidth.W))
  val usealumask =Wire(UInt(parm.MaskWidth.W))
  usealumask := io.id.alumask
  //val flushReg = RegInit(0.U((1.W)))
  val useflush = Wire(UInt(1.W))
  useflush := false.B
  //val WReg = RegInit(0.U((1.W)))
  val usew = Wire(UInt(1.W))
  usew := false.B
  //val src1Reg = RegInit(0.U(parm.REGWIDTH.W))
  //val src2Reg = RegInit(0.U(parm.REGWIDTH.W))
  val usesrc1 = Wire(UInt(parm.REGWIDTH.W))
  val usesrc2 = Wire(UInt(parm.REGWIDTH.W))
  usesrc1 := src1 
  usesrc2 := src2
  //val shamt = src2(4,0)
  //加入了乘除法单元
  io.MulU.MulValid     := useop === OpType.MUL
  io.MulU.Flush        := useflush
  io.MulU.Mulw         := usew
  io.MulU.MulSigned    := "b00".U
  io.MulU.Multiplicand := usesrc1
  io.MulU.Multiplier   := usesrc2

  io.DivU.DivValid     := useop === OpType.DIVS || useop === OpType.DIV
  io.DivU.Flush        := useflush
  io.DivU.Divw         := usew
  io.DivU.DivSigned    := MuxLookup(useop,"b00".U,Seq(
    OpType.DIVS -> "b11".U,
    OpType.DIV  -> "b00".U
  ))
  io.DivU.Divdend      := usesrc1
  io.DivU.Divisor      := usesrc2
  val sWait :: sWaitReady ::sDoing :: sWaitLsu :: Nil = Enum(4)
  val DoingState = RegInit(sWait)
  val MulDivRes = Wire(UInt(parm.REGWIDTH.W))
  val RegMulDiv = RegInit(0.U(parm.REGWIDTH.W))
  MulDivRes := RegMulDiv
  io.AluBusy := false.B//(DoingState=/=sWait)//|| io.MulU.MulValid || io.DivU.DivValid 
  io.AluValid := false.B
  switch(DoingState){
    is(sWait){
      when(io.MulU.MulValid || io.DivU.DivValid){
        io.AluBusy := true.B
        io.EXLS.valid := false.B
        when((io.MulU.MulValid && io.MulU.MulReady)||(io.DivU.DivValid && io.DivU.DivReady)){
          DoingState := sDoing
        }.otherwise{
          //乘除法模块没有准备好，Exu也跳转，用来区分Exu是否ready
          DoingState := sWaitReady
        }
        //src1Reg := src1
        //src2Reg := src2
        //flushReg := useflush
        //WReg := usew
        //opReg := op
        //alumaskReg := io.id.alumask
      }
      
    }
    is(sWaitReady){
      //useop := opReg
      //usealumask := alumaskReg
      //useflush := flushReg
      //usew := WReg
      //usesrc1 := src1Reg
      //usesrc2 := src2Reg
      io.AluBusy := true.B
      io.EXLS.valid := false.B
      when((io.MulU.MulValid && io.MulU.MulReady)||(io.DivU.DivValid && io.DivU.DivReady)){
        DoingState := sDoing
      }
    }
    is(sDoing){
      //useop := opReg
      io.EXLS.valid := false.B
      io.AluBusy := true.B
      //usealumask := alumaskReg
      when(io.MulU.OutValid){
        io.AluBusy := false.B
        io.EXLS.valid := true.B
        io.AluValid := true.B
        RegMulDiv := io.MulU.ResultL
        MulDivRes := io.MulU.ResultL
        when(io.ReadyLS.ready ){
          DoingState := sWait
        }.otherwise{
          DoingState := sWaitLsu
        }
      }
      when(io.DivU.OutValid){
        io.AluBusy := false.B
        io.EXLS.valid := true.B
        io.AluValid := true.B
        RegMulDiv := io.DivU.Quotient
        MulDivRes := io.DivU.Quotient
        when(io.ReadyLS.ready ){
          DoingState := sWait
        }.otherwise{
          DoingState := sWaitLsu
        }
          
      }

    }
    //还要加一个状态
    /*
      可能乘除法做好了，下游模块还没发出去，这个时候就不能跳回wait，因为上游也阻塞的,寄存器中还是原来的值，
      这个时候就会又向乘除法器发送请求 waitlsu valid还是一直拉高把
    */
    is(sWaitLsu){
      io.EXLS.valid := true.B
      io.AluBusy := false.B
      when(io.ReadyLS.ready ){
        DoingState := sWait
      }
    }
  }
  val AluRes = MuxLookup(useop, src1+src2,Seq(
    OpType.ADD  -> (src1+src2),
    //OpType.ADDW -> func.SignExt(func.Mask((src1+src2),"x0000ffff".U),32),
    OpType.SUB  -> (src1-src2),
    OpType.MUL  -> MulDivRes,//(src1*src2),
    OpType.DIVS -> MulDivRes,//(src1.asSInt/src2.asSInt).asUInt,
    OpType.DIV  -> MulDivRes,//(src1.asUInt/src2.asUInt).asUInt,
    OpType.REMS -> (src1.asSInt%src2.asSInt).asUInt,
    OpType.REM  -> (src1.asUInt%src2.asUInt).asUInt,
    OpType.SLTU -> (src1.asUInt < src2.asUInt),
    OpType.SLT  -> (src1.asSInt < src2.asSInt),
    OpType.SRA  -> (src1.asSInt >> src2.asUInt).asUInt,
    OpType.SRL  -> (src1.asUInt >> src2.asUInt).asUInt,
    OpType.SLL  -> (src1 << src2(5,0)),
    OpType.AND  -> (src1 & src2),
    OpType.OR   -> (src1 | src2),
    OpType.XOR  -> (src1 ^ src2)
  ))
  val maskRes = MuxLookup(usealumask, AluRes,Seq(
    "b11111".U   -> AluRes,
    "b10111".U   ->func.SignExt(func.Mask((AluRes),"x00000000ffffffff".U),32),
    "b10011".U   ->func.SignExt(func.Mask((AluRes),"x000000000000ffff".U),16),
    "b10001".U   ->func.SignExt(func.Mask((AluRes),"x00000000000000ff".U),8),
    "b00111".U   ->func.UsignExt(func.Mask((AluRes),"x00000000ffffffff".U),32),
    "b00011".U   ->func.UsignExt(func.Mask((AluRes),"x000000000000ffff".U),16),
    "b00001".U   ->func.UsignExt(func.Mask((AluRes),"x00000000000000ff".U),8)
    //OpType.ADDW -> func.SignExt(func.Mask((src1+src2),"x0000ffff".U),32),
  ))
  //printf(p"AluRes=0x${Hexadecimal(AluRes)} wflag:  ${io.id.wflag}\n")
  //io.EXLS.rddata:= maskRes
  io.EXLS.alures := maskRes
  //io.EXLS.writeaddr :=  maskRes
  //io.EXLS.readaddr := maskRes
  io.EXLS.pc := io.id.pc
  io.EXLS.jalr := io.id.jalr
  io.EXLS.abort := io.id.abort
  io.EXLS.NextPc := io.id.NextPc
  io.EXLS.RegFileIO.wdata := maskRes
  io.PC.Exuvalid := !(io.AluBusy) & !(io.MulU.MulValid || io.DivU.DivValid)
  //当前周期就要拉低，防止上一级寄存器更新新的数据，保证当前还为处理完的数据还在寄存器中。
  //乘除法完成的哪一个周期,ready就可以拉高了，下一个周期前一级寄存器就把IDU中阻塞的数据取出来了。
  //即处于busy并且数据valid的那个周期也可以拉高
  //exu 也是 aluvalid的那个周期，mulvalid等也是拉高的，要或上取。
  //这里有问题 不能valid或上去 
  /*
    起初没发现问题是因为乘除法指令执行时间太常了了，仿真环境下一般不会出现乘除法做好了下游模块还在阻塞的情况
    如果乘除法做好了，下游模块还在阻塞，那么就要所存一下了
    此时Muldivres用的是默认的寄存器中的值 除了做好的valid周期那个时候，用读出的值

    ready ： 下游阻塞的时候拉低  接收到乘除法模块的当前周期拉低  
  */
  io.ReadyID.ready := (io.ReadyLS.ready && (!(io.AluBusy)))
  //io.EXLS.CsrWb.CSR.mepc := Mux(io.id.CsrExuChoose(0),maskRes,io.id.CsrWb.CSR.mepc)
  //io.EXLS.CsrWb.CSR.mcause := Mux(io.id.CsrExuChoose(1),maskRes,io.id.CsrWb.CSR.mcause)
  //io.EXLS.CsrWb.CSR.mtvec := Mux(io.id.CsrExuChoose(2),maskRes,io.id.CsrWb.CSR.mtvec)
  //io.EXLS.CsrWb.CSR.mstatus := Mux(io.id.CsrExuChoose(3),maskRes,io.id.CsrWb.CSR.mstatus)
}