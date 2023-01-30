#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <assert.h>
#include <sys/time.h>
static int evtdev = -1;
static int fbdev = -1;
static int screen_w = 0, screen_h = 0;
static struct timeval timer_count;
uint32_t NDL_GetTicks() {
  //return ms
  //struct timeval time;
  gettimeofday(&timer_count,NULL);
  uint32_t ms = timer_count.tv_sec*1000+timer_count.tv_usec/1000;
  return ms;
  //return 0;
}
//键盘抽象成文件
//用过open 来进行系统调用文件  即借用操作系统打开文件 返回相应的文件描述 fd
//而fs_open是操作系统访问文件，用户程序通过Open 或者fopen 
//open 是更低一级的 通过NDL库来使用，用户程序直接使用NDL库
//man 2 open
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
int NDL_PollEvent(char *buf, int len) {
  int fd = open("/dev/events",0,0);
//man 2 read
  int value = read(fd,buf,len);
  close(fd);
  return (value != 0);
  //return 0;
}
// 打开一张(*w) X (*h)的画布
// 如果*w和*h均为0, 则将系统全屏幕作为画布, 并将*w和*h分别设为系统屏幕的大小
void NDL_OpenCanvas(int *w, int *h) {
  if (getenv("NWM_APP")) {
    int fbctl = 4;
    fbdev = 5;
    screen_w = *w; screen_h = *h;
    char buf[64];
    int len = sprintf(buf, "%d %d", screen_w, screen_h);
    // let NWM resize the window and create the frame buffer
    write(fbctl, buf, len);
    while (1) {
      // 3 = evtdev
      int nread = read(3, buf, sizeof(buf) - 1);
      if (nread <= 0) continue;
      buf[nread] = '\0';
      if (strcmp(buf, "mmap ok") == 0) break;
    }
    close(fbctl);
  }
  if(*w == 0 && *h == 0){
    *w = screen_w;
    *h = screen_h;
  }

}

void NDL_DrawRect(uint32_t *pixels, int x, int y, int w, int h) {
}

void NDL_OpenAudio(int freq, int channels, int samples) {
}

void NDL_CloseAudio() {
}

int NDL_PlayAudio(void *buf, int len) {
  return 0;
}

int NDL_QueryAudio() {
  return 0;
}
//解析屏幕大小dispinfo
static void dispinfo(){
  int fd = open("/proc/dispinfo",0,0);
//man 2 read
  int maxsize = 1024;
  char * buf = (char *)malloc(maxsize * sizeof(char));
  int value = read(fd,buf,maxsize);
  assert(value < maxsize);
  int read_width  = 0;
  int read_height = 0;
  //读width
  char * begin = buf;
  assert(strncmp(begin,"WIDTH",5) == 0);
  buf+=5;
  while (*buf == ' ')
  {
    buf++;
  }
  assert(*buf == ':'); buf++;
  while (*buf == ' ')
  {
    buf++;
  }
  //int pow = 1;
  while (*buf <='9' && *buf >='0')
  {
    read_width = read_width*10 + (*buf-'0'); 
    //len++;
    buf++;
  }
  while (*buf == ' ')
  {
    buf++;
  }
  assert(*buf == '\n'); buf++;
  //读weight
  assert(strncmp(buf,"HEIGHT",6) == 0);
  buf+=6;
  while (*buf == ' ')
  {
    buf++;
  }
  assert(*buf == ':'); buf++;
  while (*buf == ' ')
  {
    buf++;
  }
  //int pow = 1;
  while (*buf <='9' && *buf >='0')
  {
    read_height = read_height*10 + (*buf-'0'); 
    //len++;
    buf++;
  }
  screen_h = read_height;
  screen_w = read_width; 
  
  close(fd);
}
int NDL_Init(uint32_t flags) {
  if (getenv("NWM_APP")) {
    evtdev = 3;
  }
  //获取屏幕大小
  dispinfo();
  printf("h:%d w:%d\n",screen_h,screen_w);
  return 0;
}

void NDL_Quit() {
}
