

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



    test(new Cache(4,1,3)) { dut =>


      

      //val buffSource = io.Source.fromFile("/resources/")
      //dut.io.addr.peek()
      //dut.io.datain.peek()
      
      //dut.io.we.peek()

      //println("before input" + dut.io.dataout.peek().litValue)
      //printf("test")

      dut.clock.step(10)

      dut.io.addr.poke(3.U)
      dut.io.tag.poke(1.U) 
      dut.io.datain.poke(2.U)
      dut.io.we.poke(true.B)


      dut.clock.step(10)

      //println("after input" + dut.io.dataout.peek().litValue)

      dut.io.dataout.expect(2.U)
      dut.io.tagout.expect(1.U)
      dut.io.valid.expect(true.B)
    }
  }


  it should "just peekking into controller" in {                            //test the cache alone
    test(new CacheController(2,3,2)) { dut =>
      //val buffSource = io.Source.fromFile("/resources/")
      
    }
  }




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


      dut.cpuin.addr.poke("b010".U)
      dut.cpuin.valid.poke(true.B)
      dut.cpuin.rw.poke(false.B)
      dut.cpuin.data.poke("b11".U)

      dut.memin.data.poke("b01".U)
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



      dut.cpuout.data.expect("b01".U)
      dut.cpuout.valid.expect(true.B)
      dut.cpuout.busy.expect(true.B)
      dut.cpuout.hit.expect(true.B)

      dut.memout.addr.expect("b010".U)
      dut.memout.req.expect(false.B)
      dut.memout.rw.expect(false.B)
      dut.memout.data.expect("b11".U)
    }
  }
}
