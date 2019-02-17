/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

#define MTK_LOG_ENABLE 1
#include "ppl_agent.h"
#include <sys/types.h>
#include <sys/stat.h>
#include <string.h>

#define CONTROL_DATA_ROOT_PATH "/data/nvram/dm/"
#define CONTROL_DATA_FILE_PATH CONTROL_DATA_ROOT_PATH "ppl_config"
/* hard limit */
#define MAX_FILE_SIZE (4*1024)
#define BINDER_READ_NO_EXCEPTION (0)

#define STATUS_ENABLED 0x2
#define STATUS_LOCKED 0x4
#define STATUS_SIM_LOCKED 0x8

void PPLAgent::instantiate() {
    while (true) {
        PPLAgent *agent = new PPLAgent();
        status_t ret = defaultServiceManager()->addService(descriptor, agent);
        if (ret == OK) {
            ALOGI("Registered to Service Manager.");
            return;
        }

        ALOGW("Register FAILED. Retry in 5s.");
        sleep(5);
    }
}

PPLAgent::PPLAgent() {
    ALOGI("PPLAgent created");
}

status_t BnPPLAgent::onTransact(uint32_t code, const Parcel &data,
        Parcel *reply, uint32_t flags) {

    ALOGI("OnTransact(%u,%u)", code, flags);
    reply->writeInt32(BINDER_READ_NO_EXCEPTION);//used for readException

    switch (code) {
    case TRANSACTION_readControlData: {
        ALOGI("readControlData enter");
        data.enforceInterface(descriptor);
        int size = 0;
        char * ret = readControlData(size);
        if (ret == NULL) {
            reply->writeInt32(-1);
        } else {
            reply->writeInt32(size);
            reply->write(ret, size);
            free(ret);
        }
        ALOGI("readControlData exit");
        return NO_ERROR;
    }
        break;
    case TRANSACTION_writeControlData: {
        ALOGI("writeControlData enter");
        data.enforceInterface(descriptor);
        int len = data.readInt32();
        if (len == -1) { // array is null
            reply->writeInt32(0);
        } else {
            char buff[len];
            data.read(buff, len);
            reply->writeInt32(writeControlData(buff, len));
        }
        ALOGI("writeControlData exit");
        return NO_ERROR;
    }
        break;
    case TRANSACTION_needLock: {
        ALOGI("needLock enter");
        data.enforceInterface(descriptor);
        reply->writeInt32(needLock());
        ALOGI("needLock exit");
        return NO_ERROR;
    }
        break;
    default:
        return BBinder::onTransact(code, data, reply, flags);
    }

    return NO_ERROR;
}

char* PPLAgent::readControlData(int & size) {
    int fd = -1;
    if (-1 == (fd = open(CONTROL_DATA_FILE_PATH, O_RDONLY))) {
        ALOGD("open control data file error = %s", strerror(errno));
        return NULL;
    } else {
        // get file size
        struct stat file_stat;
        bzero(&file_stat, sizeof(file_stat));
        if (-1 == stat(CONTROL_DATA_FILE_PATH, &file_stat)) {
            ALOGD("stat control data file error = %s", strerror(errno));
            close(fd);
            return NULL;
        }
        size = file_stat.st_size;
        char *buff = (char *) malloc(size);
        if (-1 == read(fd, buff, size)) {
            ALOGD("read ControlData error = %s", strerror(errno));
            close(fd);
            return NULL;
        }
        close(fd);
        return buff;
    }
}

int PPLAgent::writeControlData(char* data, int size) {
    ALOGI("writeControlData enter");
    if (data == NULL || size == 0 || size > MAX_FILE_SIZE) {
        return 0;
    }
    int fd = -1;
    if (-1 == (fd = open(CONTROL_DATA_FILE_PATH, O_CREAT | O_WRONLY | O_TRUNC, 0775))) {
        ALOGD("open control data file error = %s", strerror(errno));
        return 0;
    } else {
        write(fd, data, size);
        fsync(fd);
        close(fd);
        FileOp_BackupToBinRegionForDM();
        ALOGI("writeControlData exit");
        return 1;
    }
}

int PPLAgent::needLock() {
    int fd = -1;
    if (-1 == (fd = open(CONTROL_DATA_FILE_PATH, O_RDONLY))) {
        ALOGD("open control data file error = %s", strerror(errno));
        return 0;
    }

    // get ControlData.status which is at the second byte
    if (-1 == lseek(fd, 1, SEEK_SET)) {
        ALOGD("lseek 1 byte error = %s", strerror(errno));
        close(fd);
        return 0;
    }
    char cstatus;
    if (-1 == read(fd, &cstatus, 1)) {
        ALOGD("read to get ControlData.status error = %s", strerror(errno));
        close(fd);
        return 0;
    }
    close(fd);

    int istatus = cstatus;
    ALOGD("istatus = %d\n", istatus);
    if ((istatus & STATUS_ENABLED) == STATUS_ENABLED
            && ((istatus & STATUS_LOCKED) == STATUS_LOCKED
                    || (istatus & STATUS_SIM_LOCKED) == STATUS_SIM_LOCKED)) {
        return 1;
    } else {
        return 0;
    }
}

int main(int argc, char *argv[]) {
    umask(000);
    if (-1 == access(CONTROL_DATA_ROOT_PATH, F_OK)) {
        if (-1 == mkdir(CONTROL_DATA_ROOT_PATH, 0775)) {
            ALOGD("make control data path error = %s", strerror(errno));
            return 0;
        }
    }

    PPLAgent::instantiate();
    ProcessState::self()->startThreadPool();
    ALOGI("PPLAgent Service is now ready");
    IPCThreadState::self()->joinThreadPool();
    return 0;
}

