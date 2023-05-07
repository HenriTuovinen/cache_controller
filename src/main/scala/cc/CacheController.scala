

package cc

import chisel3._
import chisel3.util._
import chisel3.experimental._
//import chisel3.util.Decoupled

object State extends ChiselEnum{                                   //enumeration for the finite state machine
    val idle, compare, write, allocate = Value
}
import State._

//hit signal should only go high when we get a cache hit straigth away, not when the data is read from memory

class CcCPUInputBundle(val addr_len: Int, val data_len: Int) extends Bundle{                // custom bundles for the ios
    val addr    = Input(UInt(addr_len.W))
    val valid   = Input(Bool())
    val rw      = Input(Bool())
    val data    = Input(UInt(data_len.W))
}

class CcMemoryInputBundle(val data_len: Int) extends Bundle{
    val data    = Input(UInt(data_len.W))
    val valid   = Input(Bool())
    val ready   = Input(Bool())
}

class CcCPUOutputBundle(val data_len: Int) extends Bundle{
    val data    = Output(UInt(data_len.W))
    val valid   = Output(Bool())
    val busy    = Output(Bool())
    val hit     = Output(Bool()) 
}

class CcMemoryOutputBundle(val addr_len: Int, val data_len: Int) extends Bundle{
    val addr    = Output(UInt(addr_len.W))
    val req     = Output(Bool())
    val rw      = Output(Bool())
    val data    = Output(UInt(data_len.W))
}



class CacheController(size: Int, addr_len: Int = 32, data_len: Int = 32) extends Module{
    require(size >= 0)
    require(addr_len >= 0)
    require(data_len >= 0)
    val io = IO(new Bundle {
        val cpuin   = new CcCPUInputBundle(addr_len, data_len)
        val cpuout  = new CcCPUOutputBundle(data_len)
        val memin   = new CcMemoryInputBundle(data_len)
        val memout  = new CcMemoryOutputBundle(addr_len, data_len)
    })



    val state   = RegInit(State.idle)           //state of the FSM
    val valid   = RegInit(false.B)              //registers to keep the output values
    val busy    = RegInit(false.B)
    val hit     = RegInit(false.B)
    val req     = RegInit(false.B)
    val we      = RegInit(false.B)


    val cache = Module(new Cache(size, addr_len - size, data_len, true.B)) //instantiating the cache



    val outregdata = RegInit(0.U(data_len.W))               //keep data that is written to memory in register so cpu can continue while memory is written
    val outregaddr = RegInit(0.U(addr_len.W))           //the address needs to be in register aswell since cache out addr will change with write

    val waitOneCyc = RegInit(false.B)                   //register for waiting one cycle for cache data to become readable


    io.cpuout.data      := cache.io.dataout
    io.cpuout.valid     := valid
    io.cpuout.busy      := busy 
    io.cpuout.hit       := hit && valid     //this just ensures that we report hit when the output goes valid since hit value is not valid until that point

    //io.memout.addr      := io.cpuin.addr

    //this can change the addres that goes to memory while we are writing
    //this is propably last thing to do in device
    //##############################################################
    when(io.memin.ready) { //this does not feel right I suspect that this allows writing outregs too early so think trough
        //maybe just do the write back wo registers and wait fir the write to complete
        outregaddr := cache.io.tagout##(io.cpuin.addr(size-1, 0))      //these are assigned here as to not disturb previous write to memory when only cahce is writen
        outregdata := cache.io.dataout

        when(io.cpuin.rw){
            io.memout.addr     := outregaddr
        }
        .otherwise{
            io.memout.addr     := io.cpuin.addr
        }
    } 
    
    io.memout.req       := req
    io.memout.rw        := io.cpuin.rw
    io.memout.data      := outregdata

    cache.io.index      := io.cpuin.addr(size-1, 0)
    cache.io.tag        := io.cpuin.addr(addr_len-1, size)
    when(io.cpuin.rw){
        cache.io.datain     := io.cpuin.data
    }
    .otherwise{
        cache.io.datain     := io.memin.data
    }
    //cache.io.we         := io.memin.valid
    cache.io.we         := we



    //FSM operation loop for cache controller


    when (state === idle) {
        we := false.B
        busy := false.B
        waitOneCyc := false.B

        when(io.cpuin.valid) {  //maybe use something else                   //this might happen too soon or not not sure
            busy := true.B
            valid := false.B
            hit := true.B           //true until proven false 
            //io.cpuout.hit := false.B
            state := compare
        }
    }


    .elsewhen (state === compare) {
        when(cache.io.valid) {        // if(cache.io.dataout.tail(len-1).asBool().litToBoolean) {
            when(io.cpuin.addr(addr_len-1, size) === cache.io.tagout) {        //check if the tag is same in memory and cpu addr  this may have issue with LSB MSB type thing
                when(!io.cpuin.rw){
                    valid := true.B
                    state := idle
                }
                .otherwise{
                    we := true.B
                    state := allocate
                }
            }
            .elsewhen(io.cpuin.rw){                         //we need to write to memory since the tags don't match
                when(io.memin.ready) {
                    outregaddr := cache.io.tagout##(io.cpuin.addr(size-1, 0))      //these are assigned here as to not disturb previous write to memory when only cahce is writen
                    outregdata := cache.io.dataout
                    state := write
                } 
            }
            .otherwise {
                state := allocate
            }
        }
        .otherwise {
            when(io.cpuin.rw){
                we := true.B
            }
            state := allocate
        }
    }


    .elsewhen (state === write) {               //write-back to memory
        hit := false.B              //since we have to write back we don't have direct cache write hit
        req := true.B
        we := true.B
        state := allocate
    }


    .elsewhen (state === allocate){                     //this takes 3 cycles
        when(io.cpuin.rw){                  //just write the data to cache
            we := false.B                   //we will be high for this cycle so next cycle onward it should be low
            req := false.B //this is critical for write
            valid := true.B
            state := idle   
        }

        .otherwise{                      //fetch data from memory
            hit := false.B              //since we need to fetch there was no direct cache hit
            when(io.memin.ready) {      
                req := true.B
            }
            when(io.memin.valid) {    //write data to cache when memory output has valid data //elsewhen was here
                we := true.B
                req := false.B
                when(waitOneCyc){       //writing to cache takes one cycle so we just wait known time instead of doing come comparison since that would waste comparators
                    we := false.B
                    state := compare
                }
                .otherwise{
                    waitOneCyc := true.B
                }
            }    
        }     
    }        
}




/*
            when(waitOneCyc){           //writing to cache takes one cycle so we just wait known time
                //req := false.B //this breaks everything
                we := false.B
                valid := true.B
                state := idle
            }
            .otherwise{
                waitOneCyc := true.B
            }
            */