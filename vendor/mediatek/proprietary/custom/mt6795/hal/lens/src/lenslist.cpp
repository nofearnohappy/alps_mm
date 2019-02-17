#include <utils/Log.h>
#include <utils/Errors.h>
#include <fcntl.h>
#include <math.h>
#include "MediaTypes.h"
#include "camera_custom_lens.h"
#include "kd_imgsensor.h"
#include <string.h>

extern PFUNC_GETLENSDEFAULT pDummy_getDefaultData;
#if defined(SENSORDRIVE)
extern PFUNC_GETLENSDEFAULT pSensorDrive_getDefaultData;
#endif
#if defined(DW9761BAF)
extern PFUNC_GETLENSDEFAULT pDW9761BAF_getDefaultData;
#endif
#if defined(BU6429AF)
extern PFUNC_GETLENSDEFAULT pBU6429AF_getDefaultData;
#endif

MSDK_LENS_INIT_FUNCTION_STRUCT LensList_main[MAX_NUM_OF_SUPPORT_LENS] =
{
    {DUMMY_SENSOR_ID, DUMMY_LENS_ID, "Dummy", pDummy_getDefaultData},
#if defined(SENSORDRIVE)
    {S5K4ECGX_SENSOR_ID, SENSOR_DRIVE_LENS_ID, "kd_camera_hw", pSensorDrive_getDefaultData},
#endif
#if defined(BU6429AF)
	{OV13853_SENSOR_ID, BU6429AF_LENS_ID, "BU6429AF", pBU6429AF_getDefaultData},
	{OV13853_SENSOR_ID_MTS, BU6429AF_LENS_ID, "BU6429AF", pBU6429AF_getDefaultData},
    {S5K5E8YX_SENSOR_ID, DUMMY_LENS_ID, "Dummy", pDummy_getDefaultData},
#endif
#if defined(DW9761BAF)
    {S5K3M2_SENSOR_ID, DW9761BAF_LENS_ID, "DW9761BAF", pDW9761BAF_getDefaultData},
    {S5K3M2_2ND_SENSOR_ID, DW9761BAF_LENS_ID, "DW9761BAF", pDW9761BAF_getDefaultData},
#endif
};
MSDK_LENS_INIT_FUNCTION_STRUCT LensList_sub[MAX_NUM_OF_SUPPORT_LENS] =
{
    {DUMMY_SENSOR_ID, DUMMY_LENS_ID, "Dummy", pDummy_getDefaultData},
#if defined(SENSORDRIVE)
    {S5K4ECGX_SENSOR_ID, SENSOR_DRIVE_LENS_ID, "kd_camera_hw", pSensorDrive_getDefaultData},
#endif
};
MSDK_LENS_INIT_FUNCTION_STRUCT LensList_main2[MAX_NUM_OF_SUPPORT_LENS] =
{
    {DUMMY_SENSOR_ID, DUMMY_LENS_ID, "Dummy", pDummy_getDefaultData},
};
UINT32 GetLensInitFuncList(PMSDK_LENS_INIT_FUNCTION_STRUCT pLensList, unsigned int a_u4CurrSensorDev)
{
    if(a_u4CurrSensorDev==2) //sub
        memcpy(pLensList, &LensList_sub[0], sizeof(MSDK_LENS_INIT_FUNCTION_STRUCT)* MAX_NUM_OF_SUPPORT_LENS);
    else if(a_u4CurrSensorDev==4) //main 2
        memcpy(pLensList, &LensList_main2[0], sizeof(MSDK_LENS_INIT_FUNCTION_STRUCT)* MAX_NUM_OF_SUPPORT_LENS);
    else  // main or others
        memcpy(pLensList, &LensList_main[0], sizeof(MSDK_LENS_INIT_FUNCTION_STRUCT)* MAX_NUM_OF_SUPPORT_LENS);

    return 0;
}

