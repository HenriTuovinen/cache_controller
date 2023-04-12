

package cc

import chisel3._
import scala.math
import chisel3.util._


class Cache(size: Int, addr_len: Int, data_len: Int) extends  Module {
    val io = IO(new Bundle{
        val addr    = Input(new dividedAddress(size, (addr_len - size)))
        val datain  = Input(UInt(data_len.W))
        val dataout = Output(new memField(data_len, (addr_len - size)))
        val we      = Input(Bool())
    })
    val wr  = Wire(new memField(data_len,(addr_len - size)))

    val mem = Mem(math.pow(2, size).toInt, new memField(data_len,(addr_len - size))) 

    //val temp = Wire(UInt(addr_len.W)) // might need something like this inorder to get subset of address bits to form the tag


    io.dataout := mem.read(io.addr.memadr)
    when(io.we) {
      wr.data  := io.datain
      wr.tag   := io.addr.tag
      wr.valid := true.B
 
      mem.write(io.addr.memadr, wr)
    }
}