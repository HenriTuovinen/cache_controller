//poke expect test of the cases I could come up with

package cc

import chisel3._
import chiseltest._
import chisel3.experimental._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


import scala.math
import scala.util.Random          //Random is used in most test just to generate large numbers. In most tests seed is needed to get expected numbers for testing. Only last test needs random behaviour by desing.
import os.truncate


class CacheControllerSpec extends AnyFlatSpec with ChiselScalatestTester {
  //changing these will cause issues with the test. Cache controller most likely still operates correctly the tests just expect certain kinds of random numbers which will become untrue with certain length.
  //actually making a test that can handle all possible size combinations seems nontrivial since the same issue arises even with set numbers
  val size      : Int = 4         //set cache depth
  val addr_len  : Int = 32        //set cpu/memory address
  val data_len  : Int = 32        //set word length
  val debug     : Boolean = true  //enables viewing the cache contents with wires from the VCD files


  behavior of "CacheController"



//val addr = Seq.tabulate(numTests){i=>scala.util.Random.nextInt(math.pow(2, addr_len).toInt-1).U(addr_len.W)}
//. withAnnotations (Seq( WriteVcdAnnotation ))



  it should "test Reading" in {
    test(new CacheController(size, addr_len, data_len, debug)).withAnnotations (Seq( WriteVcdAnnotation )) {dut =>
      val randgen         = new Random(777)         
      val numTests : Int  = 10                      
      
      val addr            = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, addr_len).toInt-1).U(addr_len.W)}
      val memdata         = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      val writedata       = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      
      
      for (i <- 0 until numTests) {
        if(i % 3 == 0){                               //test idling the cache controller when cpu doesn't want anything
          dut.io.cpuin.valid.poke(false.B)
          dut.clock.step(4)
          dut.io.cpuout.busy.expect(false.B)
        }

        dut.io.cpuin.addr.poke(addr(i))
        dut.io.cpuin.valid.poke(true.B)
        dut.io.cpuin.rw.poke(false.B)
        dut.io.cpuin.data.poke(0.U)

        dut.io.memin.data.poke(memdata(i))
        dut.io.memin.valid.poke(false.B)
        dut.io.memin.ready.poke(true.B)

        dut.clock.step(1)

        while (dut.io.cpuout.busy.peek().litToBoolean && !(dut.io.cpuout.valid.peek().litToBoolean)) {  //step clock while cc is busy
          if(dut.io.memout.req.peek().litToBoolean){
            dut.io.memout.addr.expect(addr(i))
            dut.io.memout.req.expect(true.B)
            dut.io.memout.rw.expect(false.B)

            dut.io.memin.ready.poke(false.B)
            dut.clock.step(4)
            dut.io.memin.valid.poke(true.B)
            dut.io.memin.ready.poke(true.B)
          }
          dut.clock.step(1)
        }

        dut.io.cpuout.data.expect(memdata(i))
        dut.io.cpuout.valid.expect(true.B)
        dut.io.cpuout.busy.expect(true.B)
        dut.io.cpuout.hit.expect(false.B)             // the tags never match so no hits

      }
    }
  }






  
  it should "test Reading twice in a row from same address, aka testing hit" in {
    test(new CacheController(size, addr_len, data_len, debug)).withAnnotations (Seq( WriteVcdAnnotation )) {dut =>//. withAnnotations (Seq( WriteVcdAnnotation ))
      val randgen         = new Random(777)
      val numTests : Int  = 10
      val addr            = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, addr_len).toInt-1).U(addr_len.W)}
      val memdata         = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      val writedata       = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      
      
      for (i <- 0 until numTests) {
        if(i % 3 == 0){                               //test idling the cache controller when cpu doesn't want anything
          dut.io.cpuin.valid.poke(false.B)
          dut.clock.step(4)
          dut.io.cpuout.busy.expect(false.B)
        }

        dut.io.cpuin.addr.poke(addr(i))
        dut.io.cpuin.valid.poke(true.B)
        dut.io.cpuin.rw.poke(false.B)
        dut.io.cpuin.data.poke(0.U)

        dut.io.memin.data.poke(memdata(i))
        dut.io.memin.valid.poke(false.B)
        dut.io.memin.ready.poke(true.B)

        dut.clock.step(1)


        while (dut.io.cpuout.busy.peek().litToBoolean && !(dut.io.cpuout.valid.peek().litToBoolean)) {  //step clock while cc is busy
          if(dut.io.memout.req.peek().litToBoolean){
            dut.io.memout.addr.expect(addr(i))
            dut.io.memout.req.expect(true.B)
            dut.io.memout.rw.expect(false.B)

            dut.io.memin.ready.poke(false.B)
            dut.clock.step(4)
            dut.io.memin.valid.poke(true.B)
            dut.io.memin.ready.poke(true.B)
          }
          dut.clock.step(1)
        }

        dut.io.cpuout.data.expect(memdata(i))
        dut.io.cpuout.valid.expect(true.B)
        dut.io.cpuout.busy.expect(true.B)
        dut.io.cpuout.hit.expect(false.B)             // the tags never match so no hits



        
        dut.clock.step(1)





        dut.io.memin.data.poke(0.U)                   //this is intentionally "wrong" so reading from memory results in error, we should not be reading from memory
        dut.io.memin.valid.poke(false.B)
        dut.io.memin.ready.poke(true.B)
        dut.clock.step(1)

        


        while (dut.io.cpuout.busy.peek().litToBoolean && !(dut.io.cpuout.valid.peek().litToBoolean)) {
          dut.io.memout.req.expect(false.B)       //we should never request data from memory
       
          dut.clock.step(1)
        }

        dut.io.cpuout.data.expect(memdata(i))
        dut.io.cpuout.valid.expect(true.B)
        dut.io.cpuout.busy.expect(true.B)
        dut.io.cpuout.hit.expect(true.B)

        dut.io.memout.addr.expect(addr(i))
      }
    }
  }






  
 it should "test Writing" in {              //write-backs will not happen since no duplicate indexes
    test(new CacheController(size, addr_len, data_len, debug)).withAnnotations (Seq( WriteVcdAnnotation )) {dut =>//. withAnnotations (Seq( WriteVcdAnnotation ))
      val randgen         = new Random(777)
      val numTests : Int  = 7
      val addr            = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, addr_len).toInt-1).U(addr_len.W)}
      val memdata         = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      val writedata       = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      
      
      for (i <- 0 until numTests) {
        if(i % 3 == 0){                               //test idling the cache controller when cpu doesn't want anything
          dut.io.cpuin.valid.poke(false.B)
          dut.clock.step(4)
          dut.io.cpuout.busy.expect(false.B)
        }

        dut.io.cpuin.addr.poke(addr(i))
        dut.io.cpuin.valid.poke(true.B)
        dut.io.cpuin.rw.poke(true.B)
        dut.io.cpuin.data.poke(writedata(i))

        dut.io.memin.data.poke(0.U)
        dut.io.memin.valid.poke(false.B)
        dut.io.memin.ready.poke(true.B)

        dut.clock.step(1)

        while (dut.io.cpuout.busy.peek().litToBoolean && !(dut.io.cpuout.valid.peek().litToBoolean)) {
          dut.io.memout.req.expect(false.B)                   //since we should not write back no requests should be made
          
          dut.clock.step(1)
        }

        dut.io.cpuout.data.expect(writedata(i))               //data should be in cache now
        dut.io.cpuout.valid.expect(true.B)
        dut.io.cpuout.busy.expect(true.B)
        dut.io.cpuout.hit.expect(true.B)                      // no writebacks so hit everytime

        dut.io.memout.addr.expect(addr(i))                    //address should be in cache now
        dut.io.memout.rw.expect(true.B)
        dut.io.memout.data.expect(writedata(i)) 
      }
    }
  }






  
