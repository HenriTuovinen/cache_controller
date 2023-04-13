

package cc

import chisel3._
import chisel3.util._
import chisel3.experimental._
//import chisel3.util.Decoupled

object State extends ChiselEnum{                                   //enumeration for the finite state machine
    val idle, compare, write, allocate = Value
}
import State._



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


class CacheController(size: Int, addr_len: Int, data_len: Int) extends Module{
    require(addr_len >= 0)
    require(data_len >= 0)
    val io = IO(new Bundle {
        val cpuin   = new CcCPUInputBundle(addr_len, data_len)
        val cpuout  = new CcCPUOutputBundle(data_len)
        val memin   = new CcMemoryInputBundle(data_len)
        val memout  = new CcMemoryOutputBundle(addr_len, data_len)
    })


    /*
    val io.cpuin   = IO(new Ccio.CPUInputBundle(addr_len, data_len))
    val io.cpuout  = IO(new Ccio.CPUOutputBundle(data_len))
    val io.memin   = IO(new CcMemoryInputBundle(data_len))
    val io.memout  = IO(new CcMemoryOutputBundle(addr_len, data_len))
    */

    //val memf = Wire(new memField(data_len,(addr_len - size)))
    //val daddr = Wire(new dividedAddress(size, (addr_len - size)))
    
    
    
    
    //val vecad = Wire(UInt(addr_len.W))


    //val len = (data_len + (addr_len - size) + 1).toInt

    val state   = RegInit(State.idle)
    val valid   = RegInit(false.B)
    val busy    = RegInit(false.B)
    val hit     = RegInit(false.B)

    val req     = RegInit(false.B)
    //val 

    val we      = RegInit(false.B)

    val cache = Module(new Cache(size, addr_len - size, data_len))



    //vecad := io.cpuin.addr
    //daddr.memadr := io.cpuin.addr((size - 1), 0)
    //daddr.tag := io.cpuin.addr((addr_len-1), size)

    //cache.io.addr := daddr



    io.cpuout.data     := cache.io.dataout
    io.cpuout.valid    := valid
    io.cpuout.busy     := busy
    io.cpuout.hit      := hit

    io.memout.addr     := io.cpuin.addr
    io.memout.req      := req
    io.memout.rw       := io.cpuin.rw
    io.memout.data     := io.cpuin.data

    cache.io.addr   := io.cpuin.addr(size-1, 0)
    cache.io.tag    := io.cpuin.addr(addr_len-1, size)
    cache.io.datain := io.memin.data
    cache.io.we     := we
                

    switch (state) {
        is (idle) {
            when(hit) {
                busy := false.B
                //io.cpuout.valid := false.B
                hit := false.B
            }
            when(io.cpuin.valid) {                     //this might happen too soon or not not sure
                busy := true.B
                valid := false.B
                //io.cpuout.hit := false.B
                state := State.compare
            }
        }
         is (compare) {
            when(cache.io.valid) {        // if(cache.io.dataout.tail(len-1).asBool().litToBoolean) {
                when(io.cpuin.addr(addr_len-1, size) === cache.io.tagout) {        //check if the tag is same in memory and cpu addr  this may have issue with LSB MSB type thing
                    //io.cpuout.data := cache.io.dataout
                    valid := true.B
                    hit := true.B
                    state := State.idle
                }
                .otherwise {
                    we := true.B
                    state := State.allocate
                }

            }
            .otherwise {
                we := true.B
                state := allocate
            }

        }
        // is (write) {}
         is (allocate){
            when(io.memin.ready) {
                req := true.B
                //io.memout.addr := io.cpuin.addr                                           //might want to check that this is still valid
                //io.memout.rw := io.cpuin.rw
                //when(io.cpuin.rw) {}
                //io.memout.data := io.cpuin.data

            }
            when(io.memin.valid) {
                //cache.io.datain := io.memin.data
                //cache.io.tag    := io.cpuin.addr(addr_len-1, size)
                req := false.B
                state := State.compare                                                // there might be some issue due to delay but should not be
            }
        }
    }

}