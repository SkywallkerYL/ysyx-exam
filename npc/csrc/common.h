#ifndef COMMON
#define COMMON


#include <stdint.h>
#include <stdbool.h>
#include <assert.h>
#include <string.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include "types.h"


#define word_t uint64_t
#define paddr_t uint64_t
#define vaddr_t uint64_t
#define CONFIG_DIFFTEST
#define CONFIG_ITRACE
#define DIFFTEST_REG_SIZE (sizeof(uint64_t) * 33) // GRPs + pc
#define CONFIG_MBASE 0x80000000
#define CONFIG_MSIZE 0x8000000
#define CONFIG_PC_RESET_OFFSET 0x0
#define PG_ALIGN __attribute((aligned(4096)))
static uint8_t pmem[CONFIG_MSIZE] PG_ALIGN = {};
enum { DIFFTEST_TO_DUT, DIFFTEST_TO_REF };
#define PMEM_LEFT  ((paddr_t)CONFIG_MBASE)
#define PMEM_RIGHT ((paddr_t)CONFIG_MBASE + CONFIG_MSIZE - 1)
#define RESET_VECTOR (PMEM_LEFT + CONFIG_PC_RESET_OFFSET)
#define FMT_PADDR "0x%016lx"
#define instr_break 0b00000000000100000000000001110011
#define MSIZE 1024 //this should be same with npc
uint32_t instr_mem[MSIZE/4];
uint8_t p_mem[MSIZE];

/* convert the guest physical address in the guest program to host virtual address in NEMU */
uint8_t* guest_to_host(paddr_t paddr);
/* convert the host virtual address in NEMU to guest physical address in the guest program */
paddr_t host_to_guest(uint8_t *haddr);

static inline bool in_pmem(paddr_t addr) {
  return addr - CONFIG_MBASE < CONFIG_MSIZE;
}

 
#define CONFIG_TARGET_NATIVE_ELF

#define Assert(cond, format, ...) \
  do { \
    if (!(cond)) { \
        (fflush(stdout), fprintf(stderr, ANSI_FMT(format, ANSI_FG_RED) "\n", ##  __VA_ARGS__)); \
           extern FILE* log_fp; fflush(log_fp); \
      extern void assert_fail_msg(); \
      assert_fail_msg(); \
      assert(cond); \
    } \
  } while (0)

#define panic(format, ...) Assert(0, format, ## __VA_ARGS__)

#define panic_on(cond, s) \
  ({ if (cond) { \
      putstr("AM Panic: "); putstr(s); \
      putstr(" @ " __FILE__ ":" TOSTRING(__LINE__) "  \n"); \
      halt(1); \
    } })
//#define panic(s) panic_on(1, s)
#define log_write(...) IFDEF(CONFIG_TARGET_NATIVE_ELF, \
  do { \
    extern FILE* log_fp; \
    extern bool log_enable(); \
    if (log_enable()) { \
      fprintf(log_fp, __VA_ARGS__); \
      fflush(log_fp); \
    } \
  } while (0) \
)

#define _Log(...) \
  do { \
    printf(__VA_ARGS__); \
    log_write(__VA_ARGS__); \
  } while (0)


#define Log(format, ...) \
    _Log(ANSI_FMT("[%s:%d %s] " format, ANSI_FG_BLUE) "\n", \
        __FILE__, __LINE__, __func__, ## __VA_ARGS__)

#endif