#include <common.h>
#include "syscall.h"
void do_syscall(Context *c) {
  uintptr_t a[4];
  a[0] = c->GPR1;//a7 存mcause
  uint64_t ret ;
  //GPR2 是a0  存的是程序退出时的状态
  switch (a[0]) {
    case SYS_exit  :
      Log("SYSTEM_CALL_EXIT");
      sys_exit(c->GPR2);break;
    case SYS_yield :
      Log("SYSTEM_CALL_YIELD");
      ret = sys_yield();break;
    default: panic("Unhandled syscall ID = %d", a[0]);
  }
  c->GPRx = ret;
}
