

package cc

import chisel3._
import scala.math
import chisel3.util._



class Cache(addr_len: Int, tag_len: Int, data_len: Int) extends  Module {
    val io = IO(new Bundle{
        val addr    = Input(UInt(addr_len.W))
        val tag     = Input(UInt(tag_len.W))
        val datain  = Input(UInt(data_len.W))
        val we      = Input(Bool())

        val dataout = Output(UInt(data_len.W))
        val tagout  = Output(UInt(tag_len.W))
        val valid   = Output(Bool())
    })
    val wr  = Wire(UInt((addr_len + tag_len + 1).W))
    val mem = Mem(math.pow(2, addr_len).toInt, UInt((addr_len + tag_len + 1).W)) 

    wr :=  mem.read(io.addr)

    io.dataout  := wr(addr_len + tag_len, tag_len+1)
    io.tagout   := wr(tag_len, 1)
    io.valid    := wr(0)

    when(io.we) {
      mem.write(io.addr, Cat(io.datain, io.tag, true.B))
    }
}