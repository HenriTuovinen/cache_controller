//simple cache with write enable and customizable size of memory, data in memory and tag in memory

package cc

import chisel3._
import scala.math       //for calculating power of two
import chisel3.util._



class Cache(index_len: Int, tag_len: Int, data_len: Int, debug: Bool = false.B) extends  Module { 
    val io = IO(new Bundle{
        val index   = Input(UInt(index_len.W))  //address to memory
        val tag     = Input(UInt(tag_len.W))    
        val datain  = Input(UInt(data_len.W))
        val we      = Input(Bool())

        val dataout = Output(UInt(data_len.W))
        val tagout  = Output(UInt(tag_len.W))
        val valid   = Output(Bool())
    })
    val wr  = Wire(UInt((data_len + tag_len + 1).W))                                  //wire for routing memory into different outputs
    val mem = Mem(math.pow(2, index_len).toInt, UInt((data_len + tag_len + 1).W))     //creating memory with sizing from length of index as a power of two

    wr :=  mem.read(io.index)

    io.dataout  := wr(data_len + tag_len, tag_len+1)
    io.tagout   := wr(tag_len, 1)
    io.valid    := wr(0)
  
    when(io.we) {
      mem.write(io.index, io.datain##(io.tag##(true.B)))
    }


    //setting debug input to true enables to view what happens in memory with wires
    when(debug){
      val READ = Seq.tabulate(math.pow(2, index_len).toInt){i => Wire(UInt((data_len + tag_len + 1).W))}
      for(i <- 0 until READ.length){
        READ(i) := mem.read(i.U)
      }
    }
}