package npc

import chisel3._
import chisel3.util._

//乘除法模块单元
trait MulDivParm {
    val xlen = parm.REGWIDTH
}

object MulDivParm extends MulDivParm {}
//Multi

class MultiIO extends Bundle with MulDivParm{
    val MulValid        = Input(Bool())//为高表示输入的数据有效，如果没有新的输入，乘法被接受的下一个周期要拉低
    val Flush           = Input(Bool())//为高取消乘法
    val Mulw            = Input(Bool())//32位
    val MulSigned       = Input(UInt(2.W))//b11 s*s   b10 s*u  b00 u*u
    val Multiplicand    = Input(UInt(xlen.W)) //被乘数
    val Multiplier      = Input(UInt(xlen.W)) //乘数
    val MulReady        = Output(Bool())
    val OutValid        = Output(Bool())
    val ResultH         = Output(UInt(xlen.W)) // 高 64位结果
    val ResultL         = Output(UInt(xlen.W)) // 低 64位结果
}

class Mul2Exu extends Bundle{
    val Exu = new MultiIO
}

class Multi(HighPerform : Boolean = false) extends Module with MulDivParm{
    val io = IO(new Bundle{
        val Exu = new MultiIO
    })
    io.Exu.MulReady := false.B
    io.Exu.OutValid := false.B
    io.Exu.ResultH := 0.U
    io.Exu.ResultL := 0.U
    //非高性能  //移位乘法器
    
    if(!HighPerform){
        val multican = RegInit(0.U((2*xlen).W)) // 被乘数
        val multicancomp = RegInit(0.U((2*xlen).W))
        val multier  = RegInit(0.U((xlen).W)) //乘数
        val MulRes = RegInit(0.U((2*xlen).W))
        val multicount =RegInit(0.U(6.W))
        val mulw = RegInit(0.U(1.W))
        val signed   = RegInit(0.U(2.W))
        val sIdle :: sBusy :: sValid :: Nil = Enum(3)
        val MainState = RegInit(sIdle)
        switch(MainState){
            is(sIdle){
                io.Exu.MulReady := true.B
                when(io.Exu.MulValid){
                    MainState := sBusy
                    val multicanext = MuxLookup(io.Exu.MulSigned,io.Exu.Multiplicand,Seq(
                        "b11".U->func.SignExtWidth(2*xlen,io.Exu.Multiplicand,xlen),
                        "b00".U->func.UsignExtWidth(2*xlen,io.Exu.Multiplicand,xlen),
                    ))
                    multican := multicanext
                    multicancomp := ~multicanext+ 1.U

                    multier  := io.Exu.Multiplier
                    signed := io.Exu.MulSigned
                    multicount := Mux(io.Exu.Mulw===1.U,31.U,63.U)
                    mulw := io.Exu.Mulw
                    MulRes := 0.U
                }
            }
            is(sBusy){
                //
                when(io.Exu.Flush){
                    MulRes := 0.U
                    MainState := sIdle
                }
                when(multicount=/=0.U){
                    multicount := multicount - 1.U
                    multican := multican << 1.U
                    multicancomp := multicancomp << 1.U
                    multier := multier >> 1.U
                    when(multier(0)){
                        MulRes := MulRes + multican
                    }
                }.otherwise{
                    //最后一位  根据有无符号来确定运算类型
                    //无符号数直接加上，有符号数，则
                    //multier最后后一位有，则是负数，那么就加上补码 
                    //注意是原来的数的补码，不是移位后的
                    when(multier(0)){
                        val add = MuxLookup(signed, multican,Seq(
                            "b11".U -> multicancomp,
                            "b00".U -> (multican)
                        ))
                        MulRes := MulRes + add
                    }
                    MainState := sValid
                }
            }
            is(sValid){
                MainState := sIdle
                MulRes := 0.U
                io.Exu.OutValid := true.B
                io.Exu.ResultL := MulRes(63,0)
                io.Exu.ResultH := MulRes(127,64)
            }
        }
    }
}
//Div
class DivIO extends Bundle with MulDivParm{
    val DivValid        = Input(Bool())//为高表示输入的数据有效，如果没有新的输入，除法被接受的下一个周期要拉低
    val Flush           = Input(Bool())//为高取消除法
    val Divw            = Input(Bool())//32位
    val DivSigned       = Input(UInt(2.W))//b11 s*s   b10 s*u  b00 u*u
    val Divdend         = Input(UInt(xlen.W)) //被除数
    val Divisor         = Input(UInt(xlen.W)) //除数
    val DivReady        = Output(Bool())
    val OutValid        = Output(Bool())
    val Quotient        = Output(UInt(xlen.W)) // 商
    val Remainder       = Output(UInt(xlen.W)) // 余数
}
//目前只支持无符号运算
class Divder(HighPerform : Boolean = false) extends Module with MulDivParm{
    val io = IO(new Bundle{
        val Exu = new DivIO
    })
    io.Exu.DivReady   := false.B
    io.Exu.OutValid   := false.B
    io.Exu.Quotient   := 0.U
    io.Exu.Remainder  := 0.U
    //非高性能  //移位除法器
    
