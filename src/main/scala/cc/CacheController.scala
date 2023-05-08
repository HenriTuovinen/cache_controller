//cache controller with read and write functionality using write back scheme for writing
//the size of the cache is customizable as well as the length of data and length of address

package cc

import chisel3._
import chisel3.util._
import chisel3.experimental._

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



class CacheController(size: Int, addr_len: Int = 32, data_len: Int = 32) extends Module{
    require(size >= 0)                                                          //size means the length of cache addres
    require(addr_len >= 0)                                                      //length of the memory address
    require(data_len >= 0)                                                      //length of data
    val io = IO(new Bundle {
        val cpuin   = new CcCPUInputBundle(addr_len, data_len)
        val cpuout  = new CcCPUOutputBundle(data_len)
        val memin   = new CcMemoryInputBundle(data_len)
        val memout  = new CcMemoryOutputBundle(addr_len, data_len)
    })


    val state   = RegInit(State.idle)                                           //state of the FSM
    val valid   = RegInit(false.B)                                              //registers to keep the output values
    val busy    = RegInit(false.B)
    val hit     = RegInit(false.B)
    val req     = RegInit(false.B)                                              //request operation might need tuning depending on how the memory works
    val we      = RegInit(false.B)

    val waitOneCyc = RegInit(false.B)                                            //register for waiting one cycle for cache data to become readable

    val cache = Module(new Cache(size, addr_len - size, data_len, true.B))      //instantiating the cache

    // wiring cpu out ports
    io.cpuout.data      := cache.io.dataout
    io.cpuout.valid     := valid
    io.cpuout.busy      := busy 
    io.cpuout.hit       := hit && valid                                         //this just ensures that we report hit when the output goes valid since hit value is not valid until that point


    //wiring memory out ports
    when(io.cpuin.rw){                                                          //address given to memoryt depend wether we read or write
        io.memout.addr     := cache.io.tagout##(io.cpuin.addr(size-1, 0))       //address of the current word in cache that will be overwritten
    }
    .otherwise{
        io.memout.addr     := io.cpuin.addr
    }
    io.memout.req       := req
    io.memout.rw        := io.cpuin.rw
    io.memout.data      := cache.io.dataout                                     //since we do write back we only write data that is allready in cache rather than what comes from cpu


    //wiring cache out ports
    cache.io.index      := io.cpuin.addr(size-1, 0)                             //cache is idexed with the first part of the addres that is equal length to the length of the index
    cache.io.tag        := io.cpuin.addr(addr_len-1, size)                      //rest of the address forms the tag
    when(io.cpuin.rw){                                                          //data source depends on read or write
        cache.io.datain := io.cpuin.data
    }
    .otherwise{
        cache.io.datain := io.memin.data
    }
    cache.io.we         := we



    //FSM operation loop for cache controller


    when (state === idle) {
        we := false.B
        busy := false.B
        waitOneCyc := false.B

        when(io.cpuin.valid) {  //we remain idle unti cpu gives valid address //###### maybe use something else                   //this might happen too soon or not not sure
            busy := true.B
            valid := false.B
            hit := true.B                                                       //hit is true until proven false 
            state := compare
        }
    }


    .elsewhen (state === compare) {
        when(cache.io.valid) {                                                  //check is cache empty   
            when(io.cpuin.addr(addr_len-1, size) === cache.io.tagout) {         //check if the tag is same in cache and cpu addr
                when(!io.cpuin.rw){                                             //when reading equal tags means cpu now receives the wanted data
                    valid := true.B
                    state := idle
                }
                .otherwise{                                                     //we just need to overwrite cache since we do only write back
                    we := true.B
                    state := allocate
                }
            }
            .elsewhen(io.cpuin.rw){                                             //we need to write to memory since the tags don't match
                when(io.memin.ready) {
                    state := write
                } 
            }
            .otherwise {                                                        //tags dont mach in read so we need to get data from memory
                state := allocate
            }
        }
        .otherwise {                                                            //cache at given index is empty
            when(io.cpuin.rw){                                                  //we just write to cache on write
                we := true.B
            }
            state := allocate                                                   //with read we fetch data from memory (write also goes to allocate)
        }
    }


    .elsewhen (state === write) {                                               //write-back to memory
        hit := false.B                                                          //since we have to write back we don't have direct cache write hit
        when(io.memin.ready) {
            when(waitOneCyc){                                                   //we wait one cycle so memory write starts and next time it is ready we have completed the write back and continue to overwrite cache
                req := false.B
                we := true.B                                                    //we want to overwrite cache next cycle since memory is now written
                state := allocate
            }
            .otherwise{
                req := true.B                                                   //when memory is ready for the first time we request to write
                waitOneCyc := true.B
            }
        }  
    }


    .elsewhen (state === allocate){                                             //writing cache from cpu or memory depending on operation
        when(io.cpuin.rw){                                                      //just write the data to cache when we are on write
            we := false.B                                                       //WE register will be high for this cycle so next cycle onward it should be low
            valid := true.B                                                     //data to cpu will be valid on next cycle
            state := idle                                                       
        }
        //the operation of this part may need some tuning depending on how the memory controller works
        //I assume that valid and ready might go high and low at the same time
        .otherwise{                                                             //fetch data from memory when we are reading
            
            when(io.memin.ready && hit) {                                       //wait for memory
                hit := false.B                                                  //since we need to fetch there was no direct cache hit we also use this to signal that we have made a request this run of the FSM loop
                req := true.B
            }
            .elsewhen(io.memin.valid) {                                         //write data to cache when memory output has valid data
                req := false.B
                when(waitOneCyc){                                               //writing to cache takes one cycle so we just wait known time instead of doing some comparison since that would waste comparators
                    we := false.B
                    state := compare
                }
                .otherwise{
                    we := true.B
                    waitOneCyc := true.B
                }
            }    
        }     
    }        
}