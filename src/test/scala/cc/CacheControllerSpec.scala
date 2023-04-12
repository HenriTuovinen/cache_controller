

package cc

import chisel3._
import chiseltest._
//import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
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

// the issue might be with cache being instantiated inside the cachecontroller and this is not supported by this test
// or this needs some additional steps


  it should "testing cache only" in {                            //test the cache alone



    test(new Cache(2,3,4)) { dut =>
      //val buffSource = io.Source.fromFile("/resources/")
      //dut.io.addr.peek()
      //dut.io.datain.peek()
      
      //dut.io.we.peek()

      //println("before input" + dut.io.dataout.peek().litValue)
      //printf("paskaaa")

      dut.io.we.poke(1.U)
      dut.io.addr.poke(7.U)
      dut.io.datain.poke(2.U)

      dut.clock.step(10)

      //println("after input" + dut.io.dataout.peek().litValue)
      dut.io.dataout.expect("hb".U)
    }
  }

/*
  it should "just peekking into controller" in {                            //test the cache alone
    test(new CacheController(2,3,2)) { dut =>
      //val buffSource = io.Source.fromFile("/resources/")
      
    }
  }
*/


/*
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
  */

}
