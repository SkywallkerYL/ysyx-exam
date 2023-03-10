package npc

import chisel3._
import chisel3.util._
// pc and inst 一直传递，方便debug
//These signals will be used in Decoupled
//so it can only be one direction
//This file define the Message between each module
//PCReg ---- IF
class Pc2Ifu extends Bundle{
  val pc        = Output(UInt(parm.PCWIDTH.W))
  //val pcvalid   = Output(Bool())
}
//PCReg ---- ID
//流水线情况 通过Pcreg 直接把Ifu下一条取的指令的pc发送给IDU
//因为流水线，id pc 延后了所以
//而NPC的nextpc是根据 pc_reg中的值+4计算的，
//而pc_reg中的本来就是下个周期的
class Pc2Idu extends Bundle{
    val nextpc = Output(UInt(parm.PCWIDTH.W))
}
//IF --- SRAM
class Ifu2Sram extends Bundle{
    val Axi = Flipped(new Axi4LiteRAMIO)
}
class Cpu2Cache extends Bundle{
    val Cache = Flipped(new CacheIO)
}
class Cache2Sram extends Bundle{
    val Axi = Flipped(new Axi4LiteRAMIO)
}
//IF --- NPC
class Ifu2Npc extends Bundle{
    val instvalid = Output(Bool())
}
class Ifu2PcReg extends Bundle{
    val ready = Output(Bool())
}
//IF --- ID
class Ifu2Idu extends Bundle{
  val inst = Output(UInt(parm.INSTWIDTH.W))
  val pc   = Output(UInt(parm.PCWIDTH.W))
  val instvalid = Output(Bool())
}
class Idu2Ifu extends Bundle{
    val ready = Output(Bool())
}
//ID --- NPCMUX
class Idu2Npc extends Bundle{
    //val NextPc  = Input(UInt(parm.PCWIDTH.W))

    val jal     = Output(UInt(OpJType.OPJNUMWIDTH.W))
    val IdPc    = Output(UInt(parm.PCWIDTH.W))
    val imm     = Output(UInt(parm.REGWIDTH.W))
    val rs1     = Output(UInt(parm.REGWIDTH.W))
    val ecallpc = Output(UInt(parm.PCWIDTH.W))
    val mretpc  = Output(UInt(parm.PCWIDTH.W))
    val valid = Output(Bool())
}
class Npc2Idu extends Bundle{
    val NextPc  = Output(UInt(parm.PCWIDTH.W))
}
class Idu2Score extends Bundle{
    val WScore = new WScoreBoardIO
    val RScore = new RScoreBoardIO
}
//NPCMUX --- PCREG
class Npc2Pcreg extends Bundle{
    val npc     = Output(UInt(parm.PCWIDTH.W))
    val pcjal   = Output(Bool())
    //val pcvalid = Output(Bool())
}
class Pcreg2Npc extends Bundle{
    val RegPc   = Output(UInt(parm.PCWIDTH.W))
}
//NPCMUX --- IF
class Npc2Ifu extends Bundle{
    val nop     = Output(Bool())
}
//
//ID --- RegFile
class Idu2Regfile extends Bundle{
    val raddr1  = Output(UInt(parm.REGADDRWIDTH.W))
    val raddr2  = Output(UInt(parm.REGADDRWIDTH.W))
}

class Regfile2Idu extends Bundle{
    val CSRs    = new CSRIO
    val rdata1  = Output(UInt(parm.REGWIDTH.W))
    val rdata2  = Output(UInt(parm.REGWIDTH.W))
}
//ID --- EX
class Idu2Exu extends Bundle{
    val pc          = Output(UInt(parm.PCWIDTH.W))
    val inst        = Output(UInt(parm.INSTWIDTH.W))
    val valid       = Output(Bool())
    val rs1addr     = Output(UInt(parm.REGADDRWIDTH.W))
    val abort       = Output(Bool())
    val jalr        = Output(Bool())
    val rs1         = Output(UInt(parm.REGWIDTH.W))
    val rs2         = Output(UInt(parm.REGWIDTH.W))
    val imm         = Output(UInt(parm.REGWIDTH.W))
    val AluOp       = Output(new ALUOP)
    val rdaddr      = Output(UInt(parm.REGADDRWIDTH.W))
    val rden        = Output(Bool())
    val wflag       = Output(Bool())
    val rflag       = Output(Bool())
    val wmask       = Output(UInt(parm.BYTEWIDTH.W))
    val choose      = Output(UInt(parm.RegFileChooseWidth.W))
    val alumask     = Output(UInt(parm.MaskWidth.W))
    val lsumask     = Output(UInt(parm.MaskWidth.W))
    val src1mask    = Output(UInt(parm.MaskWidth.W))
    val src2mask    = Output(UInt(parm.MaskWidth.W))
    val CsrWb       = new CSRWB
    val NextPc      = Output(UInt(parm.PCWIDTH.W))
    val instvalid   = Output(Bool())
}
class Exu2Idu extends Bundle{
    val ready = Output(Bool())
}
//EX --- LS
class Exu2Lsu extends Bundle{
    //这写是为了后续debug方便添加的
    val pc          = Output(UInt(parm.REGWIDTH.W))
    val inst        = Output(UInt(parm.INSTWIDTH.W))
    val valid       = Output(Bool())
    val rs1         = Output(UInt(parm.REGADDRWIDTH.W))
    val imm         = Output(UInt(parm.REGWIDTH.W))
    val rdaddr      = Output(UInt(parm.REGADDRWIDTH.W))
    val abort       = Output(Bool())
    val jalr  = Output(Bool())