it should "test Writing twice to same address with same tag, aka we should never write-back in same i cycle" in {
    test(new CacheController(size, addr_len, data_len, debug)).withAnnotations (Seq( WriteVcdAnnotation )) {dut =>//. withAnnotations (Seq( WriteVcdAnnotation ))
      val randgen         = new Random(777)
      val numTests : Int  = 7                             //this setup does not produce duplicates "natutrally" so testing is easily controllable
      val addr            = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, addr_len).toInt-1).U(addr_len.W)}
      val memdata         = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      val writedata       = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      
      
      for (i <- 0 until numTests) {
        if(i % 3 == 0){                             //test idling the cache controller when cpu doesn't want anything
          dut.io.cpuin.valid.poke(false.B)
          dut.clock.step(4)
          dut.io.cpuout.busy.expect(false.B)
        }
        dut.io.cpuin.addr.poke(addr(i))
        dut.io.cpuin.valid.poke(true.B)
        dut.io.cpuin.rw.poke(true.B)
        dut.io.cpuin.data.poke(writedata(i))

        dut.io.memin.data.poke(0.U)
        dut.io.memin.valid.poke(false.B)
        dut.io.memin.ready.poke(true.B)

        dut.clock.step(1)

        while (dut.io.cpuout.busy.peek().litToBoolean && !(dut.io.cpuout.valid.peek().litToBoolean)) {
          dut.io.memout.req.expect(false.B)                   //since we should not write back no requests should be made
          
          dut.clock.step(1)
        }

        dut.io.cpuout.data.expect(writedata(i))               //data should be in cache now
        dut.io.cpuout.valid.expect(true.B)
        dut.io.cpuout.busy.expect(true.B)
        dut.io.cpuout.hit.expect(true.B)                      // no writebacks so hit everytime

        dut.io.memout.addr.expect(addr(i))                    //address should be in cache now
        dut.io.memout.rw.expect(true.B)
        dut.io.memout.data.expect(writedata(i)) 

        dut.clock.step(1)


        dut.io.cpuin.data.poke(memdata(i))                  //write different data so cache write can be confirmed

        dut.clock.step(1)

        

        while (dut.io.cpuout.busy.peek().litToBoolean && !(dut.io.cpuout.valid.peek().litToBoolean)) {
          dut.io.memout.req.expect(false.B)                   //since we should not write back no requests should be made
          
          dut.clock.step(1)
        }

        dut.io.cpuout.data.expect(memdata(i))               //data should be in cache now
        dut.io.cpuout.valid.expect(true.B)
        dut.io.cpuout.busy.expect(true.B)
        dut.io.cpuout.hit.expect(true.B)                      // no writebacks so hit everytime

        dut.io.memout.addr.expect(addr(i))                    //address should be in cache now
        dut.io.memout.rw.expect(true.B)
        dut.io.memout.data.expect(memdata(i))    
      }
    }
  }







  it should "test Writing twice to same index but different tag, aka we have to write-back once every i cycle" in {
    test(new CacheController(size, addr_len, data_len, debug)).withAnnotations (Seq( WriteVcdAnnotation )) {dut =>//. withAnnotations (Seq( WriteVcdAnnotation ))
      val randgen         = new Random(777)
      val numTests : Int  = 7


      val addr            = Seq.tabulate(numTests){i=>(i + 16).toInt.U(addr_len.W)}
      val altaddr         = Seq.tabulate(numTests){i=>(i + 32).toInt.U(addr_len.W)}



      val memdata         = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      val writedata       = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      
      
      for (i <- 0 until numTests) {
        if(i % 3 == 0){                             //test idling the cache controller when cpu doesn't want anything

          dut.io.cpuin.valid.poke(false.B)
          dut.clock.step(4)
          dut.io.cpuout.busy.expect(false.B)
        }
        dut.io.cpuin.addr.poke(addr(i))
        dut.io.cpuin.valid.poke(true.B)
        dut.io.cpuin.rw.poke(true.B)
        dut.io.cpuin.data.poke(writedata(i))

        dut.io.memin.data.poke(0.U)
        dut.io.memin.valid.poke(false.B)
        dut.io.memin.ready.poke(true.B)

        dut.clock.step(1)

        while (dut.io.cpuout.busy.peek().litToBoolean && !(dut.io.cpuout.valid.peek().litToBoolean)) {
          dut.io.memout.req.expect(false.B)                   //since we should not write back no requests should be made
          
          dut.clock.step(1)
        }

        dut.io.cpuout.data.expect(writedata(i))               //data should be in cache now
        dut.io.cpuout.valid.expect(true.B)
        dut.io.cpuout.busy.expect(true.B)
        dut.io.cpuout.hit.expect(true.B)                      // no writebacks so hit everytime

        dut.io.memout.addr.expect(addr(i))                    //address should be in cache now
        dut.io.memout.rw.expect(true.B)
        dut.io.memout.data.expect(writedata(i)) 



        dut.io.cpuin.addr.poke(altaddr(i))
        dut.io.cpuin.valid.poke(true.B)
        dut.io.cpuin.rw.poke(true.B)
        dut.io.cpuin.data.poke(memdata(i))                  //write different data so cache write can be confirmed

        dut.io.memin.data.poke(writedata(i))                //writedata will be written to memory so it should be returned
        dut.io.memin.valid.poke(false.B)
        dut.io.memin.ready.poke(true.B)

        dut.clock.step(1)

        
        while (dut.io.cpuout.busy.peek().litToBoolean && !(dut.io.cpuout.valid.peek().litToBoolean)) {

          if(dut.io.memout.req.peek().litToBoolean){ 
            dut.io.memout.addr.expect(addr(i))
            dut.io.memout.req.expect(true.B)
            dut.io.memout.rw.expect(true.B)
            dut.io.memout.data.expect(writedata(i))

            dut.io.memin.ready.poke(false.B)
            dut.clock.step(4)
            dut.io.memin.valid.poke(true.B)
            dut.io.memin.ready.poke(true.B)
          }
          dut.clock.step(1)
        }

        dut.io.cpuout.data.expect(memdata(i))               //new data should be in cache now
        dut.io.cpuout.valid.expect(true.B)
        dut.io.cpuout.busy.expect(true.B)
        dut.io.cpuout.hit.expect(false.B)                      // allways writeback so no hits

        dut.io.memout.addr.expect(altaddr(i))                  //new address should be in cache now
        dut.io.memout.rw.expect(true.B)
        dut.io.memout.data.expect(memdata(i)) 
      }
    }
  }






  
  //this test is only for viewing the operation from VCD file and checking that the device does not get stuck
  //since testing everything in one test would require coding the whole cache conrtoller logic into the test
  it should "test Mixed R/W" in {
    test(new CacheController(size, addr_len, data_len, debug)).withAnnotations (Seq( WriteVcdAnnotation )) {dut =>
      val numTests : Int  = 1000
      val addr            = Seq.tabulate(numTests){i=>scala.util.Random.nextInt(math.pow(2, addr_len).toInt-1).U(addr_len.W)}
      val memdata         = Seq.tabulate(numTests){i=>scala.util.Random.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      val writedata       = Seq.tabulate(numTests){i=>scala.util.Random.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      val rw              = Seq.tabulate(numTests){i=>scala.util.Random.nextBoolean().B}          // this determines r or w
      
      
      for (i <- 0 until numTests) {
        if(i % 100 == 0){                             //test idling the cache controller when cpu doesn't want anything
          dut.io.cpuin.valid.poke(false.B)
          dut.clock.step(4)
          dut.io.cpuout.busy.expect(false.B)
        }
        dut.io.cpuin.addr.poke(addr(i))
        dut.io.cpuin.valid.poke(true.B)
        dut.io.cpuin.rw.poke(rw(i))
        dut.io.cpuin.data.poke(memdata(i))

        dut.io.memin.data.poke(writedata(i))
        dut.io.memin.valid.poke(false.B)
        dut.io.memin.ready.poke(true.B)
        dut.clock.step(1)

      
        while (dut.io.cpuout.busy.peek().litToBoolean && !(dut.io.cpuout.hit.peek().litToBoolean)) {
          if(dut.io.memout.req.peek().litToBoolean){
            dut.io.memin.ready.poke(false.B)
            dut.clock.step(4)
            dut.io.memin.valid.poke(true.B)
            dut.io.memin.ready.poke(true.B)
          }
          dut.clock.step(1)
        }
      }
    }
  }
}
