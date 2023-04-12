

package cc

import chisel3._
import chisel3.util._
import chisel3.experimental._
//import chisel3.util.Decoupled

object State extends ChiselEnum {                                   //enumeration for the finite state machine
    val idle, compare, write, allocate = Value
}
import State._


class memField(val dl: Int, val tl: Int) extends Bundle {           //custom bundle for all the field stored in memory 
  val data  = UInt(dl.W)
  val tag   = UInt(tl.W)
  val valid = Bool()
}

class dividedAddress(val mal: Int, val tl: Int) extends Bundle {     //custom bundle for address with tag and mem address
    val memadr  = UInt(mal.W)
    val tag     = UInt(tl.W)
}



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


    val memf = Wire(new memField(data_len,(addr_len - size)))
    val daddr = Wire(new dividedAddress(size, (addr_len - size)))
    //val vecad = Wire(Vec(addr_len, UInt(1.W)))


    val len = (data_len + (addr_len - size) + 1).toInt

    val state   = RegInit(State.idle)

    val cache = Module(new Cache(size, addr_len, data_len))



    //vecad := cpuin.addr
    daddr.memadr := cpuin.addr((size - 1), 0)
    daddr.tag := cpuin.addr((addr_len-1), size)





    //bellow unupdated code for this new way of handling the address and memoryfield








    switch (state) {
        is (idle) {
            //cpuout.busy := false.B //might need to move this to the last step istead i.e compare step I think
            when(cpuout.hit) {
                cpuout.busy := false.B
                //cpuout.valid := false.B
                //cpuout.hit := false.B
            }
            when(cpuin.valid) {
                cpuout.busy := true.B
                cpuout.valid := false.B
                //cpuout.hit := false.B
                state := State.compare
            }
        }
         is (compare) {
            cache.io.addr := cpuin.addr
            if(cache.io.dataout.apply(0).litToBoolean) {        // if(cache.io.dataout.tail(len-1).asBool().litToBoolean) {
                if((cpuin.addr(size, addr_len) === cache.io.dataout(data_len, (len - 1))).litToBoolean) {        //check if the tag is same in memory and cpu addr  this may have issue with LSB MSB type thing
                    cpuout.data := cache.io.dataout(0, data_len)
                    cpuout.valid := true.B
                    cpuout.hit := true.B
                    state := State.idle
                }
                else {
                    cache.io.we := true.B
                    state := State.allocate
                }

            }
            else {
                cache.io.we := true.B
                state := allocate
            }

        }
        // is (write) {}
         is (allocate){
            when(memin.ready) {
                memout.req := true.B
                memout.addr := cpuin.addr                                           //might want to check that this is still valid
                memout.rw := cpuin.rw
                //when(cpuin.rw) {}
                memout.data := cpuin.data

            }
            when(memin.valid) {
                cache.io.datain := memin.data
                memout.req := false.B
                state := State.compare                                                // there might be some issue due to delay but should not be
            }
        }
    }

}