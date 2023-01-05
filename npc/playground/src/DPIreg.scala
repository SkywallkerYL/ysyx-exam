package npc
import chisel3._
import chisel3.util._
import chisel3.util.HasBlackBoxInline
import chisel3.BlackBox
import chisel3.experimental._ 


class RegInOut extends Bundle{
    val a = Input(VecInit(Seq.fill(parm.RegNumber)(0.U(parm.REGWIDTH.W))))
    val b = Output(VecInit(Seq.fill(parm.RegNumber)(0.U(parm.REGWIDTH.W))))
}

//val ebrdpi = Module(new ebreakDPI)

class regDPI extends BlackBox with HasBlackBoxInline{
    val io = IO(new InOut)
    setInline("regDPI.v",
    """
    |module regDPI(
    |   input a,
    |   output b
    |);
    |
    |assign b = a;
    |
    |import "DPI-C" function void set_gpr_ptr(input logic [63:0] a[]);
    |
    |initial set_gpr_ptr(b);
    |
    |endmodule
    """.stripMargin
    )
}

class regDPIfunc extends Module{
    val io = IO(new InOut)
    val reg = Module(new regDPI)
    reg.io <> io 
}