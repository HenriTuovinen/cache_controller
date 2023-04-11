

package cc

import chisel3._
import chiseltest._
//import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.flatspec.AnyFlatSpec
//import chisel3.experimental._
//import chisel3.experimental.BundleLiterals._


/**
  * This is a trivial example of how to run this Specification
  * From within sbt use:
  * {{{
  * testOnly gcd.GcdDecoupledTester
  * }}}
  * From a terminal shell use:
  * {{{
  * sbt 'testOnly gcd.GcdDecoupledTester'
  * }}}
  */
class CacheControllerSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "CacheController"
  it should "testing basic operation with peek poke" in {
    test(new CacheController(2,3,2)) { dut =>
      //val buffSource = io.Source.fromFile("/resources/")
      dut.cpuout.data.peek()
      dut.cpuout.valid.peek()
      dut.cpuout.busy.peek()
      dut.cpuout.hit.peek()

      dut.memout.addr.peek()
      dut.memout.req.peek()
      dut.memout.rw.peek()
      dut.memout.data.peek()


      dut.cpuin.addr.poke("010".U)
      dut.cpuin.valid.poke(true.B)
      dut.cpuin.rw.poke(true.B)
      dut.cpuin.data.poke("11".U)

      dut.memin.data.poke("01".U)
      dut.memin.valid.poke(true.B)
      dut.memin.ready.poke(true.B)

      dut.clock.step(10)

      dut.cpuout.data.peek()
      dut.cpuout.valid.peek()
      dut.cpuout.busy.peek()
      dut.cpuout.hit.peek()

      dut.memout.addr.peek()
      dut.memout.req.peek()
      dut.memout.rw.peek()
      dut.memout.data.peek()
    }
  }
}
