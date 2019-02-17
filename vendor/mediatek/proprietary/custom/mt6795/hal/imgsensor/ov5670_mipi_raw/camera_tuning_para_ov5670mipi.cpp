#include <utils/Log.h>                                                                                   
#include <fcntl.h>                                                                                       
#include <math.h>                                                                                        
#include <string.h>                                                                                                         
#include "camera_custom_nvram.h"                                                                         
#include "camera_custom_sensor.h"                                                                        
#include "image_sensor.h"                                                                                
#include "kd_imgsensor_define.h"                                                                         
#include "camera_AE_PLineTable_ov5670mipi.h" //
#include "camera_info_ov5670mipi.h" //                                                             
#include "camera_custom_AEPlinetable.h"                                                                  
#include "camera_custom_tsf_tbl.h"                                                                          
                                                                                                         
#include "camera_isp_ov5670mipi.h"
#include "camera_3a_ov5670mipi.h"
#include "camera_tsf_ov5670mipi.h"
#include "camera_shading_ov5670mipi.h"
#include "camera_feature_ov5670mipi.h"

typedef NSFeature::RAWSensorInfo<SENSOR_ID> SensorInfoSingleton_T;                                             
                                                                                                               
                                                                                                               
namespace NSFeature {                                                                                          
template <>                                                                                                    
UINT32                                                                                                         
SensorInfoSingleton_T::                                                                                        
impGetDefaultData(CAMERA_DATA_TYPE_ENUM const CameraDataType, VOID*const pDataBuf, UINT32 const size) const    
{                                                                                                              
    UINT32 dataSize[CAMERA_DATA_TYPE_NUM] = {sizeof(NVRAM_CAMERA_ISP_PARAM_STRUCT),                            
                                             sizeof(NVRAM_CAMERA_3A_STRUCT),                                   
                                             sizeof(NVRAM_CAMERA_SHADING_STRUCT),                              
                                             sizeof(NVRAM_LENS_PARA_STRUCT),                                   
                                             sizeof(AE_PLINETABLE_T),                                           
                                             0,                                                                
                                             sizeof(CAMERA_TSF_TBL_STRUCT),                                    
                                             0,
                                             sizeof(NVRAM_CAMERA_FEATURE_STRUCT)
                                            };
                                                                                                               
    if (CameraDataType > CAMERA_NVRAM_DATA_FEATURE || NULL == pDataBuf || (size < dataSize[CameraDataType]))   
    {                                                                                                          
        return 1;                                                                                              
    }                                                                                                          
                                                                                                               
    switch(CameraDataType)                                                                                     
    {                                                                                                          
        case CAMERA_NVRAM_DATA_ISP:                                                                            
            memcpy(pDataBuf,&CAMERA_ISP_DEFAULT_VALUE,sizeof(NVRAM_CAMERA_ISP_PARAM_STRUCT));                  
            break;                                                                                             
        case CAMERA_NVRAM_DATA_3A:                                                                             
            memcpy(pDataBuf,&CAMERA_3A_NVRAM_DEFAULT_VALUE,sizeof(NVRAM_CAMERA_3A_STRUCT));                    
            break;                                                                                             
        case CAMERA_NVRAM_DATA_SHADING:                                                                        
            memcpy(pDataBuf,&CAMERA_SHADING_DEFAULT_VALUE,sizeof(NVRAM_CAMERA_SHADING_STRUCT));                
            break;                                                                                             
        case CAMERA_DATA_AE_PLINETABLE:                                                                        
            memcpy(pDataBuf,&g_PlineTableMapping,sizeof(AE_PLINETABLE_T));                                     
            break;                                                                                             
        case CAMERA_DATA_TSF_TABLE:                                                                            
            memcpy(pDataBuf,&CAMERA_TSF_DEFAULT_VALUE,sizeof(CAMERA_TSF_TBL_STRUCT));                          
            break;                                                                                             
        case CAMERA_NVRAM_DATA_FEATURE:                                                                            
            memcpy(pDataBuf,&CAMERA_FEATURE_DEFAULT_VALUE,sizeof(NVRAM_CAMERA_FEATURE_STRUCT));                          
            break;                                                                                             
        default:                                                                                               
            break;                                                                                         
    }                                                                                                          
    return 0;                                                                                                  
}};  //  NSFeature                                                                                             
