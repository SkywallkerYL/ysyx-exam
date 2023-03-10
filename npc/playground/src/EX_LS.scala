package  npc
import chisel3._
import chisel3.util._
import chisel3.util.HasBlackBoxInline




class EXLSIO extends Bundle{
    val wflag = Output(Bool())
    val rflag = Output(Bool())
    val alures = Output(UInt(parm.REGWIDTH.W))
    val readaddr = Output(UInt(parm.REGWIDTH.W))
    val writeaddr = Output(UInt(parm.REGWIDTH.W))
    val writedata = Output(UInt(parm.REGWIDTH.W))
    val wmask = Output(UInt(parm.BYTEWIDTH.W))
    val choose = Output(UInt(parm.RegFileChooseWidth.W))
    val lsumask = Output(UInt(parm.MaskWidth.W))
    val CsrWb = new CSRWB
    val pc  = Output(UInt(parm.PCWIDTH.W))
    val NextPc  = Output(UInt(parm.PCWIDTH.W))
    //val rdata = Output(UInt(parm.REGWIDTH.W))
    /*
    val CsrAddr = Output(UInt(parm.CSRNUMBER.W))
    val CsrExuChoose = Output(UInt(parm.CSRNUMBER.W))
    val ecall = Output(Bool())
    val mret  = Output(Bool())
    */
}




class EX_LS extends Module{
    val io = IO(new Bundle {
    val Regfile_i = Flipped(new REGFILEIO)
    val EXLS_i = Flipped(new EXLSIO)

    val Regfile_o = new REGFILEIO
    val EXLS_o = new EXLSIO
  })
  if(parm.pip){
      io.Regfile_o :=  RegNext(io.Regfile_i,0.U.asTypeOf(new REGFILEIO))
      io.EXLS_o :=  RegNext(io.EXLS_i,0.U.asTypeOf(new EXLSIO))
  }
  else {
      io.Regfile_o :=  io.Regfile_i
      io.EXLS_o :=  io.EXLS_i
  }

  
}