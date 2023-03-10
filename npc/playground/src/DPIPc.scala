package  npc
import chisel3._
import chisel3.util._
import chisel3.util.HasBlackBoxInline
import chisel3.BlackBox
import chisel3.experimental._ 

class PcIO extends Bundle{
    val pc = Input(UInt(parm.PCWIDTH.W))
    val dnpc = Input(UInt(parm.PCWIDTH.W))
}

class pcDPI extends BlackBox with HasBlackBoxInline{
    val io = IO(new PcIO)
    setInline("pcDPI.v",
    """
    |module pcDPI(
    |   input [63:0] pc,
    |   input [63:0] dnpc
    |);
    |
    |export "DPI-C" function pc_fetch;
    |export "DPI-C" function npc_fetch;
    |function longint pc_fetch;
    |/*
    |   integer k;
    |   begin
    |       for (k=0;k<64;k=k+1)begin
    |           pc_fetch[k] = pc[k];
    |       end
    |   end
    |*/
    |   pc_fetch = pc;
    |endfunction
    |
    |function longint npc_fetch;
    |/*
    |   integer k;
    |   begin
    |       for (k=0;k<64;k=k+1)begin
    |           npc_fetch[k] = dnpc[k];
    |       end
    |   end
    |*/
    |   npc_fetch = dnpc;
    |endfunction
    |
    |endmodule
    """.stripMargin
    )
}