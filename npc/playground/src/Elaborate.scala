package  npc
import circt.stage._
import npc._

object Elaborate extends App {
  def top = new RiscvCpu()
  val useMFC = true // use MLIR-based firrtl compiler
  val generator = Seq(
    chisel3.stage.ChiselGeneratorAnnotation(() => top),
    //firrtl.stage.RunFirrtlTransformAnnotation(new AddModulePrefix()),
    //ModulePrefixAnnotation("ysyx_22050550_")
  )
  if (useMFC) {
    (new ChiselStage).execute(args, generator :+ CIRCTTargetAnnotation(CIRCTTarget.Verilog))
  } else {
    (new chisel3.stage.ChiselStage).execute(args, generator)
  }
}
