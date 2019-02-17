#include "camera_custom_cam_cal.h"//for 658x new compilier option#include "camera_custom_eeprom.h"
#include <string.h>
 
CAM_CAL_TYPE_ENUM CAM_CALInit(void)
{
	return CAM_CAL_USED;
}

unsigned int CAM_CALDeviceName(char* DevName)
{
       char* str= "CAM_CAL_DRV";
       strcat (DevName, str );
	return 0;
}

unsigned int OV13853_CAM_CALDeviceName(char* DevName)
{
       char* str= "OV13853_CAM_CAL_DRV";
       strcat (DevName, str );
	return 0;
}


