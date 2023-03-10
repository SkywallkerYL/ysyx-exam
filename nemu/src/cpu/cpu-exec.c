/***************************************************************************************
* Copyright (c) 2014-2022 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include <cpu/cpu.h>
#include <cpu/decode.h>
#include <cpu/difftest.h>
#include <locale.h>

/* The assembly code of instructions executed is only output to the screen
 * when the number of instructions executed is less than this value.
 * This is useful when you use the `si' command.
 * You can modify this value as you want.
 */
#define MAX_INST_TO_PRINT 10

CPU_state cpu = {};
uint64_t g_nr_guest_inst = 0;
static uint64_t g_timer = 0; // unit: us
static bool g_print_step = false;
#define iringbufsize 16
char iringbuf[iringbufsize][128];
int iringbufind = 0;
void printiringbuf(int finalinst)
{
  for (int i = 0; i < iringbufsize; i++)
  {
    if (i == finalinst) printf("-->");
    printf("%s\n",iringbuf[i]);
  }
}

bool test_change();
void device_update();
//#define CONFIG_WATHCPOINT 1
static void trace_and_difftest(Decode *_this, vaddr_t dnpc) {
//检查监视点
#if CONFIG_WATHCPOINT
    bool change = test_change();
    //printf("aaaaa\n");
    if(change) nemu_state.state = NEMU_STOP;
#endif 
#ifdef CONFIG_ITRACE_COND
  if (ITRACE_COND) { log_write("%s\n", _this->logbuf); }
#endif
  if (g_print_step) { IFDEF(CONFIG_ITRACE, puts(_this->logbuf)); }
  IFDEF(CONFIG_DIFFTEST, difftest_step(_this->pc, dnpc));
}

static void exec_once(Decode *s, vaddr_t pc) {
  s->pc = pc;
  s->snpc = pc;
  isa_exec_once(s);
  cpu.pc = s->dnpc;
#ifdef CONFIG_ITRACE
  char *p = s->logbuf;
  //printf("p :%s logbuf:%s\n",p,s->logbuf);
  //printf("inside exec_once:p :%s, s->snpc: %ld, s->pc: %ld\n",p,s->snpc,s->pc);
  p += snprintf(p, sizeof(s->logbuf), FMT_WORD ":", s->pc);
  //printf("size:%ld\n",s->logbuf + sizeof(s->logbuf) - p);
  //printf("p :%s\n",s->logbuf);
  int ilen = s->snpc - s->pc;
  //printf("ilen :%d\n",ilen);
  int i;
  uint8_t *inst = (uint8_t *)&s->isa.inst.val;
  for (i = ilen - 1; i >= 0; i --) {
    //printf("inside exec_once:%s\n",p);
    p += snprintf(p, 4, " %02x", inst[i]);
    //printf("size:%ld\n",s->logbuf + sizeof(s->logbuf) - p);
    //printf("i:%d inst:%02x\n",i,inst[i]);
  }
  //printf("size:%d\n",*(uint32_t *)&s->isa.inst.val);
  //写进去的同时 p作为指针也+了
  //printf("p :%s\n",s->logbuf);
  //printf("inside exec_once:%s\n",p);
  int ilen_max = MUXDEF(CONFIG_ISA_x86, 8, 4);
 // printf("ilen_max :%d\n",ilen_max);
  //printf("ilen_max :%d\n",MUXDEF(CONFIG_ISA_x86, 8, 4));
  int space_len = ilen_max - ilen;
  if (space_len < 0) space_len = 0;
  space_len = space_len * 3 + 1;
  memset(p, ' ', space_len);
  p += space_len;
  //int size = s->logbuf + sizeof(s->logbuf) - p;
  //printf("size:%ld\n",s->logbuf + sizeof(s->logbuf) - p);
  //printf("inside exec_once:%s\n",p);
  //printf("chose pc:%08lx\n",MUXDEF(CONFIG_ISA_x86, s->snpc, s->pc));
  void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);
  disassemble(p, s->logbuf + sizeof(s->logbuf) - p,
      MUXDEF(CONFIG_ISA_x86, s->snpc, s->pc), (uint8_t *)&s->isa.inst.val, ilen);
  //printf("p :%s\n",s->logbuf);
  strcpy(iringbuf[iringbufind],s->logbuf);
  iringbufind=(iringbufind+1)%iringbufsize;
#endif
}
bool test_change();
static void execute(uint64_t n) {
  //printf("aaaa\n");
  Decode s;
  for (;n > 0; n --) {
    //printf("before exec_once:  n: %ld  nemu_state :%d\n ",n,nemu_state.state);
    exec_once(&s, cpu.pc);

    //printf("before g_nr_guest_inst: n: %ld  nemu_state :%d\n ",n,nemu_state.state);
    g_nr_guest_inst ++;
    //printf("before trace_and_difftest: n: %ld  nemu_state :%d\n ",n,nemu_state.state);
    trace_and_difftest(&s, cpu.pc);
    //printf("aaaaa\n");
    //printf("n: %ld  nemu_state :%d\n ",n,nemu_state.state);
    
    if (nemu_state.state != NEMU_RUNNING) {break;}
    IFDEF(CONFIG_DEVICE, device_update());
  }
  
}

static void statistic() {
  IFNDEF(CONFIG_TARGET_AM, setlocale(LC_NUMERIC, ""));
#define NUMBERIC_FMT MUXDEF(CONFIG_TARGET_AM, "%", "%'") PRIu64
  Log("host time spent = " NUMBERIC_FMT " us", g_timer);
  Log("total guest instructions = " NUMBERIC_FMT, g_nr_guest_inst);
  if (g_timer > 0) Log("simulation frequency = " NUMBERIC_FMT " inst/s", g_nr_guest_inst * 1000000 / g_timer);
  else Log("Finish running in less than 1 us and can not calculate the simulation frequency");
}

void assert_fail_msg() {
  printiringbuf((iringbufind+iringbufsize-1)%iringbufsize);
  isa_reg_display();
  statistic();
}

/* Simulate how the CPU works. */
void cpu_exec(uint64_t n) {
  g_print_step = (n < MAX_INST_TO_PRINT);
  //printf("nemu_state :%d\n ",nemu_state.state);
  switch (nemu_state.state) {
    case NEMU_END: case NEMU_ABORT:
      printf("Program execution has ended. To restart the program, exit NEMU and run again.\n");
      return;
    default: nemu_state.state = NEMU_RUNNING;
  }

  uint64_t timer_start = get_time();

  execute(n);

  uint64_t timer_end = get_time();
  g_timer += timer_end - timer_start;
  //printf("nemu_state :%d\n ",nemu_state.state);
  switch (nemu_state.state) {
    case NEMU_RUNNING: nemu_state.state = NEMU_STOP; break;

    case NEMU_END: case NEMU_ABORT:
      Log("nemu: %s at pc = " FMT_WORD,
          (nemu_state.state == NEMU_ABORT ? ANSI_FMT("ABORT", ANSI_FG_RED) :
           (nemu_state.halt_ret == 0 ? ANSI_FMT("HIT GOOD TRAP", ANSI_FG_GREEN) :
            ANSI_FMT("HIT BAD TRAP", ANSI_FG_RED))),
          nemu_state.halt_pc);
      // fall through
    case NEMU_QUIT: statistic();
  }
}
