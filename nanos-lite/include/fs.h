#ifndef __FS_H__
#define __FS_H__

#include <common.h>

#ifndef SEEK_SET
enum {SEEK_SET, SEEK_CUR, SEEK_END};
#endif
typedef size_t (*ReadFn) (void *buf, size_t offset, size_t len);
typedef size_t (*WriteFn) (const void *buf, size_t offset, size_t len);
typedef struct {
  char *name;
  size_t size;
  size_t disk_offset;
  size_t open_offset;
  ReadFn read;
  WriteFn write;
} Finfo;

enum
{
  FD_STDIN,
  FD_STDOUT,
  FD_STDERR,
  DEV_EVENTS,
  DISP_INF,
  FB_DEV,
  FD_FB
};
size_t invalid_read(void *buf, size_t offset, size_t len);
size_t invalid_write(const void *buf, size_t offset, size_t len);
size_t serial_write(const void *buf, size_t offset, size_t len);
size_t events_read(void *buf, size_t offset, size_t len); //ignore offset
size_t dispinfo_read(void *buf, size_t offset, size_t len) ;//ignore offset
size_t fb_write(const void *buf, size_t offset, size_t len);
void init_fs();

/* This is the information about all files in disk. */

static Finfo file_table[] = {
    [FD_STDIN] = {"stdin", 0, 0, 0,invalid_read, invalid_write},
    [FD_STDOUT] = {"stdout", 0, 0, 0,invalid_read, serial_write},
    [FD_STDERR] = {"stderr", 0, 0, 0,invalid_read, serial_write},
    [DEV_EVENTS] = {"/dev/events",0,0,0,events_read,invalid_write},
    [DISP_INF] = {"/proc/dispinfo",0,0,0,dispinfo_read,invalid_write},
    [FB_DEV] = {"/dev/fb",0,0,0,invalid_read,fb_write},
#include "../src/files.h"
};

#endif
