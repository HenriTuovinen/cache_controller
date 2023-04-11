

package cc

import chisel3._
import scala.math


class Cache(size: Int, addr_len: Int, data_len: Int) extends  Module {
    val io = IO(new Bundle{
        val addr    = Input(UInt(addr_len.W))
        val datain  = Input(UInt((data_len - ((addr_len - size) + 1)).W))
        val dataout = Output(UInt(data_len.W))
        val we      = Input(Bool())
    })

    val mem = Mem(pow(2, size).toInt, UInt(data_len.W)) 

    //val temp = Wire(UInt(addr_len.W)) // might need something like this inorder to get subset of address bits to form the tag

    io.dataout := mem.read(io.addr(0 until size))
    when(io.we) {
        mem.write(io.addr(0 until size), Cat(io.datain, io.addr(size until addr_len), 1.U)) //might need cat(seq("list here"))
    }
}