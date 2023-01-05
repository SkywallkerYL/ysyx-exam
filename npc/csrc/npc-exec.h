#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <stdlib.h>
#include "VRiscvCpu.h"
#include "VRiscvCpu___024root.h"
#include "VRiscvCpu__Dpi.h"
#include "verilated_vcd_c.h"
#include "svdpi.h"
#include "state.h"
#include "types.h"
#include "macro.h"
VerilatedContext* contextp = NULL;
VerilatedVcdC* tfp = NULL;

static VRiscvCpu* top;

void step_and_dump_wave(){
  top->eval();
  contextp->timeInc(1);
  tfp->dump(contextp->time());
}
//初始化
void sim_init(){
  contextp = new VerilatedContext;
  tfp = new VerilatedVcdC;
  top = new VRiscvCpu;
  contextp->traceEverOn(true);
  top->trace(tfp, 0);
  tfp->open("wave.vcd");
}

void sim_exit(){
  step_and_dump_wave();
  tfp->close();
  delete top;
  delete contextp;
}
void clockntimes(int n ){
	
	int temp = n;
	while (temp >= 1)
	{
		top->clock = 0;
		step_and_dump_wave();
		top->clock = 1;
		step_and_dump_wave();
		temp --;
	}
}
bool checkebreak ()
{
  //这里的scpoe是调用函数位置的模块的名字
  const svScope scope = svGetScopeFromName("TOP.RiscvCpu.ebrdpi");
  assert(scope);
  svSetScope(scope);
  bool flag = ebreakflag();
  return flag;
}

void load_prog(const char *bin){
  FILE *fp = fopen(bin,"r");
  //assert(fp!=NULL);
  if(fp==NULL) {printf("No Image Input\n");return;} 
  else printf("Read file %s\n",bin);
  fseek(fp,0,SEEK_SET);
  if(fread(&top->rootp->RiscvCpu__DOT__M,1,MSIZE,fp)==0) return;
  //printf("HHH\n");
  fclose(fp);
}

static void execute(uint64_t n) {
    while (n--){
      clockntimes(1);
      if(checkebreak()){
      //printf("%d\n",top->io_halt);
        if(top->io_halt == 1) printf( ANSI_FMT("HIT GOOD TRAP\n", ANSI_FG_GREEN)) ;
        else printf(ANSI_FMT("HIT BAD TRAP\n", ANSI_FG_RED));
        break;
      }
    }
    //if (nemu_state.state != NEMU_RUNNING) {break;}
    //IFDEF(CONFIG_DEVICE, device_update());
}
