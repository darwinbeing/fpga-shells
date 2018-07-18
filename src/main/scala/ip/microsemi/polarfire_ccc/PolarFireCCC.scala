// See LICENSE for license details.
package sifive.fpgashells.ip.microsemi.polarfireccc

import Chisel._
import freechips.rocketchip.util.{ElaborationArtefacts}
import freechips.rocketchip.config._
import sifive.fpgashells.clocks._

// Black Box for Microsemi PolarFire Clock Conditioning Circuit (CCC) Actel:SgCore:PF_CCC:1.0.112
class PolarFireCCCIOPads(c : PLLParameters) extends Bundle {
  val REF_CLK_0      = Clock(INPUT)
  val OUT0_FABCLK_0  = if (c.req.size >= 1) Some(Clock(OUTPUT)) else None
  val OUT1_FABCLK_0  = if (c.req.size >= 2) Some(Clock(OUTPUT)) else None
  val OUT2_FABCLK_0  = if (c.req.size >= 3) Some(Clock(OUTPUT)) else None
  val OUT3_FABCLK_0  = if (c.req.size >= 4) Some(Clock(OUTPUT)) else None
  val PLL_LOCK_0     = Bool(OUTPUT)
}

class PolarFireCCC(c : PLLParameters) extends BlackBox with PLLInstance {
  override def desiredName = c.name

  val io = new PolarFireCCCIOPads(c)

  def getInput = io.REF_CLK_0
  def getReset = None
  def getLocked = io.PLL_LOCK_0
  def getClocks = Seq() ++ io.OUT0_FABCLK_0 ++ io.OUT1_FABCLK_0 ++ 
                           io.OUT2_FABCLK_0 ++ io.OUT3_FABCLK_0 

  def getClockNames = Seq.tabulate (c.req.size) { i =>
    s"${c.name}/${c.name}_0/pll_inst_0/OUT${i}"
  }
  
  val used = Seq.tabulate(4) { i =>
    s" GL${i}_0_IS_USED:${i < c.req.size} \\\n"
  }.mkString

  val outputs = c.req.zipWithIndex.map { case (req, i) =>
      s""" GL${i}_0_OUT_FREQ:${req.freqMHz} \\
         | GL${i}_0_PLL_PHASE:${req.phaseDeg} \\
         |""".stripMargin
  }.mkString

  // !!! work-around libero bug
  // val feedback = if (c.input.feedback) "External" else "Post-VCO"
  val feedback = "Post-VCO"

  ElaborationArtefacts.add(s"${desiredName}.libero.tcl",
    s"""create_design -id Actel:SgCore:PF_CCC:1.0.112 -design_name {${desiredName}} -config_file {} -params {} -inhibit_configurator 0
       |open_smartdesign -design {${desiredName}}
       |configure_design -component {${desiredName}} -library {}
       |configure_vlnv_instance -component {${desiredName}} -library {} -name {${desiredName}_0}  -validate_rules 0 -params { \\
       | PLL_IN_FREQ_0:${c.input.freqMHz} \\
       | PLL_FEEDBACK_MODE_0:${feedback} \\
       |${used}${outputs}}
       |fix_vlnv_instance -component {${desiredName}} -library {} -name {${desiredName}_0}
       |open_smartdesign -design {${desiredName}}
       |configure_design -component {${desiredName}} -library {}
       |""".stripMargin)
}
