

package cc

import chisel3._
import chisel3.util._
import chisel3.experimental._
//import chisel3.util.Decoupled

object State extends ChiselEnum {
    val idle, compare, write, allocate = Value
}
import State._

class CcCPUInputBundle(val addr_len: Int, val data_len: Int) extends Bundle{
    val addr    = UInt(addr_len.W)
    val valid   = Bool()
    val rw      = Bool()
    val data    = UInt(data_len.W)
}

class CcMemoryInputBundle(val data_len: Int) extends Bundle{
    val data    = UInt(data_len.W)
    val valid   = Bool()
    val ready   = Bool()
}

class CcCPUOutputBundle(val data_len: Int) extends Bundle{
    val data    = UInt(data_len.W)
    val valid   = Bool()
    val busy    = Bool()
    val hit     = Bool()    
}

class CcMemoryOutputBundle(val addr_len: Int, val data_len: Int) extends Bundle{
    val addr    = UInt(addr_len.W)
    val req     = Bool()
    val rw      = Bool()
    val data    = UInt(data_len.W)
}


class CacheController(size: Int, addr_len: Int, data_len: Int) extends Module{
    require(addr_len >= 0)
    require(data_len >= 0)

    val cpuin   = IO(new CcCPUInputBundle(addr_len, data_len))
    val cpuout  = IO(new CcCPUOutputBundle(data_len))
    val memin   = IO(new CcMemoryInputBundle(data_len))
    val memout  = IO(new CcMemoryOutputBundle(addr_len, data_len))

    val len = (data_len + (addr_len - size) + 1).toInt

    val state   = RegInit(idle)

    val cache = new Cache(size, addr_len, len)

    state match {
        case State.idle => {
            //cpuout.busy := false.B //might need to move this to the last step istead i.e compare step I think
            when(cpuout.hit) {
                cpuout.busy := false.B
                //cpuout.valid := false.B
                cpuout.hit := false.B
            }
            when(cpuin.valid) {
                cpuout.busy := true.B
                cpuout.valid := false.B
                //cpuout.hit := false.B
                state := compare
            }
        }
        case State.compare => {
            cache.io.addr := cpuin.addr
            if(cache.io.dataout.tail(len-1).asBool().litToBoolean) {
                if((cpuin.addr(size, addr_len) === cache.io.dataout(data_len, (len - 1))).litToBoolean) {        //check if the tag is same in memory and cpu addr  this may have issue with LSB MSB type thing
                    cpuout.data := cache.io.dataout(0, data_len)
                    cpuout.valid := true.B
                    cpuout.hit := true.B
                    state := idle
                }
                else {
                    cache.io.we := true.B
                    state := allocate
                }

            }
            else {
                cache.io.we := true.B
                state := allocate
            }

        }
        //case write => {}
        case State.allocate => {
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
                state := compare                                                // there might be some issue due to delay but should not be
            }
        }
    }

}