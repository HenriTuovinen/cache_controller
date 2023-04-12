

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

    val cpuin   = IO(new CcCPUInputBundle(addr_len, data_len))
    val cpuout  = IO(new CcCPUOutputBundle(data_len))
    val memin   = IO(new CcMemoryInputBundle(data_len))
    val memout  = IO(new CcMemoryOutputBundle(addr_len, data_len))


    //val memf = Wire(new memField(data_len,(addr_len - size)))
    //val daddr = Wire(new dividedAddress(size, (addr_len - size)))
    val vecad = Wire(UInt(addr_len.W))


    //val len = (data_len + (addr_len - size) + 1).toInt

    val state   = RegInit(State.idle)
    val valid   = RegInit(false.B)
    val busy    = RegInit(false.B)
    val hit     = RegInit(false.B)

    val req     = RegInit(false.B)
    //val 

    val we      = RegInit(false.B)

    val cache = Module(new Cache(size, addr_len - size, data_len))



    vecad := cpuin.addr
    //daddr.memadr := cpuin.addr((size - 1), 0)
    //daddr.tag := cpuin.addr((addr_len-1), size)

    //cache.io.addr := daddr



    cpuout.data     := cache.io.dataout
    cpuout.valid    := valid
    cpuout.busy     := busy
    cpuout.hit      := hit

    memout.addr     := cpuin.addr
    memout.req      := req
    memout.rw       := cpuin.rw
    memout.data     := cpuin.data

    cache.io.addr   := cpuin.addr(size-1, 0)
    cache.io.tag    := cpuin.addr(addr_len-1, size)
    cache.io.datain := memin.data
    cache.io.we     := we
                

    switch (state) {
        is (idle) {
            when(hit) {
                busy := false.B
                //cpuout.valid := false.B
                hit := false.B
            }
            when(cpuin.valid) {                     //this might happen too soon or not not sure
                busy := true.B
                valid := false.B
                //cpuout.hit := false.B
                state := State.compare
            }
        }
         is (compare) {
            when(cache.io.valid) {        // if(cache.io.dataout.tail(len-1).asBool().litToBoolean) {
                when(cpuin.addr(addr_len-1, size) === cache.io.tagout) {        //check if the tag is same in memory and cpu addr  this may have issue with LSB MSB type thing
                    //cpuout.data := cache.io.dataout
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
            when(memin.ready) {
                req := true.B
                //memout.addr := cpuin.addr                                           //might want to check that this is still valid
                //memout.rw := cpuin.rw
                //when(cpuin.rw) {}
                //memout.data := cpuin.data

            }
            when(memin.valid) {
                //cache.io.datain := memin.data
                //cache.io.tag    := cpuin.addr(addr_len-1, size)
                req := false.B
                state := State.compare                                                // there might be some issue due to delay but should not be
            }
        }
    }

}