    val rs2         = Output(UInt(parm.REGWIDTH.W))
    val alures      = Output(UInt(parm.REGWIDTH.W))
    val CsrWb       = new CSRWB
    val RegFileIO   = new REGFILEIO
    val wflag       = Output(Bool())
    val rflag       = Output(Bool())
    val instvalid   = Output(Bool())
    //val readaddr    = Output(UInt(parm.REGWIDTH.W))
    //val writeaddr   = Output(UInt(parm.REGWIDTH.W))
    val writedata   = Output(UInt(parm.REGWIDTH.W))
    val wmask       = Output(UInt(parm.BYTEWIDTH.W))
    val choose      = Output(UInt(parm.RegFileChooseWidth.W))
    val lsumask     = Output(UInt(parm.MaskWidth.W))
    //val pc          = Output(UInt(parm.PCWIDTH.W))
    val NextPc      = Output(UInt(parm.PCWIDTH.W))
}
class Lsu2Exu extends  Bundle{
    val ready = Output(Bool())
}
class Exu2pc extends Bundle{
    val Exuvalid = Output(Bool())
}
//LS --- WB
class Lsu2Wbu extends Bundle{
    val choose  = Output(UInt(parm.RegFileChooseWidth.W))
    val Regfile = new REGFILEIO
    val LsuRes  = Output(UInt(parm.REGWIDTH.W))
    val AluRes  = Output(UInt(parm.REGWIDTH.W))
    val CsrWb   = new CSRWB
    val pc      = Output(UInt(parm.PCWIDTH.W))
    val inst    = Output(UInt(parm.INSTWIDTH.W))
    val SkipRef = Output(Bool())
    val abort       = Output(Bool())
    val jalr  = Output(Bool())
    val rs1         = Output(UInt(parm.REGADDRWIDTH.W))
    val imm         = Output(UInt(parm.REGWIDTH.W))
    val rdaddr      = Output(UInt(parm.REGADDRWIDTH.W))
    
    val valid   = Output(Bool())
    val NextPc  = Output(UInt(parm.PCWIDTH.W))
}
class Wbu2Lsu extends Bundle{
    val ready = Output(Bool())
}
class Lsu2Npc extends Bundle{
    val instvalid = Output(Bool())
}
//LS --- SRAM
class Lsu2Sram extends Bundle{
    val Axi = Flipped(new Axi4LiteRAMIO)
}
class Lsu2pc extends Bundle{
    val Lsuvalid = Output(Bool())
}
//ARBITER --- SRAM
class Arb2Sram extends Bundle{
    val Axi = Flipped(new Axi4LiteRAMIO)
}
//WB --- RegFile
class Wbu2Score extends Bundle{
    val WScore = new WScoreBoardIO
}
class Wbu2Regfile extends Bundle{
    //val Reg17       = Input(UInt(parm.REGWIDTH.W))

    val Regfile     = new REGFILEIO
    //val WbuRes      = Output(UInt(parm.REGWIDTH.W))
    val CsrRegfile  = new CSRIO
    val CsrAddr     = Output(UInt(parm.CSRNUMBER.W))
}
class Regfile2Wbu extends Bundle{
    val Reg17       = Output(UInt(parm.REGWIDTH.W)) 
    val CSRs        = new CSRIO   
}


//LS --- CLINT
class Lsu2Clint extends Bundle{
    val Clintls = new CLINTLS
}
//CLINT --- WB
class Clint2Wbu extends Bundle{
    val Mtip = Output(Bool()) 
}