    if(!HighPerform){
        val divdend     = RegInit(0.U((2*xlen).W)) // 被除数
        val divdendcmp  = RegInit(0.U((2*xlen).W))
        val divisor     = RegInit(0.U((2*xlen).W)) //除数
        val DivRes      = RegInit(0.U((2*xlen).W))
        val divcount    = RegInit(0.U(6.W))
        val divw        = RegInit(0.U(1.W))
        val signed      = RegInit(0.U(2.W))
        val symbol      = RegInit(0.U(1.W)) //有符号的情况下，记录符号位
        //对于有符号的情况，直接取其数值当作无符号作运算，最后的结果再根据情况调整符号
        val cmpRes = Mux(io.Exu.Divw===0.U,DivRes(127,64),DivRes(63,32))
        val cmpDivs = Mux(io.Exu.Divw===0.U,divisor(127,64),divisor(63,32))
        val divdendsign = Mux(io.Exu.Divw===1.U,io.Exu.Divdend(31),io.Exu.Divdend(63))
        val divendSignReg = RegInit(0.U(1.W))
        val divisorsign = Mux(io.Exu.Divw===1.U,io.Exu.Divisor(31),io.Exu.Divisor(63))
        val sIdle :: sBusy :: sValid :: Nil = Enum(3)
        val MainState = RegInit(sIdle)
        switch(MainState){
            is(sIdle){
                io.Exu.DivReady := true.B
                when(io.Exu.DivValid){
                    MainState  := sBusy
                    //如果是负数 转化为补码取绝对值
                    val choosedivdend = MuxLookup(io.Exu.DivSigned,io.Exu.Divdend,Seq(
                        "b00".U -> io.Exu.Divdend,
                        "b11".U -> Mux(divdendsign,~io.Exu.Divdend+1.U,io.Exu.Divdend)
                    ))
                    divdend := choosedivdend
                    divdendcmp := ~io.Exu.Divdend + 1.U
                    val choosedivsior = MuxLookup(io.Exu.DivSigned,io.Exu.Divisor,Seq(
                        "b00".U -> io.Exu.Divisor,
                        "b11".U -> Mux(divisorsign,~io.Exu.Divisor+1.U,io.Exu.Divisor)
                    ))
                    divendSignReg := divdendsign
                    divisor    := Mux(io.Exu.Divw===1.U,choosedivsior<<32.U,choosedivsior<<64.U)
                    signed     := io.Exu.DivSigned
                    symbol     := divdendsign^divisorsign
                    divcount   := Mux(io.Exu.Divw===1.U,31.U,63.U)
                    divw       := io.Exu.Divw
                    DivRes     := choosedivdend<<1.U
                }
            }
            is(sBusy){
                //
                when(io.Exu.Flush){
                    DivRes := 0.U
                    MainState := sIdle
                }
                when(divcount=/=0.U){
                    divcount := divcount - 1.U
                    when( cmpRes >= cmpDivs  ){
                        DivRes :=  ((DivRes - divisor) + 1.U)<<1.U
                    }.otherwise{
                        DivRes := DivRes << 1.U
                    }    
                                        
                }.otherwise{
                    when( cmpRes >= cmpDivs  ){
                        DivRes :=  ((DivRes - divisor) + 1.U)
                    }
                    MainState := sValid
                }
            }
            is(sValid){
                MainState := sIdle
                DivRes := 0.U
                io.Exu.OutValid := true.B
                //整理一下 有符号数
                //商的正负好理解
                //余数的正负  应该是与被除数的正负相同
                val quotient = Mux(io.Exu.Divw===1.U,DivRes(31,0),DivRes(63,0))
                io.Exu.Quotient := MuxLookup(signed,quotient,Seq(
                    "b00".U -> quotient,
                    "b11".U -> Mux(symbol===1.U,~quotient+1.U,quotient)
                ))
                val remainder =  Mux(io.Exu.Divw===1.U,DivRes(63,32),DivRes(127,64))
                io.Exu.Remainder := MuxLookup(signed,remainder,Seq(
                    "b00".U -> remainder,
                    "b11".U -> Mux(divendSignReg===1.U,~remainder+1.U,remainder)
                ))
            }
        }
    }
}