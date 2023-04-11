

package cc

import chisel3._
//import chisel3.util.Decoupled

object State extends ChiselEnum {
    val idle, compare, write, allocate = Value
}
import State._

class CcCPUInputBundle(val addr_len: Int, val data_len: Int) extends Bundle{
    val addr    = UInt(addr_len.W)
    val valid   = UInt(Bool())
    val rw      = UInt(Bool())
    val data    = UInt(data_len.W)
}

class CcMemoryInputBundle(val data_len: Int) extends Bundle{
    val data    = UInt(data_len.W)
    val valid   = UInt(Bool())
    val ready   = UInt(Bool())
}

class CcCPUOutputBundle(val data_len: Int) extends Bundle{
    val data    = UInt(data_len.W)
    val valid   = UInt(Bool())
    val busy    = UInt(Bool())
    val hit     = UInt(Bool())    
}

class CcMemoryOutputBundle(val addr_len: Int, val data_len: Int) extends Bundle{
    val addr    = UInt(addr_len.W)
    val req     = UInt(Bool())
    val rw      = UInt(Bool())
    val data    = UInt(data_len.W)
}


class CacheController(size: Int, addr_len: Int, data_len: Int) extends Module{
    require(addr_len >= 0)
    require(data_len >= 0)

    val cpuin   = IO(new CcCPUInputBundle(addr_len, data_len))
    val cpuout  = IO(new CcCPUOutputBundle(data_len))
    val memin   = IO(new CcMemoryInputBundle(data_len))
    val memout  = IO(new CcMemoryOutputBundle(addr_len, data_len))

    val len = (data_len + (addr_len - size) + 1).Int

    val state   = RegInit(idle)

    val cache = new Cache(size, addr_len, len)

    state match {
        case idle => {
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
        case compare => {
            cache.io.addr := cpuin.addr
            if(cache.io.dataout.tail) {
                if(cpuin.addr(size until addr_len) === cache.io.dataout(data_len until (len - 1))) {        //check if the tag is same in memory and cpu addr  this may have issue with LSB MSB type thing
                    cpuout.data := cache.io.dataout(0 until data_len)
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
        case allocate => {
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