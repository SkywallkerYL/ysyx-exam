package npc

import chisel3._
import chisel3.util._

class NPCMUX extends Module{
    val io = IO(new Bundle {
      val IDNPC = Flipped((new Idu2Npc))
      val LSNPC = Flipped((new Lsu2Npc))
      //val IFNPC = Flipped((new Ifu2Npc))
      //val PcEnable = Input(Bool())
      //val NPC = Output(UInt(parm.PCWIDTH.W))
      val NOP = Output(Bool())
      //between pcreg
      val RegPc = Flipped((new Pcreg2Npc))//Decoupled
      val NPC   = (((new Npc2Pcreg)))
      val NPCId = ((new Npc2Idu))
      
  })
  //val resetflag = io.PcRegPc===0.U
  //val regpc = Mux(resetflag,parm.INITIAL_PC.U,io.PcRegPc)
  val pc_4 = io.RegPc.RegPc + 4.U
  val jalpc  = io.IDNPC.IdPc + io.IDNPC.imm.asUInt
  val jalrpc = (io.IDNPC.imm.asUInt + io.IDNPC.rs1)&(~ (1.U(parm.REGWIDTH.W)))
  val jumppc = MuxLookup(io.IDNPC.jal,pc_4,Seq(
    0.U -> pc_4   ,
    1.U -> jalpc  ,
    2.U -> jalrpc ,
    3.U -> jalpc  ,
    4.U -> io.IDNPC.ecallpc,
    5.U -> io.IDNPC.mretpc
  ))
  //要给一个valid信号，表明当前idu处理的结果是有效的
  io.NOP := io.IDNPC.valid && io.IDNPC.jal=/=0.U
  //io.NPC := Mux(io.NOP,jumppc,pc_4)
  //io.NPC.npc := Mux(io.PcEnable,Mux(io.NOP,jumppc,pc_4),io.RegPc.RegPc)
  io.NPC.npc := Mux(io.NOP,jumppc,pc_4)
  io.NPC.pcjal := io.NOP
  //io.NPC.pcvalid :=  Mux(io.NOP,io.IDNPC.instvalid,io.LSNPC.instvalid)
  io.NPCId.NextPc := io.NPC.npc

  //io.NPC := Mux(io.resetflag,0.U,)
}