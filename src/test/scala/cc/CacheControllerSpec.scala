

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








  it should "test Reading" in {
    test(new CacheController(size, addr_len, data_len)).withAnnotations (Seq( WriteVcdAnnotation )) {dut =>//. withAnnotations (Seq( WriteVcdAnnotation ))
      val randgen         = new Random(777)         //remove when operatoin is correct
      val numTests : Int  = 10                      //increase when operation is correct
      //val addr = Seq.tabulate(numTests){i=>scala.util.Random.nextInt(math.pow(2, addr_len).toInt-1).U(addr_len.W)}
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

        //hit can not be used here since it is not always true, valid however should be
        while (dut.io.cpuout.busy.peek().litToBoolean && !(dut.io.cpuout.valid.peek().litToBoolean)) {  //step clock while cc is busy
          if(dut.io.memout.req.peek().litToBoolean){
            dut.io.memout.addr.expect(addr(i))
            dut.io.memout.req.expect(true.B)
            dut.io.memout.rw.expect(false.B)
            //dut.io.memout.data.expect(0.U)      //meaningless at this time

            dut.io.memin.ready.poke(false.B)
            dut.clock.step(4)
            dut.io.memin.valid.poke(true.B)
            dut.io.memin.ready.poke(true.B)
            //dut.io.memout.data.expect(7.U) //this fails as it should
          }
          dut.clock.step(1)
        }

        dut.io.cpuout.data.expect(memdata(i))
        dut.io.cpuout.valid.expect(true.B)
        dut.io.cpuout.busy.expect(true.B)
        dut.io.cpuout.hit.expect(false.B)             // the tags never match so no hits

      }

      dut.clock.step(1)                 //This is probably not nessesary
    }
  }













  it should "test Reading twice in a row from same address, aka testing hit" in {
    test(new CacheController(size, addr_len, data_len)).withAnnotations (Seq( WriteVcdAnnotation )) {dut =>//. withAnnotations (Seq( WriteVcdAnnotation ))
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





        dut.io.memin.data.poke(0.U)                   //this is intentionally "wrong" so reading from memory results in error
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
    test(new CacheController(size, addr_len, data_len)).withAnnotations (Seq( WriteVcdAnnotation )) {dut =>//. withAnnotations (Seq( WriteVcdAnnotation ))
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
    test(new CacheController(size, addr_len, data_len)).withAnnotations (Seq( WriteVcdAnnotation )) {dut =>//. withAnnotations (Seq( WriteVcdAnnotation ))
      val randgen         = new Random(777)
      val numTests : Int  = 7                             //this setup does not produce duplicates "natutrally" so testing is easily controllable
      val addr            = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, addr_len).toInt-1).U(addr_len.W)}
      val memdata         = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      val writedata       = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      
      
      for (i <- 0 until numTests) {
        if(i % 3 == 0){                             //test idling the cache controller when cpu doesn't want anything

          dut.io.cpuin.valid.poke(false.B)
          dut.clock.step()
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

        //dut.clock.step(1)                              // should think better logic for the above so this is not nessesary

        dut.io.cpuout.data.expect(memdata(i))               //data should be in cache now
        dut.io.cpuout.valid.expect(true.B)
        dut.io.cpuout.busy.expect(true.B)
        dut.io.cpuout.hit.expect(true.B)                      // no writebacks so hit everytime

        dut.io.memout.addr.expect(addr(i))                    //address should be in cache now
        dut.io.memout.rw.expect(true.B)
        dut.io.memout.data.expect(memdata(i)) 

        //dut.clock.step(1)     
      }
    }
  }











  it should "test Writing twice to same index but different tag, aka we have to write-back once every i cycle" in {
    test(new CacheController(size, addr_len, data_len)).withAnnotations (Seq( WriteVcdAnnotation )) {dut =>//. withAnnotations (Seq( WriteVcdAnnotation ))
      val randgen         = new Random(777)
      val numTests : Int  = 7


      val addr            = Seq.tabulate(numTests){i=>(i + 16).toInt.U(addr_len.W)}
      val altaddr         = Seq.tabulate(numTests){i=>(i + 32).toInt.U(addr_len.W)}



      val memdata         = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      val writedata       = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      
      
      for (i <- 0 until numTests) {
        if(i % 3 == 0){                             //test idling the cache controller when cpu doesn't want anything

          dut.io.cpuin.valid.poke(false.B)
          dut.clock.step()
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

          if(dut.io.memout.req.peek().litToBoolean){          //this seems meaningless maybe should rethink
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
        dut.io.cpuout.hit.expect(false.B)                      // no writebacks so hit everytime

        dut.io.memout.addr.expect(altaddr(i))                  //new address should be in cache now
        dut.io.memout.rw.expect(true.B)
        dut.io.memout.data.expect(memdata(i)) 
      }
    }
  }





















/*

  it should "test Mixed R/W" in { //not done yet
    test(new CacheController(size, addr_len, data_len)).withAnnotations (Seq( WriteVcdAnnotation )) {dut =>//. withAnnotations (Seq( WriteVcdAnnotation ))
      val randgen         = new Random(777)
      val numTests : Int  = 10
      val addr            = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, addr_len).toInt-1).U(addr_len.W)}
      val memdata         = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      val writedata       = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      val rw              = Seq.tabulate(numTests){i=>randgen.nextBoolean().B}          // this determines r or w
      
      
      for (i <- 0 until numTests) {
        dut.io.cpuin.addr.poke(addr(i))
        dut.io.cpuin.valid.poke(true.B)
        dut.io.cpuin.rw.poke(rw(i))
        dut.io.cpuin.data.poke(0.U(data_len.W)) //if (rw(i).litToBoolean) writedata(i) else memdata(i) 

        dut.io.memin.data.poke(0.U)
        dut.io.memin.valid.poke(false.B)
        dut.io.memin.ready.poke(true.B)
        dut.clock.step(1)

        

        
        //if(true.B.litToBoolean){
        
        //if(dut.io.memout.req.litToBoolean){
        while (dut.io.cpuout.busy.peek().litToBoolean && !(dut.io.cpuout.hit.peek().litToBoolean)) {
          if(dut.io.memout.req.peek().litToBoolean){
            dut.io.memin.ready.poke(false.B)

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


        dut.io.cpuout.data.expect(memdata(i))
        //dut.io.cpuout.valid.expect(true.B)
        //dut.io.cpuout.busy.expect(true.B)
        //dut.io.cpuout.hit.expect(true.B)

        dut.io.memout.addr.expect(addr(i))
        //dut.io.memout.req.expect(false.B)
        //dut.io.memout.rw.expect(false.B)
        //dut.io.memout.data.expect(0.U)

        dut.clock.step(1)
      }
    }
  }
*/

  










//this is for testing holding of the data but this seems hard to implement since index can be over written
/*
it should "test holding of data" in { //not done yet
    test(new CacheController(size, addr_len, data_len)).withAnnotations (Seq( WriteVcdAnnotation )) {dut =>//. withAnnotations (Seq( WriteVcdAnnotation ))
      val randgen         = new Random(777)
      val numTests : Int  = 10
      val addr            = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, addr_len).toInt-1).U(addr_len.W)}
      val memdata         = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      val writedata       = Seq.tabulate(numTests){i=>randgen.nextInt(math.pow(2, data_len).toInt-1).U(data_len.W)}
      val rw              = Seq.tabulate(numTests){i=>randgen.nextBoolean().B}          // this determines r or w
      
      
      for (i <- 0 until numTests) {
        dut.io.cpuin.addr.poke(addr(i))
        dut.io.cpuin.valid.poke(true.B)
        dut.io.cpuin.rw.poke(rw(i))
        dut.io.cpuin.data.poke(0.U(data_len.W)) //if (rw(i).litToBoolean) writedata(i) else memdata(i) 

        dut.io.memin.data.poke(0.U)
        dut.io.memin.valid.poke(false.B)
        dut.io.memin.ready.poke(true.B)
        dut.clock.step(1)

        

        
        //if(true.B.litToBoolean){
        
        //if(dut.io.memout.req.litToBoolean){
        while (dut.io.cpuout.busy.peek().litToBoolean && !(dut.io.cpuout.hit.peek().litToBoolean)) {
          if(dut.io.memout.req.peek().litToBoolean){
            dut.io.memin.ready.poke(false.B)

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
  */





}
