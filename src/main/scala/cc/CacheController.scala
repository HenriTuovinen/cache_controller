

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



    def counter(max: UInt) = {
        val x = RegInit(0.asUInt(max.getWidth.W))
        x := Mux(x === max, 0.U, x + 1.U)
        x
    }

    def pulse(n: UInt) = counter(n - 1.U) === 0.U

    def toggle(p: Bool) = {
        val x = RegInit(false.B)
        x := Mux(p, !x, x)
        x
    }

    def squareWave(period: UInt) = toggle(pulse(period >> 1))

    val sq = squareWave(2.U)

    //vecad := io.cpuin.addr
    //daddr.memadr := io.cpuin.addr((size - 1), 0)
    //daddr.tag := io.cpuin.addr((addr_len-1), size)

    //cache.io.addr := daddr



    io.cpuout.data      := cache.io.dataout
    io.cpuout.valid     := valid
    io.cpuout.busy      := busy
    io.cpuout.hit       := hit

    io.memout.addr      := io.cpuin.addr
    io.memout.req       := req
    io.memout.rw        := io.cpuin.rw
    io.memout.data      := io.cpuin.data

    cache.io.addr       := io.cpuin.addr(size-1, 0)
    cache.io.tag        := io.cpuin.addr(addr_len-1, size)
    cache.io.datain     := io.memin.data
    cache.io.we         := we

    when (sq) {
        when (state === idle) {
                we := false.B
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
        .elsewhen (state === compare) {
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
        .elsewhen (state === allocate){
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
                req := false.B                                              // there might be some issue due to delay but should not be
            }
            when(io.cpuout.data === io.memin.data){
                we := false.B
                state := State.compare
            }
        }
    }          
    

}

/*
switch (state) {
        is (idle) {
            println("######################## iddling")
            we := false.B
            when(hit) {
                println("######################## hit clear")
                busy := false.B
                //io.cpuout.valid := false.B
                hit := false.B
            }
            when(io.cpuin.valid) {                     //this might happen too soon or not not sure
                println("######################## cpu valid")
                busy := true.B
                valid := false.B
                //io.cpuout.hit := false.B
                state := State.compare
            }
        }
        is (compare) {
            println("######################## comparison")
            when(cache.io.valid) {        // if(cache.io.dataout.tail(len-1).asBool().litToBoolean) {
                when(io.cpuin.addr(addr_len-1, size) === cache.io.tagout) {        //check if the tag is same in memory and cpu addr  this may have issue with LSB MSB type thing
                    //io.cpuout.data := cache.io.dataout
                    valid := true.B
                    hit := true.B
                    state := State.idle
                    println("######################## its a certified hood classic")
                }
                .otherwise {
                    println("######################## wrong tag in memory")
                    we := true.B
                    state := State.allocate
                }

            }
            .otherwise {
                println("######################## not valid data in memory")
                we := true.B
                state := allocate
            }

        }
        // is (write) {}
        is (allocate){
            when(io.memin.ready) {
                req := true.B
                println("######################## mem ready")
                //io.memout.addr := io.cpuin.addr                                           //might want to check that this is still valid
                //io.memout.rw := io.cpuin.rw
                //when(io.cpuin.rw) {}
                //io.memout.data := io.cpuin.data

            }
            when(io.memin.valid) {
                println("######################## mem valid")
                //cache.io.datain := io.memin.data
                //cache.io.tag    := io.cpuin.addr(addr_len-1, size)
                req := false.B                                              // there might be some issue due to delay but should not be
            }
            when(io.cpuout.data === io.memin.data){
                println("######################## write complete")
                we := false.B
                state := State.compare
            }
        }
    }
*/