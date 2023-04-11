

package cc

import chisel3._
import scala.math
import chisel3.util._


class Cache(size: Int, addr_len: Int, data_len: Int) extends  Module {
    val io = IO(new Bundle{
        val addr    = Input(UInt(addr_len.W))
        val datain  = Input(UInt((data_len - ((addr_len - size) + 1)).W))
        val dataout = Output(UInt(data_len.W))
        val we      = Input(Bool())
    })


    //val mem = Mem(math.pow(2, size).toInt, Vec(data_len, Bool())) 
    val mem = Mem(math.pow(2, size).toInt, UInt(data_len.W)) 

    //val temp = Wire(UInt(addr_len.W)) // might need something like this inorder to get subset of address bits to form the tag

    io.dataout := mem.read(io.addr(0, size))
    when(io.we) {
        mem.write(io.addr(0, size), Cat(io.datain, io.addr(size, addr_len), 1.U)) //might need cat(seq("list here"))
        //mem.write(io.addr(0, size), Cat(Seq(io.datain, io.addr(size, addr_len), 1.U))) //might need cat(seq("list here"))
    }
}

    /*
def ffo(pwidth:Int, in:UInt) : UInt = {
    val rval = Wire(UInt(width=pwidth))
    rval(0) := in(0)
    for(w <- 1 until pwidth) {
      rval(w) := in(w) & !( in(w-1,0).orR() )
    }
    rval
  }



change the abowe into the form bellow inorder to adress idividual bits in the byte/word




def ffo(pwidth:Int, in:UInt) : UInt = {
    val ary = Wire(Vec(pwidth, Bool()))
    ary(0) := in(0)
    for(w <- 1 until pwidth) {
      ary(w) :=   in(w) && !( in(w-1,0).orR() )
    }
    val rval = Reverse(Cat(ary))
    rval
  }
    */