#ifndef __MAL_INTERFACE_H__
#define __MAL_INTERFACE_H__

#include "simmngr_interface.h"
#include "datamngr_interface.h"


void* mal_init ();

void* mal_init2 ();

int mal_exit (void *o);

int mal_execute_atcmd (void *o, char *cmd, char *response, int response_len);

int mal_ril_connect (void *o);

int mal_ril_disconnect (void *o);

#endif /* __MAL_INTERFACE_H__ */
