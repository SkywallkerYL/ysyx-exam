#include <readline/readline.h>
#include <readline/history.h>
#include <stdio.h>
#include <stdlib.h>
#include "npc-exec.h"

#define NR_CMD ARRLEN(cmd_table)


static char* rl_gets() {
  static char *line_read = NULL;

  if (line_read) {
    free(line_read);
    line_read = NULL;
  }

  line_read = readline("(npc-sdb) ");

  if (line_read && *line_read) {
    add_history(line_read);
  }

  return line_read;
}

static int cmd_si(char *args){
  //最多只能執行四步，因爲img 只有4個
  //如果要增加執行的次數 ，修改img 的大小
  //並且img最後一個不要定義爲nemu_trap
  //printf("single excutaion step!!!\n");
  char *time = strtok(NULL," ");
  if (time == NULL) {
    execute(1);
    return 0;
  }
  else 
  {
    int num ;
    sscanf(args,"%d",&num);
    execute(num);
  }
  return 0;
}

static int cmd_c(char *args) {
  execute(-1);
  return 0;
}

static struct {
  const char *name;
  const char *description;
  int (*handler) (char *);
} cmd_table [] = {
  { "help", "Display information about all supported commands", cmd_help },
  { "c", "Continue the execution of the program", cmd_c },
  //{ "q", "Exit NEMU", cmd_q },
  { "si", "Single excutaion", cmd_si}
  //{"info","info SUBCMD",cmd_info}
  //{"x","EXPR SCAN",cmd_x},
  //{"p","Expression calculation",cmd_p},
  //{"pt","Expression calculation test",cmd_pt},
  //{"w","Watchpoint add",cmd_w},
  //{"d","Watchpoint delete",cmd_d}
  /* TODO: Add more commands */

};


static bool is_batch_mode = false;

void sdb_set_batch_mode() {
  is_batch_mode = true;
}


void sdb_mainloop() {
  if (is_batch_mode) {
    cmd_c(NULL);
    return;
  }

  for (char *str; (str = rl_gets()) != NULL; ) {
    char *str_end = str + strlen(str);

    /* extract the first token as the command */
    char *cmd = strtok(str, " ");
    if (cmd == NULL) { continue; }

    /* treat the remaining string as the arguments,
     * which may need further parsing
     */
    char *args = cmd + strlen(cmd) + 1;
    if (args >= str_end) {
      args = NULL;
    }

#ifdef CONFIG_DEVICE
    extern void sdl_clear_event_queue();
    sdl_clear_event_queue();
#endif

    int i;
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(cmd, cmd_table[i].name) == 0) {
        if (cmd_table[i].handler(args) < 0) { return; }
        break;
      }
    }

    if (i == NR_CMD) { printf("Unknown command '%s'\n", cmd); }
  }
}