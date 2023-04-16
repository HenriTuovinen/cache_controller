

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
import scala.util.Random
import os.truncate


class CacheControllerSpec extends AnyFlatSpec with ChiselScalatestTester {
  val size      : Int = 4
  val addr_len  : Int = 64
  val data_len  : Int = 32


  behavior of "CacheController"




 it should "testWrite" in {
    test(new CacheController(size, addr_len, data_len)).withAnnotations (Seq( WriteVcdAnnotation )) {dut =>//. withAnnotations (Seq( WriteVcdAnnotation ))
      val randgen = new Random(777)
      val numTests : Int  = 10
      val addr            = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, addr_len).toInt-1).U(addr_len.W)}
      val memdata         = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      val writedata       = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      
      
      for (i <- 0 until numTests) {
        dut.io.cpuin.addr.poke(addr(i))
        dut.io.cpuin.valid.poke(true.B)
        dut.io.cpuin.rw.poke(true.B)
        dut.io.cpuin.data.poke(writedata(i))

        dut.io.memin.data.poke(0.U)
        dut.io.memin.valid.poke(false.B)
        dut.io.memin.ready.poke(true.B)
        dut.clock.step(1)

        


        while (dut.io.cpuout.busy.peek().litToBoolean && !(dut.io.cpuout.hit.peek().litToBoolean)) {
          //println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
          if(dut.io.memout.req.peek().litToBoolean){
            dut.io.memin.ready.poke(false.B)
            println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
            dut.clock.step(4)

            //dut.io.memout.data.expect(writedata(i))
            //dut.io.memin.ready.poke(false.B)
            dut.io.memin.valid.poke(true.B)





          }


          dut.clock.step(1)
        }



        println("second run with same addr        " + dut.io.memout.addr.peek().litValue.toInt.toBinaryString.slice(0,size))

        //dut.io.cpuout.data.expect(memdata(i))


        //dut.io.memout.addr.expect(addr(i))


        dut.clock.step(1)




        dut.io.cpuin.addr.poke(addr(i))
        dut.io.cpuin.valid.poke(true.B)
        dut.io.cpuin.rw.poke(true.B)
        dut.io.cpuin.data.poke(memdata(i))

        dut.io.memin.data.poke(0.U)
        dut.io.memin.valid.poke(false.B)
        dut.io.memin.ready.poke(true.B)
        dut.clock.step(1)

        


        while (dut.io.cpuout.busy.peek().litToBoolean && !(dut.io.cpuout.hit.peek().litToBoolean)) {
          //println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
          if(dut.io.memout.req.peek().litToBoolean){
            dut.io.memin.ready.poke(false.B)

            dut.clock.step(4)

            dut.io.memout.data.expect(memdata(i))
            //dut.io.memin.ready.poke(false.B)
            dut.io.memin.valid.poke(true.B)





          }


          dut.clock.step(1)
        }



        println("addr is now        " + dut.io.memout.addr.peek().litValue.toInt.toBinaryString.slice(0,size))

        //dut.io.cpuout.data.expect(memdata(i))


        //dut.io.memout.addr.expect(addr(i))


        dut.clock.step(1)
        

      }
    }
  }


























  it should "testAll" in {
    test(new CacheController(size, addr_len, data_len)).withAnnotations (Seq( WriteVcdAnnotation )) {dut =>//. withAnnotations (Seq( WriteVcdAnnotation ))
      val randgen = new Random(777)
      val numTests : Int  = 10
      val addr            = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, addr_len).toInt-1).U(addr_len.W)}
      val memdata         = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      val writedata       = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      
      
      for (i <- 0 until numTests) {
        dut.io.cpuin.addr.poke(addr(i))
        dut.io.cpuin.valid.poke(true.B)
        dut.io.cpuin.rw.poke(false.B)
        dut.io.cpuin.data.poke(0.U(data_len.W))

        dut.io.memin.data.poke(0.U)
        dut.io.memin.valid.poke(false.B)
        dut.io.memin.ready.poke(true.B)
        dut.clock.step(1)

        

        
        //if(true.B.litToBoolean){
        
        //if(dut.io.memout.req.litToBoolean){
        while (dut.io.cpuout.busy.peek().litToBoolean && !(dut.io.cpuout.hit.peek().litToBoolean)) {
          //println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
          if(dut.io.memout.req.peek().litToBoolean){
            dut.io.memin.ready.poke(false.B)
            //println("????????????????????????????")

            //dut.clock.step(100)

            //println("addr from cpu is now " + dut.io.cpuin.addr.peek())
            //println("valid from cpu is now " + dut.io.cpuin.valid.peek())

            //maybe need some logic os that valid is slow only for one cycle
            dut.clock.step(4)

            dut.io.memin.data.poke(memdata(i))
            //dut.io.memin.ready.poke(false.B)
            dut.io.memin.valid.poke(true.B)


            //dut.clock.step(100)


          }


          dut.clock.step(1)
        }
        //println("we finally have a hit")
        println("addr is now        " + dut.io.memout.addr.peek().litValue.toInt.toBinaryString.slice(0,size))
        /*
        println("??????????????????????????? ")
        println("i is now " + i)
        println("addr should be now " + addr(i))
        println("addr is now        " + dut.io.memout.addr.peek())
        println("memdata is now " + memdata(i))
        println("data is now    " + dut.io.cpuout.data.peek())
        */
        dut.io.cpuout.data.expect(memdata(i))
        //dut.io.cpuout.valid.expect(true.B)
        //dut.io.cpuout.busy.expect(true.B)
        //dut.io.cpuout.hit.expect(true.B)

        dut.io.memout.addr.expect(addr(i))
        //dut.io.memout.req.expect(false.B)
        //dut.io.memout.rw.expect(false.B)
        //dut.io.memout.data.expect(0.U)

        dut.clock.step(1)
        /*
        //println("cheking if that shit is still in memory")

        dut.io.cpuin.addr.poke(addr(i))
        dut.io.cpuin.valid.poke(true.B)
        dut.io.cpuin.rw.poke(false.B)
        //dut.io.cpuin.data.poke(0.U(data_len.W))

        dut.io.memin.data.poke(0.U)
        dut.io.memin.valid.poke(false.B)
        dut.io.memin.ready.poke(true.B)
        dut.clock.step(1)

        while (dut.io.cpuout.busy.peek().litToBoolean && !(dut.io.cpuout.hit.peek().litToBoolean)) {
          //println("this is a minor issue maybe")
          if(dut.io.memout.req.peek().litToBoolean){
            println("We should not be here")

            dut.io.memin.data.poke(memdata(i))
            dut.io.memin.ready.poke(false.B)
            dut.io.memin.valid.poke(true.B)
          }


          dut.clock.step(1)
        }
        */

        



      }
      dut.clock.step(10)
      println("lets go around another time and see that the data that should stay in memory does stay there")

      for (i <- 0 until numTests) {
          
        //println("lets go around another time and see that the data that should stay in memory does stay there")

        dut.io.cpuin.addr.poke(addr(i))
        dut.io.cpuin.valid.poke(true.B)
        dut.io.cpuin.rw.poke(false.B)
        dut.io.cpuin.data.poke(0.U(data_len.W))

        dut.io.memin.data.poke(0.U)
        dut.io.memin.valid.poke(false.B)
        dut.io.memin.ready.poke(true.B)
        println("addr that was input was " + addr(i).litValue.toInt.toBinaryString.slice(0,size))

        dut.clock.step(1)

        while (dut.io.cpuout.busy.peek().litToBoolean && !(dut.io.cpuout.hit.peek().litToBoolean)) {
            
          //println("!########################!!##!#!##!#!#!#!#!#")
          if(dut.io.memout.req.peek().litToBoolean){
            dut.io.memin.ready.poke(false.B)
            println("we should only be here if there are two same adresses")
            println("addr is now             " + dut.io.memout.addr.peek().litValue.toInt.toBinaryString.slice(0,size))

            dut.clock.step(4)

            //println("addr from cpu is now " + dut.io.cpuin.addr.peek())
            //println("valid from cpu is now " + dut.io.cpuin.valid.peek())


            dut.io.memin.data.poke(memdata(i))

            dut.io.memin.valid.poke(true.B)


            //dut.clock.step(100)


          }


          dut.clock.step(1)
        }
      }
    }
  }


  







}
