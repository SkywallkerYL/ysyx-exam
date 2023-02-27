#include <amtest.h>

void rtc_test() {
  AM_TIMER_RTC_T rtc;
  int sec = 1;
  printf("time:%d\n",io_read(AM_TIMER_UPTIME).us);
  while (1) {
    while(io_read(AM_TIMER_UPTIME).us / 1000000 < sec) ;
    rtc = io_read(AM_TIMER_RTC);
    printf("%d-%d-%d %d:%d:%d GMT (", rtc.year, rtc.month, rtc.day, rtc.hour, rtc.minute, rtc.second);
    if (sec == 1) {
      printf("%d second).\n", sec);
    } else {
      //printf("us: %d\n",(uint64_t)io_read(AM_TIMER_UPTIME).us);
      printf("%d seconds).\n", sec);
    }
    sec ++;
  }
}