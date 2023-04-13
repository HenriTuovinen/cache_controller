

package cc

import chisel3._
import chiseltest._
//import chisel3.util._
import chisel3.experimental._
//import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
//import chisel3.experimental.BundleLiterals._

import scala.math


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
  val size      : Int = 4
  val addr_len  : Int = 64
  val data_len  : Int = 32


  behavior of "CacheController"

// the issue might be with cache being instantiated inside the cachecontroller and this is not supported by this test
// or this needs some additional steps


it should "test all" in {
    test(new CacheController(size, addr_len, data_len)) {dut =>
      val numTests : Int = 10
      val addr = Seq.tabulate(numTests){i=>scala.util.Random.nextInt(math.pow(2, addr_len).toInt-1).U(addr_len.W)}
      val memdata = Seq.tabulate(numTests){i=>scala.util.Random.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      val writedata = Seq.tabulate(numTests){i=>scala.util.Random.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      
      
      for (i <- 0 until numTests) {
        dut.io.cpuin.addr.poke(addr(i))
        dut.io.cpuin.valid.poke(true.B)
        dut.io.cpuin.rw.poke(false.B)
        //dut.io.cpuin.data.poke(0.U(data_len.W))

        //dut.io.memin.data.poke(0.U)
        dut.io.memin.valid.poke(false.B)
        dut.io.memin.ready.poke(true.B)
        dut.clock.step(1)

        println("??????????????????????????? ")
        println("i is now " + i)
        println("addr is now " + addr(i))
        println("memdata is now " + memdata(i))

        
        //if(true.B.litToBoolean){
        
        //if(dut.io.memout.req.litToBoolean){
        while (dut.io.cpuout.busy.peek().litToBoolean) {
          println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
          if(dut.io.memout.req.peek().litToBoolean){
            println("i is now " + i)
            println("addr is now " + addr(i))
            println("memdata is now " + memdata(i))


            //dut.clock.step(100)

            println("addr from cpu is now " + dut.io.cpuin.addr.peek())
            println("valid from cpu is now " + dut.io.cpuin.valid.peek())


            dut.io.memin.data.poke(memdata(i))
            dut.io.memin.ready.poke(false.B)
            dut.io.memin.valid.poke(true.B)


            //dut.clock.step(100)


          }


          dut.clock.step(1)
        }

        println("addr is now " + dut.io.memout.addr.peek())
        println("data is now " + dut.io.cpuout.data.peek())

        dut.io.cpuout.data.expect(memdata(i))
        //dut.io.cpuout.valid.expect(true.B)
        //dut.io.cpuout.busy.expect(true.B)
        //dut.io.cpuout.hit.expect(true.B)

        dut.io.memout.addr.expect(addr(i))
        //dut.io.memout.req.expect(false.B)
        //dut.io.memout.rw.expect(false.B)
        //dut.io.memout.data.expect(0.U)

      }



      /*
      val addr = Wire(Vec(numTests, UInt(addr_len.W)))      //mignth need Wire() wrapper
      for (i <- 0 until addr.length) {
        addr(i) := scala.util.Random.nextInt(math.pow(2, addr_len).toInt-1).U
        //addr(i) := 2.U(addr_len.W)
        //scala.util.Random.nextBytes(addr_len)
      }
      */
      //val io = IO(chiselTypeOf(dut.io))
      //dut.io <> io
      /*
      val memdata = Wire(Vec(numTests, UInt(data_len.W)))
      for (i <- 0 until memdata.length) {
        memdata(i) := scala.util.Random.nextInt(math.pow(2, data_len).toInt-1).U
      }
      */
      /*
      val writedata = Wire(Vec(numTests, UInt(data_len.W)))           //for testing writing data from cpu
      for (i <- 0 until writedata.length) {
        writedata(i) := scala.util.Random.nextInt(math.pow(2, data_len).toInt-1).U
      }
      */


      /*
      addr.zip(memdata).foreach { case (a, d) =>
        dut.io.cpuin.addr.poke(a)
        dut.io.cpuin.valid.poke(true.B)
        dut.io.cpuin.rw.poke(false.B)
        dut.io.cpuin.data.poke(0.U(data_len.W))

        dut.io.memin.data.poke(0.U)
        dut.io.memin.valid.poke(false.B)
        dut.io.memin.ready.poke(true.B)
        

        when(dut.io.memout.req){
          dut.clock.step(4)
          dut.io.memin.data.poke(d)
          dut.io.memin.ready.poke(false.B)
          dut.io.memin.valid.poke(true.B)
        }

        dut.io.cpuout.data.expect(d)
        dut.io.cpuout.valid.expect(true.B)
        dut.io.cpuout.busy.expect(true.B)
        dut.io.cpuout.hit.expect(true.B)

        dut.io.memout.addr.expect(a)
        dut.io.memout.req.expect(false.B)
        dut.io.memout.rw.expect(false.B)
        dut.io.memout.data.expect(0.U)

      }
      */


      /*
      for {
        a <- addr
        d <- memdata
      } {
        
        dut.io.cpuin.addr.poke(a)
        dut.io.cpuin.valid.poke(true.B)
        dut.io.cpuin.rw.poke(false.B)
        dut.io.cpuin.data.poke(0.U(data_len.W))

        dut.io.memin.data.poke(0.U)
        dut.io.memin.valid.poke(false.B)
        dut.io.memin.ready.poke(true.B)
        

        when(dut.io.memout.req){
          dut.clock.step(4)
          dut.io.memin.data.poke(d)
          dut.io.memin.ready.poke(false.B)
          dut.io.memin.valid.poke(true.B)
        }

        dut.io.cpuout.data.expect(d)
        dut.io.cpuout.valid.expect(true.B)
        dut.io.cpuout.busy.expect(true.B)
        dut.io.cpuout.hit.expect(true.B)

        dut.io.memout.addr.expect(a)
        dut.io.memout.req.expect(false.B)
        dut.io.memout.rw.expect(false.B)
        dut.io.memout.data.expect(0.U)
        


      }
      */

    }
  }


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




  it should "do basic operation with peek poke" in {
    test(new CacheController(2,3,2)) { dut =>
      //val buffSource = io.Source.fromFile("/resources/")
      dut.io.cpuout.data.peek()
      dut.io.cpuout.valid.peek()
      dut.io.cpuout.busy.peek()
      dut.io.cpuout.hit.peek()

      dut.io.memout.addr.peek()
      dut.io.memout.req.peek()
      dut.io.memout.rw.peek()
      dut.io.memout.data.peek()


      dut.io.cpuin.addr.poke("b010".U)
      dut.io.cpuin.valid.poke(true.B)
      dut.io.cpuin.rw.poke(false.B)
      dut.io.cpuin.data.poke("b11".U)

      dut.io.memin.data.poke("b01".U)
      dut.io.memin.valid.poke(true.B)
      dut.io.memin.ready.poke(true.B)

      dut.clock.step(10)
      dut.clock.step(10)

      println(dut.io.cpuout.data.peek().litValue)
      println(dut.io.cpuout.valid.peek().litValue)
      println(dut.io.cpuout.busy.peek().litValue)
      println(dut.io.cpuout.hit.peek().litValue)    

      println(dut.io.memout.addr.peek().litValue)
      println(dut.io.memout.req.peek().litValue)
      println(dut.io.memout.rw.peek().litValue)
      println(dut.io.memout.data.peek().litValue)
            
dut.clock.step(10)

      dut.io.cpuout.data.expect("b01".U)
      dut.io.cpuout.valid.expect(false.B)
      dut.io.cpuout.busy.expect(true.B)
      dut.io.cpuout.hit.expect(false.B)

      dut.io.memout.addr.expect("b010".U)
      dut.io.memout.req.expect(false.B)
      dut.io.memout.rw.expect(false.B)
      dut.io.memout.data.expect("b11".U)
    
    }
  }

  


}
