TARGET_BOARD_PLATFORM := mt6795

# Use the non-open-source part, if present
-include vendor/xiaomi/hennessy/BoardConfigVendor.mk

# Use the 6795 common part
include device/mediatek/mt6795/BoardConfig.mk

#Config partition size
include device/xiaomi/hennessy/partition_size.mk
BOARD_CACHEIMAGE_FILE_SYSTEM_TYPE := ext4
BOARD_FLASH_BLOCK_SIZE := 4096

include device/xiaomi/$(MTK_TARGET_PROJECT)/ProjectConfig.mk

MTK_INTERNAL_CDEFS := $(foreach t,$(AUTO_ADD_GLOBAL_DEFINE_BY_NAME),$(if $(filter-out no NO none NONE false FALSE,$($(t))),-D$(t))) 
MTK_INTERNAL_CDEFS += $(foreach t,$(AUTO_ADD_GLOBAL_DEFINE_BY_VALUE),$(if $(filter-out no NO none NONE false FALSE,$($(t))),$(foreach v,$(shell echo $($(t)) | tr '[a-z]' '[A-Z]'),-D$(v)))) 
MTK_INTERNAL_CDEFS += $(foreach t,$(AUTO_ADD_GLOBAL_DEFINE_BY_NAME_VALUE),$(if $(filter-out no NO none NONE false FALSE,$($(t))),-D$(t)=\"$($(t))\")) 

COMMON_GLOBAL_CFLAGS += $(MTK_INTERNAL_CDEFS)
COMMON_GLOBAL_CPPFLAGS += $(MTK_INTERNAL_CDEFS)

ifeq ($(strip $(MTK_IPOH_SUPPORT)), yes)
# Guarantee cache partition size: 420MB.
BOARD_MTK_SYSTEM_SIZE_KB :=2621440
BOARD_MTK_CACHE_SIZE_KB :=430080
BOARD_MTK_USERDATA_SIZE_KB :=1572864
endif
-include device/mediatek/build/build/tools/base_rule_remake.mk
